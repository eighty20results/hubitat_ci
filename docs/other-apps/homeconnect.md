# Hubitat Home Connect Integration – Detailed Usage & Test Guide

## Overview
- Parent app `apps/HomeConnect.groovy` handles OAuth, Home Connect REST/SSE, device discovery, and child driver lifecycle.
- Child drivers (CleaningRobot, CoffeeMaker, CookProcessor, Dishwasher, Dryer, FridgeFreezer, Hob, Hood, Oven, Washer, WasherDryer, WineCooler) expose Hubitat capabilities, map Home Connect status/settings/programs to attributes, and delegate commands to the parent.
- Eventing: children open SSE via parent `HomeConnectAPI.connectDeviceEvents`, parse incoming data, then parent dispatches events back to children through `processMessage` → `processData` → `sendEventToDevice`.
- Testing focus: Hubitat CI Spock specs using `HubitatAppSandbox` / `HubitatDeviceSandbox` to validate pages, device sync, command delegation, SSE parsing, and attribute emission.

## Build & Test Setup
- Gradle (root) tasks: `./gradlew test` runs hubitat_ci specs (hubitat_ci already provides Hubitat mocks/sandboxes).
- Relevant existing helpers: `me.biocomp.hubitat_ci.device.HubitatDeviceSandbox`, `me.biocomp.hubitat_ci.app.HubitatAppSandbox`, validation flags in `me.biocomp.hubitat_ci.validation.Flags` (see `src/test/groovy/me/biocomp/hubitat_ci/app/ChildDeviceLifecycleSpec.groovy`).
- Mocks already available in hubitat_ci: app API executor stub, device sandbox, lifecycle automation; you stub HTTP and parent/child calls directly in specs.

## Parent App (`apps/HomeConnect.groovy`)
### Responsibilities
- Preference pages: intro (clientId/secret, region, log level), OAuth connect, device selection.
- OAuth: builds auth URL, handles callback, exchanges/refreshes tokens, stores in `atomicState`.
- Discovery/sync: fetch appliances, create/delete child devices per selected items; maps Home Connect type → driver name.
- Device init: fetches status/settings/active program; pushes events to children.
- Command routing: start/stop program, set selected program/option, power/lighting/ambient/venting/intensive settings, program/option retrieval helpers.
- SSE/event handling: parses raw SSE lines (`processMessage`), normalizes to data map, then `processData` → `sendEventToDevice` to children.

### Lifecycle & Methods (selected)
| Method | Purpose | Notes / Test angles |
| --- | --- | --- |
| `installed()` | Log & `synchronizeDevices()` | Assert children created for selected devices. |
| `updated()` | Log & `synchronizeDevices()` | Ensure adds/removes according to settings. |
| `uninstalled()` | Delete all children | Verify cleanup. |
| `pageIntro()/pageAuthentication()/pageDevices()` | Dynamic pages for setup | Validate required inputs, next-page flow, href URL contains state/access token. |
| `synchronizeDevices()` | Add/remove children per `settings.devices` & `state.foundDevices` | Test mapping type→driver name; removal of stale children. |
| `intializeStatus(device, checkActiveProgram=true)` | Fetch status/settings/active program; send to child | Mock `HomeConnectAPI.getStatus/getSettings/getActiveProgram`; assert `sendEventToDevice` calls. |
| `startProgram/stopProgram` | Delegate to `HomeConnectAPI` | Verify correct haId and option handling. |
| `setPowertate` (typo), `setPowerState` fallback | Set power setting | Children call both spellings; test both. |
| Lighting/ambient/venting/intensive setters | Set corresponding settings | Validate payload names. |
| `getAvailableProgramList/getAvailableProgramOptionsList/getActiveProgramOption` | Fetch options/programs | Return arrays; children rely on these. |
| `setSelectedProgram/Option` | Write selected program/option | Ensure correct keys. |
| `processMessage(String|List)` | SSE line handler → `processData` | Feed sample SSE lines (data/event/error) and assert dispatch. |
| `processData(device, dataContainer)` | Route STATUS/NOTIFY/EVENT/error | Map to `sendEventToDevice`. |
| `sendEventToDevice(device, data)` | Maps Home Connect keys → Hubitat events/settings | Central attribute mapping; assert each key is covered for driver under test. |
| OAuth helpers: `generateOAuthUrl`, `oAuthCallback`, `acquireOAuthToken`, `refreshOAuthToken`, `apiRequestAccessToken`, `getOAuthAuthToken`, `HomeConnectAPI_create` | Token lifecycle & API client | Stub httpPost/httpGet; check state transitions and headers; ensure refresh before expiry. |

