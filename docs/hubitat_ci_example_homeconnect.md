# Hubitat CI example: Home Connect-style driver

This is a minimal, hypothetical example showing how to test a Hubitat device driver with hubitat_ci. It mirrors patterns you might use in the Hubitat Home Connect drivers without copying their code.

## Setup (Gradle)
In your driver project’s `build.gradle`:
```groovy
dependencies {
    testImplementation 'me.biocomp.hubitat_ci:hubitat_ci:0.25.2'
    testImplementation 'org.spockframework:spock-core:1.2-groovy-2.5'
}
```
Ensure JDK 11 is configured for the project.

## Hypothetical driver snippet (`homeconnect_driver.groovy`)
```groovy
metadata {
    definition(name: "HomeConnectDemo", namespace: "demo", author: "you") {
        capability "Switch"
        command "startProgram"
    }
}

def parse(String desc) {
    // parse device message
    [name: "switch", value: "on", descriptionText: desc]
}

def startProgram(String program) {
    sendEvent(name: "lastProgram", value: program)
}
```

## Minimal Spock test (`src/test/groovy/HomeConnectSpec.groovy`)
```groovy
import me.biocomp.hubitat_ci.device.HubitatDeviceSandbox
import me.biocomp.hubitat_ci.api.device_api.DeviceExecutor
import spock.lang.Specification

class HomeConnectSpec extends Specification {
    def "validates metadata and startProgram behavior"() {
        given:
        DeviceExecutor api = Mock() {
            _ * sendEvent(_ as Map) >> { args -> args[0] }
            // parent calls are not modeled here; if your driver calls parent methods, mock them accordingly
        }

        def sandbox = new HubitatDeviceSandbox(new File("homeconnect_driver.groovy"))
        def driver = sandbox.run(api: api)

        expect:
        driver.getProducedDefinition().capabilities.contains("Switch")
        driver.startProgram("Eco")
    }
}
```

## Running the test
```bash
./gradlew test
```

## If an API is missing
If the real Home Connect driver calls Hubitat APIs not yet modeled (e.g., HTTP helpers, scheduling), you have options:
- Mock them on the `DeviceExecutor` interface by extending its behavior in your tests.
- Add extra allowed classes/expressions to the sandbox constructor if the AST checker blocks them.
- As a last resort, temporarily use `validationFlags: [Flags.DontRestrictGroovy]` to identify missing pieces, then tighten back by adding proper mocks/whitelists.

## Sandboxing caveats for Home Connect-like code
- Avoid defining inner classes inside scripts; the sandbox rejects class definitions.
- Dynamic metaprogramming and reflection are restricted; prefer explicit methods and mocks.
- Time-sensitive code should rely on injected time or `TimeKeeper.now()` for determinism.
- Network calls should be mocked; the sandbox does not automatically block I/O beyond the class whitelist.
