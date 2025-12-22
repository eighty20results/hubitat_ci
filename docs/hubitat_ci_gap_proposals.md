# Hubitat CI proposals: extend allowed imports and driver metadata validation

Goal: strict enforcement aligned with Hubitat docs and the Hubitat forum allowed-import list; add coverage for allowed imports, driver capabilities/attributes/commands/preferences; outline code/doc changes and required Spock tests (Gradle). Include an optional flag to relax to legacy behavior.

## Patch plan (sequence)
1) Update allowed-import whitelist and hubitat helper mappings per forum list + docs.
2) Refresh capability/attribute catalog vs Hubitat docs; add missing capabilities and enums.
3) Extend driver preferences validation to cover additional input types/constraints.
4) Add strict validation options (default on) and keep opt-outs via Flags (including a new relax-imports flag).
5) Add Spock tests for new validations and mappings.
6) Update docs to reflect expanded support and strictness.

## Proposed code changes
### Allowed imports / class whitelist
- File: `validation/ValidatorBase.groovy`
  - Expand `classOriginalWhiteList` to include forum-allowed imports: `groovy.transform.Field`, `groovy.json.JsonSlurperClassic`, `groovy.xml.StreamingMarkupBuilder`, `groovy.sql.Sql`, `java.security.MessageDigest`, `java.security.SecureRandom`, `java.util.concurrent.*` (e.g., `ConcurrentHashMap`, `ConcurrentLinkedQueue`), `java.util.regex.Pattern`, `java.time.*` (if allowed), `javax.crypto` classes if listed, `java.net.URL`.
  - Add hubitat helper classes to whitelist: `hubitat.helper.ColorUtils`, `hubitat.helper.HexUtils`, `hubitat.helper.RMUtils`, `hubitat.helper.ZigbeeUtils` (mapped via SandboxClassLoader).
  - Introduce a new flag (e.g., `AllowLegacyImports`) to relax to previous whitelist when needed; default remains strict.

- File: `util/SandboxClassLoader.groovy`
  - Add class name mappings for helper utilities: `hubitat.helper.ColorUtils`, `hubitat.helper.HexUtils`, `hubitat.helper.RMUtils`, `hubitat.helper.ZigbeeUtils` → stubs in `api.common_api` (add stubs as needed).
  - Add mappings for any additional hubitat.* package classes present in allowed imports but not yet mapped.

- New stubs: under `src/main/groovy/me/biocomp/hubitat_ci/api/common_api/` add minimal helper classes with signatures from the forum/docs; they can throw unless mocked.

### Capabilities / attributes / commands
- File: `capabilities/AllCapabilities.groovy`
  - Add missing Hubitat capabilities per docs (e.g., `AirQuality`, `Light`, `PowerSource`, `ReleasableButton`, `PushableButton`, `VoltageMeasurement`, `PM2_5`, `PM10`, full `WindowShade` states, `MediaPlayback`, etc.).
  - Update enums to match Hubitat (e.g., `powerSource` values, `windowShade` states, button events aligned with `PushableButton`/`HoldableButton`/`ReleasableButton`).
  - Ensure required attributes/commands per capability are declared.

- Command parameter validation (likely in metadata validators):
  - Support Hubitat types: `ENUM` (with options), `JSON_OBJECT`, `VECTOR3`, `COLOR_MAP`, `NUMBER/DECIMAL`, `INTEGER`, `LONG`, `BOOLEAN`, `STRING`, `DATE`, `TIME`.
  - Enforce matching method signatures unless `Flags.AllowMissingCommandMethod` is set.

### Preferences
- File: `device/metadata/DeviceInput.groovy`
  - Expand `validStaticInputTypes` to include: `date`, `textarea` (if supported), selectors `mode`, `hub`, `device.<capability>`, `capability.<capability>`, `icon`, and `enum` with `multiple`.
  - Validate `multiple`, `submitOnChange`, `options` non-empty for enum (unless a flag), `required`, `defaultValue` consistency, numeric `range` for number/decimal.
  - Add selector-aware handling for capability/device inputs (record capability filters, etc.).

### Validation strictness and flags
- Default strict mode uses updated lists. 
- Add new flag `AllowLegacyImports` (or similar) to revert to prior whitelist/import behavior when needed.
- Keep existing relax flags (`DontRestrictGroovy`, etc.) for broader opt-out.

### Docs updates
- Update `docs/hubitat_ci_overview.md` and `docs/hubitat_ci_testing.md` to state alignment with the forum allowed-import list and list key supported helper classes; note the relax-imports flag.
- Update `docs/hubitat_ci_gap_report.md`/proposals to reflect the stricter default and optional relax flag.

## Tests (Spock, Gradle)
- Whitelist tests: scripts importing each forum-allowed import (Field, JsonSlurperClassic, StreamingMarkupBuilder, Sql, MessageDigest, SecureRandom, ColorUtils, HexUtils, RMUtils, ZigbeeUtils) compile; disallowed imports fail under strict mode and pass only with the relax flag.
- Capability coverage: table-driven spec to assert new capabilities/attributes/commands are recognized; outdated enums should fail.
- Preferences: tests for `capability.switch`, `device.switch`, `mode`, `hub`, `textarea`, `enum` with options/multiple, numeric range validation (pass/fail cases).
- Command parameters: tests for supported types vs unsupported types; signature matching enforced unless the relevant flag is set.

## Work breakdown (outline)
1. Implement whitelist/mapping/stub additions + tests.
2. Refresh capabilities/enums + tests.
3. Extend preferences validation + tests.
4. Extend command parameter validation + tests.
5. Update docs for new defaults and relax flag.