### HomeConnectAPI (inline factory)
- GET: appliances, status, settings, active program, available programs/options.
- PUT: set settings, active program, stop program, selected program/option.
- SSE: `connectDeviceEvents(haId, interfaces)`, `disconnectDeviceEvents` (delegates to Hubitat `interfaces.eventStream` semantics via provided interfaces object in driver).
- Headers: `Authorization: Bearer <token>`, `Accept-Language`, `accept: application/vnd.bsh.sdk.v1+json`.
- Error handling: logs `HttpResponseException`, special-case when no active program.

## Child Drivers (common patterns)
- Capabilities: mostly `Sensor`, `Switch`, many with `ContactSensor`, `Initialize`; Hood adds `FanControl`.
- Commands: `startProgram`, `stopProgram`, `deviceLog`; Hood adds `setLighting`, `setLightingBrightness`, `setIntensiveLevel`; others include light/ambient setters via parent.
- Attributes: per-appliance status/options (RemoteControlActive/StartAllowed, OperationState, DoorState, PowerState, EventPresentState, Active/SelectedProgram, EventStreamStatus, DriverVersion, plus appliance-specific option/event attributes).
- Preferences: selected program (enum from parent), per-option bools, logLevel.
- Lifecycle: `installed/updated/initialize` call `intializeStatus`, set program/options; `uninstalled` disconnects SSE; `initialize` usually connects SSE.
- Event stream: `connectEventStream/reconnectEventStream/disconnectEventStream`, `setEventStreamStatusToConnected/Disconnected` with exponential backoff retry state.
- Switch on/off: delegates power state to parent (`setPowerState` or typo `setPowertate`).
- Program/options helpers: fetch from parent lists and set selected program/option.

### Per-driver highlights & method tables

#### CleaningRobot (`drivers/CleaningRobot.groovy`)
- Extra attrs: `EmptyDustBoxAndCleanFilter`, `RobotIsStuck`, `DockingStationNotFound`.
- Methods mirror base pattern (start/stop, init/update, connect SSE, program/options helpers, power on/off).
- Test angle: parse events mapped in parent `sendEventToDevice` keys `ConsumerProducts.CleaningRobot.*`.

#### CoffeeMaker (`drivers/CoffeeMaker.groovy`)
- Extra attrs: `BeanContainerEmpty`, `WaterTankEmpty`, `DripTrayFull`.
- Methods: start/stop, initialize/installed/updated → `intializeStatus`, SSE connect/disconnect, program/options update.
- Test angle: event mapping keys `ConsumerProducts.CoffeeMaker.Event.*` → attributes.

#### CookProcessor (`drivers/CookProcessor.groovy`)
- Similar to CoffeeMaker with appliance-specific options; uses base pattern.
- Test angle: ensure available programs/options handling and OperationState/DoorState.

#### Dishwasher (`drivers/Dishwasher.groovy`)
- Capabilities: Sensor, Switch, ContactSensor, Initialize.
- Extra attrs: ProgramProgress, RemainingProgramTime, StartInRelative, option toggles (IntensivZone, BrillianceDry, VarioSpeedPlus, SilenceOnDemand, HalfLoad, ExtraDry, HygienePlus), consumables (RinseAidNearlyEmpty, SaltNearlyEmpty), Node-RED extras remainingTime/remainingTimeDisplay.
- Methods: start/stop; initialize/installed → `intializeStatus` + SSE; updated → program/options refresh; switch on/off → parent power setter; program/option update helpers; SSE reconnect/backoff.
- Test angle: ensure `RemainingProgramTime` converts seconds to HH:MM; option bools update settings; contact mirrors DoorState lower-cased.

#### Dryer (`drivers/Dryer.groovy`)
- Similar to Washer; attrs for dryer-specific options/events; base pattern.
- Test angle: map events and options; power on/off fallback.

#### FridgeFreezer (`drivers/FridgeFreezer.groovy`)
- Attrs: FreshMode, VacationMode, SabbathMode, Door alarms, Temp alarms; lighting/ambient support.
- Test angle: settings setters in parent: ambient light enable/brightness; event mapping in parent for fridge keys.

#### Hob (`drivers/Hob.groovy`)
- Likely limited commands; uses base pattern.
- Test angle: verify any hob-specific options exposed.

