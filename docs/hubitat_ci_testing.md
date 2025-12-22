# Hubitat CI testing guide

## How the harness works
- Use `HubitatAppSandbox` for apps and `HubitatDeviceSandbox` for drivers. Construct with a `File` or inline script text.
- Call `run(...)` to compile, validate, and get a script instance. Common params:
  - `api`: mock implementing `AppExecutor` or `DeviceExecutor` to handle platform calls.
  - `customizeScriptBeforeRun`: closure to tweak metaclass or inject collaborators before first call.
  - `validationFlags`: list of `Flags` to relax/skip validations (e.g., `DontValidateDefinition`, `DontRunScript`, `DontRestrictGroovy`).
- The returned script exposes captured setup data, e.g., `getProducedDefinition()`, `getProducedPreferences()`, `getProducedMappings()` (apps), and device preferences/definition for drivers.

## Minimal Spock examples
App:
```groovy
import me.biocomp.hubitat_ci.app.HubitatAppSandbox
import spock.lang.Specification

class AppSpec extends Specification {
    def "basic app validation"() {
        expect:
        new HubitatAppSandbox(new File("app_script.groovy")).run()
    }
}
```
Device:
```groovy
import me.biocomp.hubitat_ci.device.HubitatDeviceSandbox
import spock.lang.Specification

class DeviceSpec extends Specification {
    def "basic device validation"() {
        expect:
        new HubitatDeviceSandbox(new File("device_script.groovy")).run()
    }
}
```

## Mocking Hubitat API calls
Provide a mock executor via `api`:
```groovy
import me.biocomp.hubitat_ci.app.HubitatAppSandbox
import me.biocomp.hubitat_ci.api.app_api.AppExecutor
import spock.lang.Specification

class ApiMockSpec extends Specification {
    def "mock hub UID"() {
        given:
        AppExecutor api = Mock() {
            1 * getHubUID() >> "Mocked UID"
        }

        def script = new HubitatAppSandbox(new File("app_script.groovy")).run(api: api)

        expect:
        script.myHubUIdMethod() == "Mocked UID"
    }
}
```

## Overriding script internals
Use `customizeScriptBeforeRun` to swap private methods (privacy is stripped for scripts):
```groovy
def script = new HubitatDeviceSandbox(new File("device_script.groovy")).run(
    customizeScriptBeforeRun: { s ->
        s.metaClass.parse = { String msg -> [source: msg, result: 1] }
    }
)
```

## Deterministic time
`new Date()` is rewritten to `TimeKeeper.now()`. For time-sensitive tests, stub `TimeKeeper.now = { fixedInstant }` within the test JVM (restore afterward) or inject time into your code paths.

## Validation flags
Selected flags (see `me.biocomp.hubitat_ci.validation.Flags` for full list):
- `DontRunScript`: skip initialization.
- `DontValidateDefinition`, `DontValidatePreferences`: skip specific checks.
- `AllowNullEnumInputOptions`, `AllowNullListOptions`: relax input validation.
- `DontRestrictGroovy`: disable AST restrictions (use sparingly; prefer adding whitelisted classes/expressions).

## Common pitfalls
- Platform APIs not modeled in `api.*` must be mocked or whitelisted; otherwise validation fails.
- Class definitions inside scripts are rejected.
- Dynamic metaprogramming (`methodMissing`, `propertyMissing`, heavy reflection) is blocked by the sandbox.
- Network/filesystem calls aren’t automatically mocked—guard or mock them.

## Running tests
From a Gradle project:
```bash
gradlew test
```
From Maven:
```bash
mvn test
```
From plain Groovy with Spock on classpath (and @Grab):
```bash
groovy -cp "path/to/spock-core.jar" src/test/groovy/AppSpec.groovy
```

## GitHub Actions (Gradle) snippet
Save as `.github/workflows/ci.yml`:
```yaml
name: CI
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 11
        uses: actions/setup-java@v4
        with:
          java-version: '11'
          distribution: 'temurin'
      - name: Cache Gradle
        uses: actions/cache@v4
        with:
          path: ~/.gradle/caches
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
          restore-keys: ${{ runner.os }}-gradle-
      - name: Build & test
        run: ./gradlew clean test
```

## GitHub Actions (Maven) snippet
Save as `.github/workflows/ci.yml`:
```yaml
name: CI
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 11
        uses: actions/setup-java@v4
        with:
          java-version: '11'
          distribution: 'temurin'
          cache: maven
      - name: Build & test
        run: mvn -B clean test
```

## Extending the sandbox
- Add allowed classes/expressions by supplying constructor args to sandboxes or by relaxing flags judiciously.
- Provide mocks for any Hubitat capability or platform service your script uses that isn’t in the shipped API facades.
- Keep `DontRestrictGroovy` off unless debugging; re-enable once additional whitelisting/mocks are in place.
