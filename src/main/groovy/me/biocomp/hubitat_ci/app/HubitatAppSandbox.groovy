package me.biocomp.hubitat_ci.app

import groovy.transform.PackageScope
import me.biocomp.hubitat_ci.api.app_api.AppExecutor
import me.biocomp.hubitat_ci.app.preferences.Preferences
import me.biocomp.hubitat_ci.validation.DebuggerDetector
import me.biocomp.hubitat_ci.validation.Flags
import me.biocomp.hubitat_ci.validation.NamedParametersValidator
import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import groovy.transform.TypeCheckingMode
import me.biocomp.hubitat_ci.app.child.AppChildExecutor
import me.biocomp.hubitat_ci.app.child.ChildAppRegistry
import me.biocomp.hubitat_ci.app.child.InstalledAppWrapperImpl
import me.biocomp.hubitat_ci.device.child.ChildDeviceRegistry
import me.biocomp.hubitat_ci.device.child.ChildDeviceWrapperImpl
import me.biocomp.hubitat_ci.device.HubitatDeviceSandbox
import me.biocomp.hubitat_ci.api.common_api.DeviceWrapper

/**
 * This sandbox can load script from file or string,
 * parse it, while wrapping into a sandbox.
 *
 * Then it can run initialization methods and
 * produce script object that user can then test further by calling its methods.
 */
@TypeChecked
class HubitatAppSandbox {
    /**
     * Read script from file.
     * @param file
     */
    HubitatAppSandbox(File file) {
        this.file = file
        assert file
    }

    /**
     * Read script from string.
     * @param scriptText
     */
    HubitatAppSandbox(String scriptText) {
        this.text = scriptText
    }

    /**
     * Compile script only.
     * @param options:
     *
     * @return script object. User can now call script's methods manually.
     */
    HubitatAppScript compile(Map options = [:]) {
        addFlags(options, [Flags.DontRunScript])
        return setupImpl(options)
    }

    /**
     * Compile script and run initialization methods.
     * @param options (see compile() method for options).
     * @return script object. User can now call script's methods manually.
     */
    HubitatAppScript run(Map options = [:]) {
        return setupImpl(options)
    }

    /**
     * Calls run() with Flags.DontValidateDefinition and returns juts preferences, not script.
     * @param options
     * @return
     */
    Preferences readPreferences(Map options = [:]) {
        addFlags(options, [Flags.DontValidateDefinition])
        setupImpl(options).getProducedPreferences()
    }

