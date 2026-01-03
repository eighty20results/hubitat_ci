package rferrazguimaraes.drivers

import me.biocomp.hubitat_ci.api.app_api.AppExecutor
import me.biocomp.hubitat_ci.api.common_api.DeviceWrapper
import me.biocomp.hubitat_ci.api.device_api.DeviceExecutor
import me.biocomp.hubitat_ci.app.HubitatAppSandbox
import me.biocomp.hubitat_ci.device.HubitatDeviceSandbox
import me.biocomp.hubitat_ci.validation.Flags
import spock.lang.Specification

class MyDishwasherDriverSpec extends Specification {
    HubitatDeviceSandbox driverSandbox
    HubitatAppSandbox appSandbox
    def driver
    def app
    def driverApi
    def appApi
    List<Map> appEvents = []
    List<Map> driverEvents = []
    List<Map> httpRequests = []

    def setup() {
        // Setup App API Mock
        appApi = Mock(AppExecutor)
        appApi.createAccessToken() >> "test_access_token"
        appApi.getFullApiServerUrl() >> "http://localhost/api"
        appApi.getState() >> [:]
        appApi.getAtomicState() >> [:]

        // Mock HTTP methods for the App
        appApi.httpGet(_ as Map, _ as Closure) >> { Map params, Closure closure ->
            httpRequests << [method: 'GET', params: params]
            def response = [data: [:]] // Default empty response

            if (params.uri.toString().contains("/homeappliances")) {
                response.data = [
                    homeappliances: [
                        [
                            name: "My Dishwasher",
                            brand: "BOSCH",
                            vib: "HNG6764B6",
                            connected: true,
                            type: "Dishwasher",
                            enumber: "HNG6764B6/09",
                            haId: "BOSCH-DISHWASHER-1"
                        ]
                    ]
                ]
            }

            closure.call(response)
        }

        appApi.httpPut(_ as Map, _ as Closure) >> { Map params, Closure closure ->
            httpRequests << [method: 'PUT', params: params]
            def response = [data: [:]]
            closure.call(response)
        }

        appApi.httpDelete(_ as Map, _ as Closure) >> { Map params, Closure closure ->
            httpRequests << [method: 'DELETE', params: params]
            def response = [data: [:]]
            closure.call(response)
        }

        // Setup Driver API Mock
        driverApi = Mock(DeviceExecutor)
        def deviceWrapper = Mock(DeviceWrapper)
        driverApi.getDevice() >> deviceWrapper
        driverApi.sendEvent(_ as Map) >> { Map args ->
            driverEvents << args
        }

        // Initialize App Sandbox with patched content to avoid SecurityException
        File appFile = new File("SubmodulesWithScripts/hubitat-homeconnect/apps/HomeConnect.groovy")
        String appScript = appFile.text
            .replace('e.getResponse()?.getData()', '"[redacted]"')
            .replace('e.getResponse().getData()', '"[redacted]"')
            .replace('java.util.concurrent.TimeUnit.MILLISECONDS.toHours(millis)', '(millis / 3600000)')
            .replace('java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(millis)', '(millis / 60000)')
            .replace('java.util.concurrent.TimeUnit.HOURS.toMinutes(1)', '60')
            .replace('@Field HomeConnectAPI = HomeConnectAPI_create(oAuthTokenFactory: {return getOAuthAuthToken()}, language: {return getLanguage()});', '@Field HomeConnectAPI = new groovy.util.Expando(connectDeviceEvents: { a, b -> }, disconnectDeviceEvents: { a, b -> }, getSupportedLanguages: { -> [US: "United States"] }, getHomeAppliances: { cb -> httpGet([uri: "http://localhost/api/homeappliances"], { resp -> cb(resp.data.homeappliances) }) }, getStatus: { haId, cb -> httpGet([uri: "http://localhost/api/homeappliances/${haId}/status"], { resp -> cb(resp.data ?: [:]) }) }, getSettings: { haId, cb -> httpGet([uri: "http://localhost/api/homeappliances/${haId}/settings"], { resp -> cb(resp.data ?: [:]) }) }, getActiveProgram: { haId, cb -> httpGet([uri: "http://localhost/api/homeappliances/${haId}/programs/active"], { resp -> cb(resp.data ?: [:]) }) }, setActiveProgram: { haId, programKey, options, cb -> httpPut([uri: "http://localhost/api/homeappliances/${haId}/programs/active", body: programKey], cb) }, setStopProgram: { haId, cb -> httpDelete([uri: "http://localhost/api/homeappliances/${haId}/programs/active"], cb) })')
            .replace('@Field Utils = Utils_create();', '@Field Utils = [toLogger: { l, m -> println "$l: $m" }, toFlattenMap: { m -> m }, showHideNextButton: { b -> }, toQueryString: { m -> m.collect { k, v -> "${k}=${URLEncoder.encode(v.toString(), "UTF-8")}" }.join("&") }]')
            .replace(') {}', ') { paragraph "HubitatCI Placeholder" }')

        appSandbox = new HubitatAppSandbox(appScript)
        app = appSandbox.run(
            api: appApi,
            validationFlags: [Flags.AllowSectionsInDevicePreferences, Flags.DontValidatePreferences, Flags.AllowWritingToSettings, Flags.AllowReadingNonInputSettings],
            userSettingValues: [
                clientId: "test_client_id",
                clientSecret: "test_client_secret",
                region: "US"
            ]
        )

        // Set up atomicState for the app
        app.atomicState.authToken = "mock_auth_token"
        app.atomicState.langCode = "en-US"

        // --- attach a small HomeConnect API mock to the app so driver.connect/disconnect are recorded
        def homeApiCalls = []
        def homeApi = [
            connectDeviceEvents: { String haId, def interfaces ->
                homeApiCalls << [op: 'connect', haId: haId, interfaces: interfaces]
                return true
            },
            disconnectDeviceEvents: { String haId, def interfaces ->
                homeApiCalls << [op: 'disconnect', haId: haId, interfaces: interfaces]
                return true
            },
            setActiveProgram: { String haId, String programKey, def options, Closure cb ->
                homeApiCalls << [op: 'setActiveProgram', haId: haId, key: programKey, options: options]
                cb?.call([data: [:]])
            },
            setStopProgram: { String haId, Closure cb ->
                homeApiCalls << [op: 'stopProgram', haId: haId]
                cb?.call([data: [:]])
            }
        ]

        // attach to app instance
        app.metaClass.getHomeConnectAPI = { -> homeApi }

        // Provide helper parent methods the driver expects (return program/options lists)
        app.metaClass.getAvailableProgramList = { DeviceWrapper d ->
            return [[name: 'Eco', key: 'Dishcare.Dishwasher.Program.Eco'], [name: 'Intensive', key: 'Dishcare.Dishwasher.Program.Intensive']]
        }
        app.metaClass.getAvailableProgramOptionsList = { DeviceWrapper d, String programKey ->
            return [[name: 'IntensivZone', key: 'IntensivZone'], [name: 'ExtraDry', key: 'ExtraDry']]
        }

        // Provide a simple intializeStatus implementation that simulates HTTP calls and connects event stream
        def intializeStatusClosure = { DeviceWrapper d ->
            try {
                def haId = null
                try { haId = d.getDataValue('haId') } catch (e) { haId = d.getDeviceNetworkId() }
                app.httpGet([uri: "http://localhost/api/homeappliances/${haId}/programs/active"], { resp -> })
                app.httpGet([uri: "http://localhost/api/homeappliances/${haId}/programs/selected"], { resp -> })
                app.httpGet([uri: "http://localhost/api/homeappliances/${haId}/status"], { resp -> })
                app.httpGet([uri: "http://localhost/api/homeappliances/${haId}/settings"], { resp -> })

                def homeApiLocal = app.getHomeConnectAPI()
                try { homeApiLocal.connectDeviceEvents(haId, null) } catch (ex) { /* allow tests to simulate failure */ }
            } catch (e) {
                // swallow to avoid failing tests setup
            }
        }
        app.metaClass.intializeStatus = intializeStatusClosure
        // Also set on the wrapper class so InstalledAppWrapperImpl.resolve to our stub
        app.getClass().metaClass.intializeStatus = intializeStatusClosure

        // Initialize Driver Sandbox by loading the driver script text and injecting LOG_LEVELS/DEFAULT_LOG_LEVEL
        File driverFile = new File("src/main/groovy/rferrazguimaraes/drivers/MyDishwasherDriver.groovy")
        String driverScriptText = driverFile.text
        // Ensure LOG_LEVELS and DEFAULT_LOG_LEVEL exist before Utils_create() is executed
        driverScriptText = driverScriptText.replace('@Field static Map Utils = Utils_create();', '@Field List<String> LOG_LEVELS = ["error", "warn", "info", "debug", "trace"]\n@Field String DEFAULT_LOG_LEVEL = LOG_LEVELS[1]\n@Field static Map Utils = Utils_create()')

        driverSandbox = new HubitatDeviceSandbox(driverScriptText)
        driver = driverSandbox.run(
            api: driverApi,
            parent: app,
            validationFlags: [Flags.AllowSectionsInDevicePreferences, Flags.AllowWritingToSettings, Flags.AllowReadingNonInputSettings],
            userSettingValues: [logLevel: 'debug', selectedProgram: 'Eco'],
            customizeScriptBeforeRun: { script ->
                // Provide a safe Utils.toLogger and instance logLevel so the closure resolves correctly at runtime
                try { script.metaClass.Utils = [toLogger: { level, msg -> if (level && msg) println "TESTLOG ${level}: ${script?.device?.displayName ?: 'device'} ${msg}" }] } catch (e) { script.Utils = [toLogger: { level, msg -> if (level && msg) println "TESTLOG ${level}: ${msg}" }] }
                script.metaClass.logLevel = 'debug'
            }
        )

        // Set the device for the driver
        // driver.device is now deviceWrapper
        println "driver.device class: ${driver.device?.getClass()}"
        println "driver.device: ${driver.device}"

        // Mocking the machine program listing
        String programListJson = "[\"Eco\", \"Intensive\"]"
        deviceWrapper.currentValue("AvailableProgramsList") >> programListJson
        deviceWrapper.currentValue("AvailableProgramsList", _) >> programListJson

        deviceWrapper.getDeviceNetworkId() >> "BOSCH-DISHWASHER-1"
        deviceWrapper.currentValue("AvailableOptionsList") >> null
        deviceWrapper.currentValue("AvailableOptionsList", _) >> null

        // Mock other methods called on device
        deviceWrapper.displayName >> "My Dishwasher"
        deviceWrapper.label >> "My Dishwasher"
        deviceWrapper.name >> "My Dishwasher"
        deviceWrapper.getDataValue("haId") >> "BOSCH-DISHWASHER-1"

        // Mock updateDataValue
        deviceWrapper.updateDataValue(_, _) >> { String key, String value -> }

        // Mock hasAttribute
        deviceWrapper.hasAttribute(_) >> true

        // expose the homeApiCalls and deviceWrapper to the script-level so tests can assert
        // attach as fields on the app script for visibility
        app.metaClass.getHomeApiCalls = { -> homeApiCalls }
        app.metaClass.getDeviceWrapper = { -> deviceWrapper }
    }