#### Hood (`drivers/Hood.groovy`)
- Capabilities: adds `FanControl`; attrs include `speed`, `VentingLevel`, `IntensiveLevel`, `Lighting`, `LightingBrightness`.
- Commands: `setLighting`, `setLightingBrightness`, `setIntensiveLevel`; speed mapping to venting/intensive options via parent setters.
- Test angle: command → parent `setLighting`/`setLightingBrightness`/`setIntensiveLevel`; event mapping for grease filter saturation events.

#### Oven (`drivers/Oven.groovy`)
- Attrs: CurrentCavityTemperature, SabbathMode; lighting options; door/contact.
- Test angle: `setCurrentProgram/Options` flows; SSE for temperature updates.

#### Washer (`drivers/Washer.groovy`) & WasherDryer (`drivers/WasherDryer.groovy`)
- Attrs: IDos1/2FillLevelPoor, ProgramProgress/Remaining, etc.; ContactSensor; same program/options flow.
- Test angle: event keys `LaundryCare.Washer.Event.*` mapping; switch on/off delegation.

#### WineCooler (`drivers/WineCooler.groovy`)
- Attrs: likely FreshMode/VacationMode/SabbathMode; base pattern.
- Test angle: temperature/zone attributes if present; event mapping.

### Example method table (applies similarly to all child drivers)
| Method | Purpose | Notes / Tests |
| --- | --- | --- |
| `installed()` | call `intializeStatus` | Assert init called and SSE connect attempted. |
| `updated()` | refresh program/options | Ensure program selection propagates via parent. |
| `initialize()` | init + SSE connect | Verify connectEventStream invoked. |
| `uninstalled()` | disconnect SSE | Assert disconnect called. |
| `startProgram()` | call parent `startProgram` with selected program key | Mock parent; assert key derived from `state.foundAvailablePrograms`. |
| `stopProgram()` | parent `stopProgram` | Straight delegation. |
| `setCurrentProgram()` | parent `setSelectedProgram` based on preference | Verify option key mapping. |
| `setCurrentProgramOptions()` | loop options, call parent `setSelectedProgramOption` | Assert bool prefs → option keys. |
| `updateAvailableProgramList()` | fetch list from parent, send as JSON string | Assert event `AvailableProgramsList` payload. |
| `updateAvailableOptionsList()` | fetch options for selected program | Assert state + event update. |
| `on/off()` | delegate power state to parent (`setPowerState` or `setPowertate`) | Verify fallback when typo method exists. |
| `intializeStatus()` | refresh programs/options, call parent `intializeStatus`, reconnect SSE | Assert retry/backoff state changes. |
| `connectEventStream/reconnectEventStream/disconnectEventStream` | manage SSE; backoff via `state.connectionRetryTime` | Simulate EventStreamStatus transitions. |
| `setEventStreamStatusToConnected/Disconnected` | update attr, manage retry schedule | Assert unschedule/schedule behavior. |

## Parent/Child Interaction Flows (text sequences)
- **Install + OAuth + Child Creation**: User opens app → `pageIntro` collects clientId/secret/region → `pageAuthentication` triggers OAuth href → Home Connect redirects to `/oauth/callback` → `acquireOAuthToken` stores tokens → `pageDevices` lists appliances via `HomeConnectAPI.getHomeAppliances` → user selects devices → `installed/updated` → `synchronizeDevices` adds children per type mapping.
- **Child Initialize → Status Fetch → SSE Connect**: child `initialize/installed` → `intializeStatus` (child) → `updateAvailableProgramList/OptionsList` → parent `intializeStatus` → parent GET status/settings/active program → parent `sendEventToDevice` emits attributes → child `connectEventStream` calls parent API to open SSE.
- **Start Program Command**: user calls `startProgram` on driver → driver finds program key in `state.foundAvailablePrograms` → parent `startProgram` → `HomeConnectAPI.setActiveProgram` PUT to `/programs/active`.
- **Event Stream Message**: SSE line arrives at driver interfaces → driver forwards raw text to parent `processMessage` → parent parses (`data`/`event`/`error`) → `processData` routes STATUS/NOTIFY/EVENT → `sendEventToDevice` maps Home Connect keys to device events and updates settings/preferences.

## Spock Spec Skeletons (hubitat_ci)
> Adapt per driver; leverage `HubitatAppSandbox` / `HubitatDeviceSandbox`. Stub HTTP and parent methods as needed.