    @TypeChecked(TypeCheckingMode.SKIP)
    private HubitatAppScript setupImpl(Map options) {
        validateAndUpdateSandboxOptions(options)

        def validator = readValidator(options)

        // Per-sandbox registries and ID generation
        def childDeviceRegistry = new ChildDeviceRegistry()
        def childAppRegistry = new ChildAppRegistry()

        InstalledAppWrapperImpl parentWrapper = options.parent as InstalledAppWrapperImpl
        if (!parentWrapper) {
            parentWrapper = new InstalledAppWrapperImpl(nextAppId(), "App", "App", null)
        }

        // Provide a minimal default childDeviceResolver that searches common locations
        if (!options.childDeviceResolver) {
            options.childDeviceResolver = { String namespace, String typeName ->
                // normalize typeName (strip .groovy if present)
                String tn = typeName
                if (tn == null) tn = ''
                // If typeName contains '.groovy' somewhere (e.g. 'name.groovy - description'), extract up to it
                if (tn.contains('.groovy')) {
                    tn = tn.substring(0, tn.indexOf('.groovy'))
                }
                // If still contains a description after ' - ' or whitespace, take the first token
                if (tn.contains(' - ')) {
                    tn = tn.split(' - ')[0]
                }
                if (tn.contains(' ')) {
                    tn = tn.split(' ')[0]
                }

                // Try local src tree first
                def candidatePaths = [
                        "src/main/groovy/${namespace.replace('.', '/')}/${tn}.groovy",
                        "src/main/groovy/${namespace.replace('.', '/')}/${tn}.groovy",
                        "SubmodulesWithScripts/${namespace}/${tn}.groovy",
                        "SubmodulesWithScripts/${namespace}/drivers/${tn}.groovy",
                        "SubmodulesWithScripts/${namespace}/Drivers/${tn}.groovy",
                        "Scripts/Devices/${tn}.groovy",
                        "Scripts/Devices/${namespace}/${tn}.groovy",
                        "Scripts/Devices/${tn}.groovy"
                ]

                for (p in candidatePaths) {
                    def f = new File(p)
                    if (f.exists()) return f
                }

                // As a last resort, try searching Scripts/Devices for files whose name contains tn
                def scriptsDir = new File('Scripts/Devices')
                if (scriptsDir.exists()) {
                    for (f in scriptsDir.listFiles()) {
                        if (f.name.contains(tn)) return f
                    }
                }

                return null
            } as Closure<File>
        }

        Closure buildChildDevice = { String namespace, String typeName, String dni, Long hubId, Map opts ->
            Closure<File> resolver = (Closure<File>) options.childDeviceResolver
            assert resolver : "childDeviceResolver is required to build child devices"
            File deviceFile = resolver(namespace, typeName)
            assert deviceFile?.exists(): "Could not resolve child device file for ${namespace}:${typeName}"

            // Merge validation flags so child device respects parent flags (including DontRunScript)
            def childValidationFlags = [] as List<Flags>
            childValidationFlags.addAll([Flags.DontValidateDefinition, Flags.DontValidatePreferences])
            if (options.validationFlags) {
                childValidationFlags.addAll(options.validationFlags as List<Flags>)
            }

            def deviceSandbox = new HubitatDeviceSandbox(deviceFile)
            // Ensure we only pass a DeviceExecutor to child device runs. If caller provided
            // a specific childDeviceApi use it; otherwise, only use options.api when it
            // is actually a DeviceExecutor implementation. Passing an AppExecutor here
            // causes DeviceMetadataReader to be constructed with the wrong delegate and
            // later MissingMethodException when device-specific getters (like getZwave)
            // are invoked.
            def resolvedApi = null
            if (options.childDeviceApi) {
                resolvedApi = options.childDeviceApi
            } else if (options.api instanceof me.biocomp.hubitat_ci.api.device_api.DeviceExecutor) {
                resolvedApi = options.api
            }

            def childRunOptions = [
                    api: resolvedApi,
                    validationFlags: childValidationFlags,
                    globals: opts?.globals ?: (options.globals ?: [:]),
                    userSettingValues: opts?.settings ?: [:],
                    parent: parentWrapper
            ] as Map

            if (options.containsKey('withLifecycle') && options.withLifecycle != null) {
                childRunOptions.withLifecycle = options.withLifecycle
            }

            def childScript = deviceSandbox.run(childRunOptions)

            // Wrap existing device wrapper, fall back to GeneratedDeviceInputBase
            def wrapper = new ChildDeviceWrapperImpl(childScript as DeviceWrapper, dni, parentWrapper?.id, null)
            childDeviceRegistry.add(dni, wrapper, childScript)
            return wrapper
        }

        def childDeviceFactory = { Object opOrNamespace, Object typeName = null, Object deviceNetworkId = null, Object hubId = null, Object opts = [:] ->
            if (opOrNamespace == 'delete') {
                childDeviceRegistry.delete(deviceNetworkId as String)
                return null
            }
            if (opOrNamespace == 'get') {
                return childDeviceRegistry.getByDni(typeName as String)
            }
            if (opOrNamespace == 'list') {
                return childDeviceRegistry.listAll()
            }

            def namespace = opOrNamespace as String
            def dni = deviceNetworkId as String
            return buildChildDevice(namespace, typeName as String, dni, hubId as Long, opts as Map)
        }

        Closure childAppBuilder = { String namespace, String smartAppVersionName, String label, Map props ->
            Closure<File> appResolver = (Closure<File>) options.childAppResolver
            assert appResolver : "childAppResolver is required to build child apps"
            File appFile = appResolver(namespace, smartAppVersionName)
            assert appFile?.exists(): "Could not resolve child app file for ${namespace}:${smartAppVersionName}"

            def appSandbox = new HubitatAppSandbox(appFile)
            def childParentWrapper = new InstalledAppWrapperImpl(nextAppId(), label, smartAppVersionName, parentWrapper?.id)
            def childAppRunOptions = [
                    api: options.childAppApi ?: options.api,
                    validationFlags: [Flags.DontValidateDefinition, Flags.DontValidatePreferences],
                    parent: childParentWrapper,
                    childDeviceResolver: options.childDeviceResolver,
                    childAppResolver: options.childAppResolver
            ] as Map

            if (options.containsKey('withLifecycle') && options.withLifecycle != null) {
                childAppRunOptions.withLifecycle = options.withLifecycle
            }

            def childScript = appSandbox.run(childAppRunOptions)

            childAppRegistry.add(childParentWrapper.id, childParentWrapper, childScript)
            return childParentWrapper
        }

        def childAppAccessor = { String op, Object arg = null ->
            switch (op) {
                case 'get': return childAppRegistry.getById(arg as Long)
                case 'getByLabel': return childAppRegistry.getByLabel(arg as String)
                case 'list': return childAppRegistry.listAll()
                case 'delete': childAppRegistry.delete(arg as Long); return null
            }
        }

        // Wrap executor with child support
        AppExecutor effectiveApi = new AppChildExecutor(options.api as AppExecutor, parentWrapper, childDeviceRegistry, { a,b,c,d,e -> childDeviceFactory(a,b,c,d,e) }, childAppBuilder, childAppRegistry)

        HubitatAppScript script = file ? validator.parseScript(file) : validator.parseScript(text);

        // Inject script into parentWrapper so child devices can call methods on the parent app
        if (parentWrapper.respondsTo('setScript')) {
            parentWrapper.setScript(script)
        }

        script.initializeScript(
                effectiveApi,
                validator,
                readUserSettingValues(options),
                (Closure) options.customizeScriptBeforeRun,
                (Closure) childDeviceFactory,
                (Closure) childAppBuilder,
                (Object) parentWrapper)

        if (options.withLifecycle && script.metaClass.respondsTo(script, 'installed')) {
            script.installed()
        }

        if (options.withLifecycle && script.metaClass.respondsTo(script, 'initialize', [] as Object[])) {
            script.initialize()
        }

        if (options.parent) {
            script.setParent(options.parent)
        }

        if (!validator.hasFlag(Flags.DontRunScript)) {
            script.run()
        }

        return script
    }