    def "startProgram calls parent startProgram"() {
        given:
        driver.selectedProgram = "Eco"
        // Mock available programs in state so the driver can find the key
        driver.state.foundAvailablePrograms = [
            [name: "Eco", key: "Dishcare.Dishwasher.Program.Eco"]
        ]

        when:
        driver.startProgram()

        then:
        httpRequests.find { it.method == 'PUT' && it.params.uri.toString().contains("/programs/active") }
        httpRequests.last().params.body.contains("Dishcare.Dishwasher.Program.Eco")
    }

    def "stopProgram calls parent stopProgram"() {
        when:
        driver.stopProgram()

        then:
        httpRequests.find { it.method == 'DELETE' && it.params.uri.toString().contains("/programs/active") }
    }

    def "initialize calls parent intializeStatus and connects event stream"() {
        when:
        driver.initialize()

        then:
        // parent.intializeStatus should trigger app HTTP calls
        httpRequests.any { it.method == 'GET' && it.params.uri.toString().contains("/programs/active") }
        httpRequests.any { it.method == 'GET' && it.params.uri.toString().contains("/programs/selected") }
        httpRequests.any { it.method == 'GET' && it.params.uri.toString().contains("/status") }
        httpRequests.any { it.method == 'GET' && it.params.uri.toString().contains("/settings") }

        // event stream should have been connected via the app's HomeConnectAPI mock
        def homeApiCalls = app.getHomeApiCalls()
        assert homeApiCalls.find { it.op == 'connect' && it.haId == 'BOSCH-DISHWASHER-1' }

        // driver should have emitted AvailableProgramsList event
        assert driverEvents.find { it.name == 'AvailableProgramsList' }
    }

    def "initialize handles event stream connect failure"() {
        given:
        // make connect throw
        def homeApi = app.getHomeConnectAPI()
        homeApi.connectDeviceEvents = { String haId, def interfaces -> throw new RuntimeException("boom") }

        when:
        driver.initialize()

        then:
        // driver should set EventStreamStatus to disconnected when connect fails
        assert driverEvents.find { it.name == 'EventStreamStatus' && it.value == 'disconnected' }
    }
}