### App: Pages & OAuth URL
```groovy
class HomeConnectAppPagesSpec extends Specification {
  def "intro/auth/devices pages render and include OAuth state"() {
    given:
    def appFile = new File('SubmodulesWithScripts/hubitat-homeconnect/apps/HomeConnect.groovy')
    def sandbox = new HubitatAppSandbox(appFile)

    when:
    def script = sandbox.run(validationFlags: [Flags.DontValidatePreferences], globals: [:])
    def intro = script.pageIntro()
    script.atomicState.accessToken = 'tok'
    def auth = script.pageAuthentication()

    then:
    intro instanceof groovy.lang.Closure // or map per sandbox behavior
    auth.toString().contains('oauth')
  }
}
```

### App: Child sync
```groovy
class HomeConnectChildSyncSpec extends Specification {
  def "creates and removes children based on selected devices"() {
    given:
    def appFile = new File('SubmodulesWithScripts/hubitat-homeconnect/apps/HomeConnect.groovy')
    def sandbox = new HubitatAppSandbox(appFile)
    def mockDevices = [[haId:'X1', name:'Dish', type:'Dishwasher']]

    when:
    def app = sandbox.run(globals:[HomeConnectAPI:[getHomeAppliances:{ c-> c(mockDevices) }]],
                          validationFlags:[Flags.DontValidatePreferences],
                          withLifecycle:false,
                          childDeviceResolver:{ ns,type-> new File('SubmodulesWithScripts/hubitat-homeconnect/drivers/Dishwasher.groovy') })
    app.state.foundDevices = mockDevices
    app.settings.devices = ['X1']
    app.synchronizeDevices()

    then:
    app.getChildDevices()*.deviceNetworkId == ['X1']
  }
}
```

### Driver: Command delegation
```groovy
class DishwasherCommandsSpec extends Specification {
  def "startProgram delegates to parent with selected key"() {
    given:
    def driverFile = new File('SubmodulesWithScripts/hubitat-homeconnect/drivers/Dishwasher.groovy')
    def sandbox = new HubitatDeviceSandbox(driverFile)
    def parent = Mock(Object) // minimal parent mock
    def driver = sandbox.run(parent: parent, validationFlags:[Flags.DontValidatePreferences])
    driver.state.foundAvailablePrograms = [[name:'Eco', key:'Dishcare.Dishwasher.Program.Eco']]
    driver.settings.selectedProgram = 'Eco'

    when:
    driver.startProgram()

    then:
    1 * parent.startProgram(driver.device, 'Dishcare.Dishwasher.Program.Eco')
  }
}
```

### Driver: SSE parse & event mapping via parent
```groovy
class DishwasherEventMappingSpec extends Specification {
  def "handles RemainingProgramTime and DoorState"() {
    given:
    def driverFile = new File('SubmodulesWithScripts/hubitat-homeconnect/drivers/Dishwasher.groovy')
    def sandbox = new HubitatDeviceSandbox(driverFile)
    def parent = new Expando()
    parent.sendEventToDevice = { dev, data -> dev.sendEvent(name:'DoorState', value:'Open'); dev.sendEvent(name:'RemainingProgramTime', value:'00:30') }
    def driver = sandbox.run(parent: parent, validationFlags:[Flags.DontValidatePreferences])

    when:
    driver.sendEvent(name:'DoorState', value:'Open') // emulate parent call

    then:
    driver.currentValue('DoorState') == 'Open'
  }
}
```

## Edge Cases & Testing Notes
- Event stream empty/keep-alive lines ignored; ensure tests cover comment/blank lines.
- Active program fetch may throw when none; parent catches; test `checkActiveProgram=false` path.
- Power setter typo: children fall back to `setPowertate`; include a parent mock with that method for backward compatibility.
- Exponential backoff in `setEventStreamStatusToDisconnected` up to 900s; test retry state growth.
- Attribute JSON strings: program/option lists are serialized with `JsonBuilder` to avoid Groovy block ambiguity; assert string form, not array object.

## Future Extension Points
- Add new appliance types by extending `synchronizeDevices` switch and providing driver mapping plus attribute mappings in `sendEventToDevice`.
- Centralize event key → attribute mapping into reusable helper to reduce duplication and ease testing.
- Add unit specs per driver for program/option list population and SSE reconnection logic.
- Consider abstracting common driver behaviors (init, SSE, program/options) into shared script fragment for consistency.

