package me.biocomp.hubitat_ci.device

import groovy.transform.PackageScope
import me.biocomp.hubitat_ci.api.device_api.DeviceExecutor
import me.biocomp.hubitat_ci.validation.DebuggerDetector
import me.biocomp.hubitat_ci.validation.Flags
import me.biocomp.hubitat_ci.validation.NamedParametersValidator
import groovy.transform.TypeChecked
import groovy.transform.TypeCheckingMode
import me.biocomp.hubitat_ci.device.child.ChildDeviceRegistry
import me.biocomp.hubitat_ci.device.child.ChildDeviceWrapperImpl
import me.biocomp.hubitat_ci.device.child.DeviceChildExecutor
import me.biocomp.hubitat_ci.api.common_api.DeviceWrapper

@TypeChecked
class HubitatDeviceSandbox {
    /**
     * Read script from file.
     * @param file
     */
    HubitatDeviceSandbox(File file) {
        this.file = file
        assert file
    }

    /**
     * Read script from string.
     * @param scriptText
     */
    HubitatDeviceSandbox(String scriptText) {
        this.text = scriptText
    }

    /**
     * Compile script only.
     * @param options:
     *
     * @return script object. User can now call script's methods manually.
     */
    HubitatDeviceScript compile(Map options = [:]) {
        addFlags(options, [Flags.DontRunScript])
        return setupImpl(options)
    }

    /**
     * Compile script and run initialization methods.
     * @param options (see compile() method for options).
     * @return script object. User can now call script's methods manually.
     */
    HubitatDeviceScript run(Map options = [:]) {
        return setupImpl(options)
    }

    @TypeChecked(TypeCheckingMode.SKIP)
    private HubitatDeviceScript setupImpl(Map options) {
        validateAndUpdateSandboxOptions(options)

        def validator = readValidator(options)

        def registry = new ChildDeviceRegistry()

        def builderClosure = { Object opOrNamespace, Object typeName = null, Object deviceNetworkId = null, Object hubId = null, Object opts = [:] ->
            if (opOrNamespace == 'delete') {
                registry.delete(deviceNetworkId as String)
                return null
            }
            if (opOrNamespace == 'get') {
                return registry.getByDni(typeName as String)
            }
            if (opOrNamespace == 'list') {
                return registry.listAll()
            }

            Closure<File> resolver = (Closure<File>) options.childDeviceResolver
            assert resolver : "childDeviceResolver is required to build component devices"
            File deviceFile = resolver(opOrNamespace as String, typeName as String)
            assert deviceFile?.exists(): "Could not resolve component device file for ${opOrNamespace}:${typeName}"

            def sandbox = new HubitatDeviceSandbox(deviceFile)
            def childRunOptions = [
                    api: options.childDeviceApi ?: options.api,
                    validationFlags: [Flags.DontValidateDefinition, Flags.DontValidatePreferences],
                    globals: (opts instanceof Map) ? opts.globals : null,
                    userSettingValues: (opts instanceof Map) ? (opts.settings ?: [:]) : [:],
                    parent: options.parent
            ] as Map

            // Merge parent sandbox validation flags into the child so that flags
            // like Flags.DontRunScript are honored consistently across parent/child.
            def parentValidationFlags = options.validationFlags
            if (parentValidationFlags) {
                def mergedFlags = []
                mergedFlags.addAll(childRunOptions.validationFlags as Collection)
                if (parentValidationFlags instanceof Collection) {
                    mergedFlags.addAll(parentValidationFlags as Collection)
                } else {
                    mergedFlags.add(parentValidationFlags)
                }
                childRunOptions.validationFlags = mergedFlags.unique()
            }
            // Only set withLifecycle if the caller explicitly provided it. Passing
            // a null value will cause the NamedParametersValidator.boolParameter to
            // assert (it expects a non-null boolean when the key is present).
            if (options.containsKey('withLifecycle') && options.withLifecycle != null) {
                childRunOptions.withLifecycle = options.withLifecycle
            }

            def childScript = sandbox.run(childRunOptions)

            def dni = deviceNetworkId as String
            def parentId = (options.parent && options.parent.respondsTo('getId')) ? options.parent.id as Long : null
            def wrapper = new ChildDeviceWrapperImpl(childScript.device as DeviceWrapper, dni, null, parentId)
            registry.add(dni, wrapper, childScript)
            return wrapper
        }

        DeviceExecutor effectiveApi
        if (options.childDeviceResolver && (options.parent instanceof DeviceWrapper)) {
            effectiveApi = new DeviceChildExecutor(
                    options.api as DeviceExecutor,
                    options.parent as DeviceWrapper,
                    registry,
                    { a, b, c, d, e -> builderClosure(a, b, c, d, e) }
            )
        } else {
            effectiveApi = options.api as DeviceExecutor
        }

        HubitatDeviceScript script = file ? validator.parseScript(file) : validator.parseScript(text);

        // Set base environment before any user-provided customization so closures can
        // reference parent/globals safely (and so custom metaClass changes don't
        // interfere with our own initialize() wiring).
        if (options.parent) {
            script.setParent(options.parent)
        }

        if (options.globals) {
            script.setGlobals(readGlobals(options))
        }

        script.initialize(
                effectiveApi,
                validator,
                readUserSettingValues(options),
                options.customizeScriptBeforeRun as Closure)

        // Ensure 'state' exists even if the provided DeviceExecutor mock doesn't stub getState().
        try {
            if (effectiveApi != null && effectiveApi.metaClass.respondsTo(effectiveApi, 'getState')) {
                def st = effectiveApi.getState()
                if (st == null) {
                    def backingState = [:]
                    effectiveApi.metaClass.getState = { -> backingState }
                }
            }
        } catch (Throwable ignored) {
            // Best-effort fallback.
        }

        // Some real-world drivers access `device.displayName` (and friends) during installed()/initialize().
        // When tests supply a minimal DeviceExecutor mock, getDevice() may be null unless stubbed.
        // Provide a small default DeviceWrapper so scripts can run lifecycle code safely.
        try {
            if (effectiveApi != null && effectiveApi.metaClass.respondsTo(effectiveApi, 'getDevice')) {
                def currentDevice = effectiveApi.getDevice()
                if (currentDevice == null) {
                    def fallbackDevice = [
                            getDisplayName: { -> "Device" },
                            getLabel      : { -> "Device" },
                            getName       : { -> "Device" }
                    ] as DeviceWrapper
                    effectiveApi.metaClass.getDevice = { -> fallbackDevice }
                }
            }
        } catch (Throwable ignored) {
            // Best-effort fallback; ignore if executor implementation forbids metaClass changes.
        }

        if (options.withLifecycle && script.metaClass.respondsTo(script, 'installed')) {
            script.installed()
        }

        if (options.withLifecycle && script.metaClass.respondsTo(script, 'initialize')) {
            script.initialize()
        }

        if (!validator.hasFlag(Flags.DontRunScript)) {
            script.run()
        }

        return script
    }

