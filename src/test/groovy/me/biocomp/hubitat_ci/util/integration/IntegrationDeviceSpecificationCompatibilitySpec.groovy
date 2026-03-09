package me.biocomp.hubitat_ci.util.integration

import me.biocomp.hubitat_ci.validation.Flags

/**
 * Regression coverage for issue #44 integration harness compatibility.
 */
class IntegrationDeviceSpecificationCompatibilitySpec extends IntegrationDeviceSpecification {
    private File scriptFile

    @Override
    def setup() {
        scriptFile = File.createTempFile("issue44-device-", ".groovy")
        scriptFile.deleteOnExit()
        scriptFile.text = '''
metadata {
    definition(name: "Issue44 Test Device", namespace: "test", author: "test") {
        capability "Sensor"
    }
    preferences {
        input "enableDebugLogging", "bool", title: "Enable Debug Logging", required: false
    }
}

def callHttpPost() {
    Integer status = null
    httpPost([uri: "https://example.test"]) { response ->
        status = response.status as Integer
    }
    return status
}

def ensureSettingDefault() {
    device.updateSetting("enableDebugLogging", [value: "true", type: "bool"])
    return settings.enableDebugLogging
}

def callTimeHelper() {
    return _time_utcNow()
}

Map parse(String s) { [:] }
'''

        initializeEnvironment(
            deviceScriptFilename: scriptFile.absolutePath,
            validationFlags: [
                Flags.Default,
                Flags.DontValidateCapabilities,
                Flags.DontRequireParseMethodInDevice,
                Flags.AllowSectionsInDevicePreferences,
                Flags.AllowWritingToSettings
            ],
            userSettingValues: [enableDebugLogging: false],
            scriptStubs: [
                httpPost    : { Map options, Closure callback -> callback([status: 200]) },
                _time_utcNow: { -> "stubbed-utc-now" }
            ]
        )
    }

    @Override
    def cleanup() {
        scriptFile?.delete()
    }

    def "script stubs handle httpPost and helper overrides in integration harness"() {
        expect:
            deviceScript.callHttpPost() == 200
            deviceScript.callTimeHelper() == "stubbed-utc-now"
    }

    def "device.updateSetting(String, Map) updates settings and avoids ABI errors"() {
        expect:
            deviceScript.ensureSettingDefault() == true
            device.getSetting("enableDebugLogging") == true
            deviceScript.settings.enableDebugLogging == true
    }

    def "registerScriptStub can be used as a post-initialize replacement for direct metaClass edits"() {
        when:
            registerScriptStub("_time_utcNow", { -> "stubbed-later" })

        then:
            deviceScript.callTimeHelper() == "stubbed-later"
    }
}
