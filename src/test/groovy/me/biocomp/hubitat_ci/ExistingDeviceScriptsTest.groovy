package me.biocomp.hubitat_ci

import me.biocomp.hubitat_ci.api.common_api.Log
import me.biocomp.hubitat_ci.api.device_api.DeviceExecutor
import me.biocomp.hubitat_ci.device.HubitatDeviceSandbox
import me.biocomp.hubitat_ci.util.SubmoduleFixtureLoader
import me.biocomp.hubitat_ci.validation.Flags
import spock.lang.Shared
import spock.lang.Specification

class WeatherDisplayScriptTest extends
        Specification
{
    HubitatDeviceSandbox sandbox = new HubitatDeviceSandbox(new File("Scripts/Devices/Weather-Display With External Forecast.groovy"))

    def "Basic validation"() {
        setup:
            def log = Mock(Log)
            DeviceExecutor api = Mock{ _ * getLog() >> log }

        expect:
            sandbox.run(api: api, validationFlags: [Flags.AllowSectionsInDevicePreferences, Flags.AllowWritingToSettings])
    }
}

class Fibaro223SciptTest extends Specification
{
    @Shared
    static final List<File> REQUIRED_SUBMODULE_FILES = SubmoduleFixtureLoader.loadSubmoduleFixtures()

    def setupSpec() {
        def missing = REQUIRED_SUBMODULE_FILES.findAll { !it.exists() }
        if (!missing.isEmpty()) {
            throw new FileNotFoundException("Missing required submodule fixtures: ${missing.join(', ')}. For local development, run 'git submodule update --init --recursive'. If running in CI, ensure your checkout step fetches submodules recursively (for example, by using 'submodules: recursive' with actions/checkout).")
        }
    }

    HubitatDeviceSandbox sandbox = new HubitatDeviceSandbox(new File("SubmodulesWithScripts/Hubitat/Drivers/fibaro-double-switch-2-fgs-223.src/fibaro-double-switch-2-fgs-223.groovy"))

    def "Basic validation"() {
        expect:
            sandbox.run(validationFlags: [Flags.AllowMissingDeviceInputNameOrType, Flags.AllowCommandDefinitionWithNoArgsMatchAnyCommandWithSameName])
    }
}