    @TypeChecked(TypeCheckingMode.SKIP)
    private static DeviceValidator readValidator(Map options) {
        if (options.validationFlags)
        {
            return new DeviceValidator(options.validationFlags as List<String>)
        }

        return new DeviceValidator()
    }

    private static Map readUserSettingValues(Map options) {
        return options.userSettingValues ? options.userSettingValues as Map : [:]
    }

    private static Map readGlobals(Map options) {
        return options.globals ? options.globals as Map : [:]
    }

    private static final NamedParametersValidator optionsValidator = NamedParametersValidator.make {
        objParameter("api", notRequired(), canBeNull(), { v -> new Tuple2("DeviceExecutor", v instanceof DeviceExecutor)} )
        objParameter("userSettingValues", notRequired(), mustNotBeNull(), { v -> new Tuple2("Map<String, Object>", v as Map<String, Object>) })
        objParameter("customizeScriptBeforeRun", notRequired(), mustNotBeNull(), { v -> new Tuple2("Closure taking HubitatDeviceScript", v as Closure) })
        objParameter("validationFlags", notRequired(), mustNotBeNull(), { v -> new Tuple2("List<Flags>", v as List<Flags>) })
        objParameter("globals", notRequired(), mustNotBeNull(), { v -> new Tuple2("Map<String, Object>", v as Map<String, Object>) })
        objParameter("parent", notRequired(), mustNotBeNull(), { v -> new Tuple2("DeviceWrapper", v instanceof DeviceWrapper) })
        objParameter("childDeviceResolver", notRequired(), mustNotBeNull(), { v -> new Tuple2("Closure", v as Closure) })
        objParameter("childDeviceApi", notRequired(), mustNotBeNull(), { v -> new Tuple2("DeviceExecutor", v as DeviceExecutor) })
        boolParameter("withLifecycle", notRequired())
    }

    private static void validateAndUpdateSandboxOptions(Map options) {
        optionsValidator.validate("Validating sandbox options", options, EnumSet.noneOf(Flags))
    }

    static private void addFlags(Map options, List<Flags> flags) {
        options.putIfAbsent('validationFlags', [])
        (options.validationFlags as List<Flags>).addAll(flags)
    }

    final private File file = null
    final private String text = null
}