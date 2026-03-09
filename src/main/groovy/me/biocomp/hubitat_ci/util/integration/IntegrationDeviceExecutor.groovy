package me.biocomp.hubitat_ci.util.integration

import me.biocomp.hubitat_ci.api.device_api.DeviceExecutor
import me.biocomp.hubitat_ci.api.common_api.BaseScheduler
import me.biocomp.hubitat_ci.device.HubitatDeviceScript
import me.biocomp.hubitat_ci.util.integration.DeviceEventArgs
import me.biocomp.hubitat_ci.util.integration.PassthroughScheduler

/**
* An implementation of portions of DeviceExecutor that are useful for integration tests.
* It is not a full implementation of the DeviceExecutor abstract class, so it
* is still expected to be wrapped in a Spock Spy.
* The parts that are implemented are:
* - Time methods from BaseExecutor trait
* - Select methods from DeviceExecutor trait
* - Methods from BaseScheduler trait (passed through to a BaseScheduler dependency)
*/
abstract class IntegrationDeviceExecutor implements DeviceExecutor, PassthroughScheduler {
    private static final groovy.json.JsonSlurper JSON_SLURPER = new groovy.json.JsonSlurper()
    private final Map<String, Closure> methodStubs = [:]

    IntegrationDeviceExecutor() {
        this((BaseScheduler) null)
    }

    IntegrationDeviceExecutor(Map args) {
        this(args?.scheduler as BaseScheduler)
    }
    IntegrationDeviceExecutor(BaseScheduler scheduler) {
        this.scheduler = scheduler
    }

    /**************************************************************
     * BEGIN SECTION: Time methods from BaseExecutor trait
     **************************************************************/

    @Override
    long now() {
        return TimeKeeper.now().getTime()
    }

    @Override
    Date toDateTime(String dateTimeString) {
        // Hubitat hub converts dates to strings when you store them in state.  It uses the
        // format: 2020-11-02T14:32:17+0000
        // Hubitat provides the toDateTime(String) method to convert back to a Date object.
        return Date.parse("yyyy-MM-dd'T'HH:mm:ssZ", dateTimeString)
    }

    /**************************************************************
     * END SECTION: Time methods from BaseExecutor trait
     **************************************************************/


    /*****************************************************************
     * BEGIN SECTION: Select methods from DeviceExecutor trait
     *****************************************************************/


    @Override
    void sendEvent(Map properties) {
        // FUTURE:  It might be desirable to send the event to apps that subscribe.
        // For example, if you were trying to system test both an app script and a device script
        // at the same time.  However, that would require allowing AppSandbox/Executor/Wrapper to work
        // together with DeviceSandbox/Executor/Wrapper, and that's not supported yet. So this is
        // commented out for now.
        // def deviceId = 0    // device.getIdAsLong()
        // def generatedEvent = new DeviceEventArgs(deviceId, device, properties.name, properties.value)
        // throw new Exception("Need to send it somewhere.")

        device.sendEvent(properties)
    }

    /*****************************************************************
     * END SECTION: Select methods from DeviceExecutor trait
     *****************************************************************/

    /*****************************************************************
     * BEGIN SECTION: HTTP methods from BaseHttp trait
     *****************************************************************/

    /**
     * Register or replace a named method stub used by integration tests.
     * Supported API names include (at minimum): httpGet, httpPost.
     */
    void registerMethodStub(String methodName, Closure stub) {
        if (stub == null) {
            methodStubs.remove(methodName)
            return
        }
        methodStubs.put(methodName, stub)
    }

    private Closure findStub(List<String> keys) {
        for (String key in keys) {
            Closure stub = methodStubs.get(key)
            if (stub != null) {
                return stub
            }
        }
        return null
    }

    /**
     * HTTP GET method stub for testing.
     * Tests should stub/mock this method to provide appropriate responses.
     */
    @Override
    Object httpGet(String address, Closure handler) {
        Closure stub = findStub(['httpGetString', 'httpGet'])
        if (stub != null) {
            return stub.call(address, handler)
        }

        throw new UnsupportedOperationException(
            "httpGet(String, Closure) is not implemented in IntegrationDeviceExecutor. " +
            "Please stub/mock this method in your test."
        )
    }

    /**
     * HTTP GET method stub for testing.
     * Tests should stub/mock this method to provide appropriate responses.
     */
    @Override
    Object httpGet(Map options, Closure handler) {
        Closure stub = findStub(['httpGetMap', 'httpGet'])
        if (stub != null) {
            return stub.call(options, handler)
        }

        throw new UnsupportedOperationException(
            "httpGet(Map, Closure) is not implemented in IntegrationDeviceExecutor. " +
            "Please stub/mock this method in your test."
        )
    }

    /**
     * HTTP POST method stub for testing.
     * Tests should stub/mock this method to provide appropriate responses.
     */
    @Override
    void httpPost(Map options, Closure handler) {
        Closure stub = findStub(['httpPostMap', 'httpPost'])
        if (stub != null) {
            stub.call(options, handler)
            return
        }

        throw new UnsupportedOperationException(
            "httpPost(Map, Closure) is not implemented in IntegrationDeviceExecutor. " +
            "Please stub/mock this method in your test."
        )
    }

    /**
     * HTTP POST method stub for testing.
     * Tests should stub/mock this method to provide appropriate responses.
     */
    @Override
    void httpPost(String address, String request, Closure handler) {
        Closure stub = findStub(['httpPostString', 'httpPost'])
        if (stub != null) {
            stub.call(address, request, handler)
            return
        }

        throw new UnsupportedOperationException(
            "httpPost(String, String, Closure) is not implemented in IntegrationDeviceExecutor. " +
            "Please stub/mock this method in your test."
        )
    }

    /*****************************************************************
     * END SECTION: HTTP methods from BaseHttp trait
     *****************************************************************/

    @Override
    def parseJson(String json) {
        JSON_SLURPER.parseText(json)
    }

    void setSubscribingScript(HubitatDeviceScript script) {
        this.scheduler?.setHandlingObject(script)
    }
}
