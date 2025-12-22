# Hubitat CI gap report (allowed imports & driver metadata)

Scope: Compare current hubitat_ci sandbox/validators against Hubitat driver requirements for allowed imports and driver metadata (definition, capabilities, attributes, commands, preferences). Sources checked: project code (`ValidatorBase`, `SandboxClassLoader`, `device/metadata/*`, `capabilities/AllCapabilities.groovy`) vs Hubitat docs (allowed imports, capabilities list, attribute object, driver preferences) and the Hubitat forum allowed-imports list (https://community.hubitat.com/t/is-there-a-list-of-allowed-groovy-imports-and-libraries/81759/2).

## Allowed imports / class whitelist
- Whitelist is static in `validation/ValidatorBase.classOriginalWhiteList`; it’s Groovy/JDK-centric. Missing relative to the forum’s allowed list include: `groovy.transform.Field`; `groovy.json.JsonSlurperClassic`; `groovy.xml.StreamingMarkupBuilder`; `groovy.sql.Sql`; `java.security.MessageDigest`; `java.security.SecureRandom`; `java.util.concurrent.*` (e.g., `ConcurrentHashMap`, `ConcurrentLinkedQueue`); `java.util.regex.Pattern`; additional XML helpers (`groovy.xml.StreamingMarkupBuilder`); and broader date/time utilities (e.g., `java.time.*` if permitted). 
- Hubitat helper classes called out in the forum list (`hubitat.helper.ColorUtils`, `hubitat.helper.HexUtils`, `hubitat.helper.RMUtils`, `hubitat.helper.ZigbeeUtils`) are not mapped/whitelisted; `SandboxClassLoader` only maps a subset of `hubitat.*` classes.
- No structured alignment to the published allowed-import list; adding many imports is manual. Forbidden expressions remain (`println`, `sleep`, reflection hooks), which differs from Hubitat’s guidance that focuses on import-level restrictions rather than expression-level bans.
- Net: Scripts using forum-allowed imports or helper classes will fail AST checks today.

## Driver metadata: definition/capabilities
- Capability list in `capabilities/AllCapabilities.groovy` is SmartThings-era; likely missing newer Hubitat capabilities (`AirQuality`, `Light`, `PowerSource`, `ReleasableButton`, `PushableButton`, `VoltageMeasurement`, `PM2_5`, `PM10`, fuller `WindowShade` states, `MediaPlayback`, etc.). No automated sync with the published capability list; drift risk is high.

## Driver metadata: attributes
- Attributes recorded as name/type/values, but validation relies on capability-embedded schemas; newer enums (e.g., `powerSource`, `windowShade` positions) may be outdated or missing. Custom attributes are unvalidated.

## Driver metadata: commands
- Command validation is lenient on parameter types and method matching. Hubitat supports types like `ENUM` with options, `JSON_OBJECT`, `VECTOR3`, `COLOR_MAP`, `DATE`, `TIME`, which aren’t enforced.

## Preferences
- Supported types (`DeviceInput.validStaticInputTypes`): `bool, decimal, email, enum, number, password, phone, time, text, paragraph`.
- Missing vs Hubitat docs: selectors `capability.<capabilityName>`, `device.<capabilityName>`, `mode`, `hub`, `icon`, `textarea`, `date`, `enum` with `multiple`, richer validation of `options`, `required`, `submitOnChange`, and range checking. Sections allowed only behind `AllowSectionsInDevicePreferences`.

## Other observations
- `SandboxClassLoader` lacks mappings for helper utilities; AST whitelist is class-name based and not derived from the official allowed-import list.
- Tests do not assert parity with Hubitat’s allowed-import list or the published capability/attribute/command/input catalogs.

## Impact
- Drivers using forum-allowed imports or helper classes fail to compile under hubitat_ci today.
- Capability/attribute drift reduces confidence that validations match Hubitat behavior.
- Preferences using Hubitat selectors or additional input types are unvalidated, so valid scripts may fail or invalid ones may pass.
