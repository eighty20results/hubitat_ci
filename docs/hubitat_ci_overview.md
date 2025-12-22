# Hubitat CI overview

Audience: Groovy/Hubitat developers (including newcomers) who want a fast local and CI test harness for Hubitat apps and drivers.

## What the library is
- Provides Groovy sandboxes (`HubitatAppSandbox`, `HubitatDeviceSandbox`) to compile and execute Hubitat scripts outside the hub.
- Performs structural validation of `definition()`, `preferences()`, capabilities, commands, and `parse()` signatures.
- Emits captured metadata (definition, preferences, mappings) so tests can assert against what the script declared.
- Ships Hubitat API facades (`api.*`) to mock platform calls in tests.

## Key components
- **Sandboxes**: Wrap GroovyShell with custom classloader, AST checks, and optional validation flags.
- **Validation**: Defaults on; selective opt-out via `me.biocomp.hubitat_ci.validation.Flags` (e.g., `DontValidateDefinition`, `DontRunScript`).
- **Produced data**: Accessors on the returned script (e.g., `getProducedDefinition()`, `getProducedPreferences()`, `getProducedMappings()` for apps) expose captured setup data.
- **API facades**: `me.biocomp.hubitat_ci.api.app_api.AppExecutor` and `me.biocomp.hubitat_ci.api.device_api.DeviceExecutor` define the supported Hubitat surface for mocking.
- **Utilities**: Time control via `TimeKeeper`, class-name remapping for Hubitat types, and validation helpers.

## Security and sandboxing model
- AST-level restrictions: only whitelisted classes/properties/method calls are allowed; common unsafe/dynamic constructs (reflection, `sleep`, `println`, `methodMissing`, `propertyMissing`, class definitions) are rejected.
- Script-scope `private` modifiers are stripped to ease testing; avoid relying on privacy for invariants in tests.
- `new Date()` is rewritten to `TimeKeeper.now()` for deterministic time during tests.
- Hubitat platform classes are remapped to safe stand-ins (see `util/SandboxClassLoader.groovy`), so platform calls cannot hit real devices/cloud.
- Flags allow relaxing checks (`DontRestrictGroovy`, etc.), but doing so reduces safety—prefer extending whitelists/mocks instead.

## Versioning and compatibility
- Published artifact coordinates: `groupId` `me.biocomp.hubitat_ci`, `artifactId` `hubitat_ci`, `version` `0.25.2` (from build script at time of writing).
- Tested with Groovy 2.5.x; target JDK 11 for best compatibility. Later JDKs may work but are not guaranteed.
- Local checkout: you can build/publish to your local Maven repo (`mvnLocal`) or use a flatDir JAR reference when iterating.

## When to use it
- Rapid feedback for Hubitat apps/drivers without deploying to the hub.
- CI validation of metadata, capabilities, and basic behavior via mocks.
- Regressions tests for parsing, scheduling, mappings, and state handling.

## Limitations to keep in mind
- Any platform API not modeled in `api.*` or whitelisted classes will fail validation until mocked or permitted.
- Class definitions inside scripts are blocked; keep code script-style or refactor helpers into separate files loaded as scripts.
- Network/filesystem operations are not automatically blocked beyond whitelisted types—mock or guard them explicitly.
- Dynamic metaprogramming beyond simple metaclass tweaks may be rejected by AST checks.

