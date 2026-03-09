package me.biocomp.hubitat_ci.device

import spock.lang.Specification

/**
 * Regression coverage for issue #44: helper methods should accept interpolated
 * strings when calling script-local logging helpers.
 */
class DeviceScriptGStringLogCompatibilityTest extends Specification {
    def "script helper can call _log(String, GString) without MissingMethodException"() {
        given:
            def sandbox = new HubitatDeviceSandbox("""
metadata {
    definition(name: "GStringLog Device", namespace: "test", author: "test") {
        capability "Sensor"
    }
}

def logFromHelper() {
    def value = 42
    return _log("warn", "value=\${value}")
}

static String _log(String level, String message) {
    return "\${level}|\${message}"
}

Map parse(String s) { [:] }
""")

        when:
            def script = sandbox.run(noValidation: true)
            def result = script.logFromHelper()

        then:
            result == "warn|value=42"
    }
}