    @TypeChecked(TypeCheckingMode.SKIP)
    private static AppValidator readValidator(Map options) {
        if (options.validationFlags)
        {
            return new AppValidator(options.validationFlags as List<String>)
        }
        else if (options.validator)
        {
            assert options.validator
            return options.validator as AppValidator
        }

        return new AppValidator()
    }

    private static Map readUserSettingValues(Map options) {
        return options.userSettingValues ? options.userSettingValues as Map : [:]
    }

    private static final NamedParametersValidator optionsValidator = NamedParametersValidator.make {
        objParameter("api", notRequired(), canBeNull(), { v -> new Tuple2("AppExecutor", v instanceof AppExecutor)} )
        objParameter("userSettingValues", notRequired(), mustNotBeNull(), { v -> new Tuple2("Map<String, Object>", v as Map<String, Object>) })
        objParameter("customizeScriptBeforeRun", notRequired(), mustNotBeNull(), { v -> new Tuple2("Closure taking HubitatAppScript", v as Closure) })
        objParameter("validationFlags", notRequired(), mustNotBeNull(), { v -> new Tuple2("List<Flags>", v as List<Flags>) })
        objParameter("validator", notRequired(), mustNotBeNull(), { v -> new Tuple2("AppValidator", v as AppValidator) })
        boolParameter("noValidation", notRequired())
        objParameter("parent", notRequired(), mustNotBeNull(), { v -> new Tuple2("InstalledAppWrapper", true) })
        objParameter("childDeviceResolver", notRequired(), mustNotBeNull(), { v -> new Tuple2("Closure", v as Closure) })
        objParameter("childAppResolver", notRequired(), mustNotBeNull(), { v -> new Tuple2("Closure", v as Closure) })
        objParameter("childDeviceApi", notRequired(), mustNotBeNull(), { v -> new Tuple2("AppExecutor", v as AppExecutor) })
        objParameter("childAppApi", notRequired(), mustNotBeNull(), { v -> new Tuple2("AppExecutor", v as AppExecutor) })
        objParameter("globals", notRequired(), mustNotBeNull(), { v -> new Tuple2("Map<String, Object>", v as Map<String, Object>) })
         boolParameter("withLifecycle", notRequired())
    }

    @CompileStatic
    private static void validateAndUpdateSandboxOptions(Map options) {
        optionsValidator.validate("Validating sandbox options", options, EnumSet.noneOf(Flags))

        if (options.noValidation) {
            addFlags(options, [Flags.DontValidateDefinition, Flags.DontValidatePreferences])
        }
    }

    static private void addFlags(Map options, List<Flags> flags) {
        options.putIfAbsent('validationFlags', [])
        (options.validationFlags as List<Flags>).addAll(flags)
    }

    private static long appIdCounter = 1000
    private static synchronized long nextAppId() { return ++appIdCounter }

    final private File file = null
    final private String text = null
}