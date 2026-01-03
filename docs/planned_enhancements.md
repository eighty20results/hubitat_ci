### Root causes the tests exposed in hubitat_ci (analysis)

**When**: Jan 03, 2026<br/>
**Author**: chatgpt-5-mini

The following root causes were exposed by the original MyDishwasherDriverSpec.groovy and MyDishwasherDriver.groovy files:

1. Missing LOG_LEVELS / DEFAULT_LOG_LEVEL visibility for a static closure in user scripts
    - The driver uses a Utils_create() closure that references LOG_LEVELS and DEFAULT_LOG_LEVEL when it is created.
    - When the test/sandbox parsed the driver script, those fields were not always present/visible to the closure and produced MissingPropertyException.
    - Workaround in test: inject LOG_LEVELS/DEFAULT_LOG_LEVEL into the driver text before parsing (or via the script instance/metaClass).

2. HomeConnectAPI patching in tests produced a Map literal replacement which doesn't expose methods
    - The HomeConnect app expects HomeConnectAPI to be an object with methods (getStatus(haId, cb), getSettings(...), etc.).
    - Replacing it with a map literal leads to MissingMethodException when the app or driver calls HomeConnectAPI.getStatus(...).
    - Workaround in test: use an Expando (object with named methods) rather than a plain map.

3. customizeScriptBeforeRun / setProperty timing NPE
    - customizeScriptBeforeRun executes early in HubitatDeviceScript.initialize() (before DeviceMetadataReader sets up userSettingsMap).
    - If customizeScriptBeforeRun tries to set properties on the script with script.someProperty = ..., ScriptUtil.handleSetProperty gets a null userSettingsMap and ends up NPEing.
    - In tests I avoided calling setProperty directly in customizeScriptBeforeRun or set everything into the sandbox globals/ metaClass instead.
    - This indicates hubitat_ci should be more defensive: either run customizeScriptBeforeRun after metadata reader is installed or make handleSetProperty null-safe.

4. parent resolution: Installed wrapper vs script method dispatch
    - The app is wrapped by InstalledAppWrapperImpl; it uses methodMissing to forward calls to `script`.
    - If the test patches intializeStatus incorrectly on the app only (and not on the wrapper), the driver may call a wrapper method that doesn't exist — causing method dispatch problems. Setting the method on both app instance metaClass and app.getClass().metaClass (the wrapper) solves it.

### Concrete recommendations for hubitat_ci fixes

Concrete recommendations / fixes to hubitat_ci (what to change in the library) Below are safe, minimal hubitat_ci code changes I recommend to avoid requiring test hacks. I list the file and function and explain the change and why it helps.

1. Make ScriptUtil.handleSetProperty tolerant of a null userSettingsMap
    - File: src/main/groovy/me/biocomp/hubitat_ci/util/ScriptUtil.groovy
    - Function: handleSetProperty(String property, def scriptObject, def newValue, Map userSettingsMap)
    - Problem: if userSettingsMap is null, the current implementation does userSettingsMap.put(property, newValue) and raises NPE when customizeScriptBeforeRun sets script properties early.
    - Fix (concept): check if userSettingsMap is null; if so, set the property on scriptObject's metaClass (or create a local backing map). Example algorithm:
      * if (userSettingsMap == null) { if (scriptObject.getMetaClass().hasProperty(scriptObject, property)) { scriptObject.getMetaClass().setProperty(scriptObject, property, newValue) return } else { // fallback: create a local map on the script object (backing state) if (scriptObject.@internalState == null) scriptObject.@internalState = [:] scriptObject.@internalState[property] = newValue return } }
      * Otherwise keep existing setter logic.

    - Rationale: makes tests and real driver scripts robust when tests or user code set properties early.

2. Consider moving customizeScriptBeforeRun timing (alternative)
   - File: src/main/groovy/me/biocomp/hubitat_ci/device/HubitatDeviceScript.groovy
   - Change: call customizeScriptBeforeRun after metadataReader is created (so userSettingsMap exists) — or add a second hook runAfterMetadata for callers wanting to patch metaClass early. 
   - Rationale: avoids the null userSettingsMap mismatch and allows customizeScriptBeforeRun to safely set script properties. This might be safer than changing ScriptUtil, but could change behavior tests rely on (some tests patch metaClass before metadataReader runs). If you change ordering, add a dedicated flag/option to opt into the old behavior.

3. In HubitatAppSandbox, when tests replace @Field HomeConnectAPI using string replace, prefer creating an Expando object (or ensure replacements use new groovy.util.Expando(...)) not a map literal
   - File: src/main/groovy/me/biocomp/hubitat_ci/app/HubitatAppSandbox.groovy (or document recommended test helper)
   - Change: If the test or a helper is providing a replacement HomeConnectAPI text, use Expando to preserve method invocation semantics. The Unit-test helper code doing textual patching should wrap replacements with new groovy.util.Expando(...).
   - Rationale: method invocation on Expando works as expected whereas a literal Map may cause MissingMethodException.

4. Provide a small test helper / default globals on driver sandbox
   - File: src/main/groovy/me/biocomp/hubitat_ci/device/HubitatDeviceSandbox.groovy (or HubitatDeviceScript initialization)
   - Change: when creating a driver script, if LOG_LEVELS/DEFAULT_LOG_LEVEL are missing, inject sane defaults into the script metaClass/globals so Utils_create() closure has what it expects.
   - Rationale: makes many driver scripts (that rely on LOG_LEVELS) behave without tests having to patch script text.

5. Improve InstalledAppWrapperImpl delegation documentation (optional)
   - File: src/main/groovy/me/biocomp/hubitat_ci/app/child/InstalledAppWrapperImpl.groovy
   - Note: This already uses methodMissing to forward to the script. Tests that patch instance methods must also patch wrapper-level metaClass to ensure wrapper.method(...) resolves. Document this behavior in HubitatAppSandbox docs or implement a small convenience method to set the method on both script and wrapper.

### Minimal patch update recommendations

1. ScriptUtil.handleSetProperty
   - Add a null check for userSettingsMap at the end and handle property assignment through metaClass or a temporary internal state map.

2. HubitatDeviceScript.initialize
   - Consider moving customizeScriptBeforeRun call to after building metadataReader, or expose a new option to control the order.

### Small roadmap
Potential next steps:

* Option A (low risk): Implement the ScriptUtil.handleSetProperty null-safe change (small and local). I can make that change and run the full test suite. 
* Option B (safer for backwards compatibility): Add a new option to HubitatDeviceSandbox.run e.g., runAfterMetadata: true/false; if true run customizeScriptBeforeRun after metadataReader initialize. Implement default behavior unchanged. I can implement and run tests.
* Option C (helpful helper): Add a small helper in HubitatDeviceSandbox that will inject LOG_LEVELS and DEFAULT_LOG_LEVEL into scripts that lack them (only for tests), then run tests.