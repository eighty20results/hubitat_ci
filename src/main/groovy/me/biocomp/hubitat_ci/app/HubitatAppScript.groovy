package me.biocomp.hubitat_ci.app

import me.biocomp.hubitat_ci.api.app_api.AppExecutor
import me.biocomp.hubitat_ci.api.common_api.ChildDeviceWrapper
import me.biocomp.hubitat_ci.api.common_api.InstalledAppWrapper
import me.biocomp.hubitat_ci.app.preferences.AppPreferencesReader
import me.biocomp.hubitat_ci.app.preferences.Preferences
import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import me.biocomp.hubitat_ci.util.ScriptUtil
import me.biocomp.hubitat_ci.validation.DebuggerDetector

/*
    Custom Script that redirects most unknown calls to app_, and does not use Binding.
*/

@TypeChecked
abstract class HubitatAppScript extends
        Script
{
    private Map userSettingsMap
    private AppPreferencesReader preferencesReader = null
    private AppDefinitionReader definitionReader = null
    private AppMappingsReader mappingsReader = null
    private AppSubscriptionReader subscriptionReader = null
    final private List<Closure> validateAfterRun = []
    private AppValidator validator = null
    private AppData data = new AppData()
    private Closure childDeviceFactory
    private Closure childAppFactory
    private Closure childAppAccessor

    private final HashSet<String> existingMethods = InitExistingMethods()

    @Delegate
    private AppExecutor api = null

    private Object parent

    private static HashSet<String> InitExistingMethods() {
        return HubitatAppScript.metaClass.methods.collect { m -> m.name } as HashSet<String>;
    }

    @TypeChecked
    @CompileStatic
    void initializeFromParent(HubitatAppScript parent) {
        this.api = readPrivateField(parent, 'api') as AppExecutor
        this.preferencesReader = readPrivateField(parent, 'preferencesReader') as AppPreferencesReader
        this.definitionReader = readPrivateField(parent, 'definitionReader') as AppDefinitionReader
        this.mappingsReader = readPrivateField(parent, 'mappingsReader') as AppMappingsReader
        this.userSettingsMap = readPrivateField(parent, 'userSettingsMap') as Map
    }

    @CompileStatic
    private static Object readPrivateField(Object instance, String fieldName) {
        Class currentClass = instance.getClass()
        while (currentClass != null) {
            try {
                java.lang.reflect.Field field = currentClass.getDeclaredField(fieldName)
                field.setAccessible(true)
                return field.get(instance)
            } catch (NoSuchFieldException ignored) {
                currentClass = currentClass.getSuperclass()
            }
        }
        throw new MissingFieldException(fieldName, instance.getClass())
    }

    private Map<String, Object> injectedMappingHandlerData = [:]

    @CompileStatic
    void installMappingInjectedProps(def params, def request) {
        injectedMappingHandlerData['params'] = params
        injectedMappingHandlerData['request'] = request
    }

    @CompileStatic
    void initializeScript(
            AppExecutor api,
            AppValidator validator,
            Map userSettingValues,
            Closure customizeScriptBeforeRun,
            Closure childDeviceFactory,
            Closure childAppFactory,
            Object parent,
            Closure childAppAccessor = null)
    {
        customizeScriptBeforeRun?.call(this)

        this.childDeviceFactory = childDeviceFactory
        this.childAppFactory = childAppFactory
        this.childAppAccessor = childAppAccessor
        this.parent = parent

        // This guy needs to be first - it checks if its api is null,
        // and then does nothing in subscribe().
        this.subscriptionReader = new AppSubscriptionReader(api, validator, data, this)
        api = this.subscriptionReader
        validateAfterRun.add(this.subscriptionReader.&initializationComplete)

        this.preferencesReader = new AppPreferencesReader(this,
                api,
                validator,
                userSettingValues,
                data.preferences,
                data,
                new DebuggerDetector())
        api = this.preferencesReader
        validateAfterRun.add(this.preferencesReader.&validateAfterRun)

        this.definitionReader = new AppDefinitionReader(api, validator.flags, data.definitions)
        api = this.definitionReader
        validateAfterRun.add(this.definitionReader.&validateAfterRun)

        this.mappingsReader = new AppMappingsReader(api, this, validator)
        api = this.mappingsReader
        validateAfterRun.add(this.mappingsReader.&validateAfterRun)

        validateAfterRun.add(validator.&validateAfterRun.curry(data))

        this.api = api
        this.userSettingsMap = preferencesReader.getSettings()

        this.validator = validator
    }

    /**Initialize with just SettingsContainer for stack trace detection*/
    @CompileStatic
    void initializeForStackDetection(Map settingsContainer) {
        this.userSettingsMap = settingsContainer
    }

    /**
     * Call to this method is injected into every user's method.
     * This allows additional validations while calling separate methods on script object.*/
    @CompileStatic
    void hubitatciValidateAfterMethodCall(String methodName) {
        this.preferencesReader.validateAfterMethodCall(methodName)
    }

    @CompileStatic
    Preferences getProducedPreferences() {
        data.preferences
    }

    Map<String, Object> getProducedDefinition() {
        data.definitions
    }

    Map<String, MappingPath> getProducedMappings() {
        mappingsReader.getMappings()
    }

    AppMappingsReader getMappingsReader() {
        return mappingsReader
    }

    ChildDeviceWrapper addChildDevice(String namespace, String typeName, String deviceNetworkId, Long hubId, Map options) {
        if (childDeviceFactory == null) {
            throw new IllegalStateException("Child device factory is not configured")
        }
        return childDeviceFactory(namespace, typeName, deviceNetworkId, hubId, options)
    }

    ChildDeviceWrapper addChildDevice(String namespace, String typeName, String deviceNetworkId) {
        return addChildDevice(namespace, typeName, deviceNetworkId, null, [:])
    }

    void deleteChildDevice(String deviceNetworkId) {
        if (childDeviceFactory == null) {
            throw new IllegalStateException("Child device factory is not configured")
        }
        childDeviceFactory.call('delete', null, deviceNetworkId)
    }

    List getChildDevices() {
        if (childDeviceFactory == null) {
            throw new IllegalStateException("Child device factory is not configured")
        }
        List createdChildren = childDeviceFactory.call('list') as List
        List existingChildren = null
        try {
            existingChildren = this.@api?.getChildDevices() as List
        } catch (Throwable ignored) {
            existingChildren = null
        }

        return mergeChildDeviceLists(existingChildren, createdChildren)
    }

    ChildDeviceWrapper getChildDevice(String deviceNetworkId) {
        assert childDeviceFactory != null: "Child device factory is not configured"
        final ChildDeviceWrapper createdChild = childDeviceFactory.call('get', deviceNetworkId) as ChildDeviceWrapper
        if (createdChild != null) {
            return createdChild
        }

        return this.@api?.getChildDevice(deviceNetworkId) as ChildDeviceWrapper
    }

    List getAllChildDevices() {
        if (childDeviceFactory == null) {
            throw new IllegalStateException("Child device factory is not configured")
        }
        List createdChildren = childDeviceFactory.call('list') as List
        List existingChildren = null
        try {
            existingChildren = this.@api?.getAllChildDevices() as List
        } catch (Throwable ignored) {
            existingChildren = null
        }
        if (existingChildren == null) {
            try {
                existingChildren = this.@api?.getChildDevices() as List
            } catch (Throwable ignored) {
                existingChildren = null
            }
        }
        return mergeChildDeviceLists(existingChildren, createdChildren)
    }

    InstalledAppWrapper addChildApp(String namespace, String smartAppVersionName, String label, Map properties) {
        if (childAppFactory == null) {
            throw new IllegalStateException("Child app factory is not configured")
        }
        return childAppFactory(namespace, smartAppVersionName, label, properties)
    }

    InstalledAppWrapper addChildApp(String namespace, String smartAppVersionName, String label) {
        return (InstalledAppWrapper)addChildApp(namespace, smartAppVersionName, label, [:])
    }

    Object getChildApps() {
        if (childAppAccessor == null) {
            throw new IllegalStateException("Child app accessor is not configured")
        }
        return childAppAccessor('list')
    }

    Object getChildAppById(Long id) {
        if (childAppAccessor == null) {
            throw new IllegalStateException("Child app accessor is not configured")
        }
        return childAppAccessor('get', id)
    }

    Object getAllChildApps() {
        return getChildApps()
    }

    Object getChildAppByLabel(String label) {
        if (childAppAccessor == null) {
            throw new IllegalStateException("Child app accessor is not configured")
        }
        return childAppAccessor('getByLabel', label)
    }

    void deleteChildApp(Long id) {
        if (childAppAccessor == null) {
            throw new IllegalStateException("Child app accessor is not configured")
        }
        childAppAccessor('delete', id)
    }

    private static List mergeChildDeviceLists(List existingChildren, List createdChildren) {
        LinkedHashSet result = new LinkedHashSet()
        if (existingChildren != null) {
            result.addAll(existingChildren)
        }
        if (createdChildren != null) {
            result.addAll(createdChildren)
        }
        return new ArrayList(result)
    }

    InstalledAppWrapper getParent() {
        return this.@parent as InstalledAppWrapper
    }

    void setParent(Object parent) {
        this.@parent = parent
    }

    /*
        Don't let Script base class to redirect properties to binding,
            it causes confusing issues when using non-supported methods and properties.

        Also, getProperty() is still going to be called for valid get*() methods added by @Delegate.
        So we need to intercept those separately.
     */

    @Override
    @CompileStatic
    Object getProperty(String property) {
        switch (property) {
            case "params":
                if (this.@injectedMappingHandlerData != null) {
                    return this.@injectedMappingHandlerData['params']
                }

                break;

            case "parent":
                if (this.@parent != null) {
                    return this.@parent
                }

                break;

            case "request":
                if (this.@injectedMappingHandlerData != null) {
                    return this.@injectedMappingHandlerData['request']
                }

                break;

        // default: - continue processing below
        }

        return ScriptUtil.handleGetProperty(property, this, this.@userSettingsMap)
    }

    /*
        Don't let Script base class to redirect properties to binding
        + handle special cases of 'params' and 'props'
    */

    @Override
    @CompileStatic
    void setProperty(String property, Object newValue) {
        switch (property) {
            case "params":
                if (this.@injectedMappingHandlerData != null) {
                    throw new ReadOnlyPropertyException(
                            "'params' injected value in mapping handler is for reading only", this.class)
                }

                break;

            case "request":
                if (this.@injectedMappingHandlerData != null) {
                    throw new ReadOnlyPropertyException(
                            "'request' injected value in mapping handler is for reading only", this.class)
                }

                break;
        }

        ScriptUtil.handleSetProperty(property, this, newValue, this.@userSettingsMap)
    }

    @Override
    def run() {
        scriptBody()
        validateAfterRun.each { it() }
    }

    abstract void scriptBody()
}
