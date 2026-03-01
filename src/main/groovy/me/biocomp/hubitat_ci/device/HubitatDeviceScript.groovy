package me.biocomp.hubitat_ci.device

import me.biocomp.hubitat_ci.api.device_api.DeviceExecutor
import me.biocomp.hubitat_ci.api.common_api.DeviceWrapper
import me.biocomp.hubitat_ci.device.metadata.Definition
import me.biocomp.hubitat_ci.device.metadata.DeviceInput
import me.biocomp.hubitat_ci.device.metadata.DeviceMetadataReader
import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import me.biocomp.hubitat_ci.util.ScriptUtil
import me.biocomp.hubitat_ci.validation.DebuggerDetector

/**
 * Custom Script that redirects most unknown calls to app_, and does not use Binding.
 */
@TypeChecked
abstract class HubitatDeviceScript extends Script
{
    private Map userSettingsMap
    private DeviceMetadataReader metadataReader = null
    private DeviceValidator validator = null
    private DeviceData data = null

    @Delegate
    private DeviceExecutor api = null

    // Hubitat provides a persistent state map; drivers commonly use it.
    // In hubitat_ci we delegate state storage to the executor API.
    @CompileStatic
    Map getState() {
        Map s = this.@api?.getState()
        if (s != null) {
            return s
        }

        // Some tests may provide an API mock without getState() stubbing.
        // Provide a local backing map so `state.foo = ...` keeps working.
        if (this.@internalState == null) {
            this.@internalState = [:]
        }
        return this.@internalState
    }

    private Object parent

    @CompileStatic
    void initialize(DeviceExecutor api, DeviceValidator validator, Map userSettingValues, Closure customizeScriptBeforeRun)
    {
        try {
            customizeScriptBeforeRun?.call(this)
        } catch (MissingPropertyException ignored) {
            // Some tests/scripts dynamically patch metaClass and can trigger Groovy internals
            // to look up a 'propertyMissing' property on MetaClassImpl. That lookup is not
            // relevant to hubitat_ci initialization, so we ignore it here.
        }

        this.data = new DeviceData()

        this.metadataReader = new DeviceMetadataReader(api/*this, api*/, validator, userSettingValues, this.getMetaClass(), data, new DebuggerDetector())
        api = this.metadataReader

        this.api = api

        this.userSettingsMap = this.metadataReader.settings

        this.validator = validator
    }

    /**
     * Initialize with just SettingsContainer for stack trace detection
    */
    @CompileStatic
    void initializeForStackDetection(Map settingsContainer) {
        this.userSettingsMap = settingsContainer
    }

    @CompileStatic
    Definition getProducedDefinition()
    {
        metadataReader.getProducedDefinition()
    }

    @CompileStatic
    List<DeviceInput> getProducedPreferences()
    {
        return data.producedPreferences
    }

    /**
     * Call to this method is injected into every user's method.
     * This allows additional validations while calling separate methods on script object.
     */
    @CompileStatic
    void hubitatciValidateAfterMethodCall(String methodName)
    {
        // This call is injected into (nearly) every user-defined method.
        // During script construction / very early lifecycle (before initialize()),
        // metadataReader can still be null, so validation must be a no-op.
        if (this.@metadataReader == null || this.@metadataReader.settings == null) {
            return
        }

        //println "hubitatciValidateAfterMethodCall(${methodName} called!)"
        this.@metadataReader.settings.validateAfterPreferences(methodName)
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
            case "metaClass":
                return getMetaClass();
            case "parent":
                if (this.@parent != null) {
                    return this.@parent
                }
                break
        }

        if (this.@globals != null && this.@globals.containsKey(property)) {
            return this.@globals.get(property)
        }

        return ScriptUtil.handleGetProperty(property, this, this.@userSettingsMap, this.@globals)
    }

    /*
        Don't let Script base class to redirect properties to binding
        + handle special cases of 'params' and 'props'
    */
    @Override
    @CompileStatic
    void setProperty(String property, Object newValue) {
        ScriptUtil.handleSetProperty(property, this, newValue, this.@userSettingsMap)
    }

    @Override
    @CompileStatic
    def run()
    {
        scriptBody()
        validator.validateAfterRun(this.metadataReader)
    }

    abstract void scriptBody()

    void setGlobals(Map globals) {
        this.globals = globals ?: [:]
    }

    Map getGlobals() { return this.globals }

    void setParent(Object parent) {
        this.parent = parent
    }

    Object getParent() {
        return this.parent
    }

    private Map internalState = null

    private Map globals = [:]

    @CompileStatic
    DeviceWrapper getDevice() {
        // If executor provides it, use that.
        def d = this.@api?.getDevice()
        if (d != null) {
            return d as DeviceWrapper
        }

        // Otherwise, provide a minimal stub.
        if (this.@fallbackDevice == null) {
            this.@fallbackDevice = [
                    getDisplayName: { -> "Device" },
                    getLabel      : { -> "Device" },
                    getName       : { -> "Device" },
                    getDeviceNetworkId: { -> "device" },
                    getId         : { -> 1L }
            ] as DeviceWrapper
        }

        return this.@fallbackDevice
    }

    private DeviceWrapper fallbackDevice = null

    @CompileStatic
    Map getSettings() {
        return this.@userSettingsMap
    }
}
