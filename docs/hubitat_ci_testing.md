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
- `Default`: baseline validation set.
- `DontRunScript`: skip initialization/execute after compile.
- `DontRestrictGroovy`: disable AST/groovy restriction checks (use sparingly).
- `AllowNullListOptions`: allow null list options in inputs.
- `AllowEmptyOptionValueStrings`: permit required option strings to be empty.

**App-specific**
- `DontValidateMetadata`, `DontValidatePreferences`, `DontValidateDefinition`, `DontValidateMappings`, `DontValidateCapabilities`, `DontValidateAttributes`: skip respective metadata/preferences/definition/mappings/capability/attribute validations.
- `DontValidateSubscriptions`: skip subscribe() argument validation.
- `AllowAnyDeviceAttributeOrCapabilityInSubscribe`: allow any attr/cap name in subscribe.
- `AllowAnyExistingDeviceAttributeOrCapabilityInSubscribe`: allow any existing attr/cap in subscribe.
- `AllowAnyAttributeEnumValuesInSubscribe`: allow any enum values in subscribe attribute filters.
- `DontValidateDeviceInputName`: skip device input name checks.
- `AllowWritingToSettings`: allow writing to settings during validation.
- `AllowReadingNonInputSettings`: allow reads of settings not declared as inputs.
- `AllowUnreachablePages`: allow pages not reachable from navigation.
- `AllowTitleInPageCallingMethods`: allow title in page-calling methods.
- `AllowMissingOAuthPage`: do not require OAuth page.
- `AllowMissingInstall`: do not require at least one install:true page.

**Driver-specific**
- `AllowCommandDefinitionWithNoArgsMatchAnyCommandWithSameName`: allow commands without matching arg signatures.
- `AllowSectionsInDevicePreferences`: allow section() inside driver preferences.
- `AllowMissingCommandMethod`: skip requiring a backing method for command args.
- `AllowMissingDeviceInputNameOrType`: allow missing name/type in device inputs.
- `DontRequireCapabilityImplementationMethods`: allow missing capability methods.
- `DontRequireParseMethodInDevice`: do not require parse().
- `AllowNullEnumInputOptions`: allow enum inputs with null options.
- `AllowLegacyImports`: use legacy import whitelist (pre forum/docs merge).
- `StrictEnumInputValidation`: enforce strict enum input validation (options required, default checked). Off by default; enable to be stricter.

Use `Flags.from(...)` or a list literal when passing to sandboxes, e.g. `.run(validationFlags: [Flags.DontRunScript, Flags.AllowLegacyImports])`.

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
