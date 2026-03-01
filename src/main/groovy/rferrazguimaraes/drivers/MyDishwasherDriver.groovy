package rferrazguimaraes.drivers
//file:noinspection GrPackage
// package rferrazguimaraes
/**
 *  Home Connect Dishwasher (Child Device of Home Connection Integration)
 *
 *  Author: Rangner Ferraz Guimaraes (rferrazguimaraes)
 *  Contributors: @you-know-who for Hubitat update compatibility & Node-RED remaining-time support
 *
 *  Version history
 *  1.0 - Initial commit
 *  1.1 - Added attributes ProgramProgress and RemainingProgramTime
 *  1.2 - Better handling of STOP events from event stream
 *  1.3 - Updating program when pressing 'Initialize' button
 *  1.4 - Added events for RinseAidNearlyEmpty and SaltNearlyEmpty
 *  1.5 - Added event for StartInRelative
 *  1.6 - Local STATUS sniffing, contact mirroring, bool preference read, JSON .toString()
 *  1.7 - Hubitat update compatibility; also parse Option.* ProgramProgress/RemainingProgramTime and expose remainingTime/remainingTimeDisplay
 *  1.7.2 - Fix for Norwegian values for DoorState and PowerState
 *  1.7.3 - Removes a syntax error, Ensures contact values are open/closed (Hubitat consumers work), Uses canonical English for logic checks while still accepting Norwegian input.
 *  1.7.4 - Fixes unicode un-escape for AvailableProgramsList and AvailableOptionsList
 */


import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
// import groovy.transform.CompileStatic // - No longer needed, all annotations removed
import groovy.transform.Field

@Field static Map Utils = Utils_create()
@Field List<String> LOG_LEVELS = ["error", "warn", "info", "debug", "trace"]
@Field String DEFAULT_LOG_LEVEL = LOG_LEVELS[1]
@Field static final Integer eventStreamDisconnectGracePeriod = 30
@Field static final String driverVersion = "1.7.5"

// @CompileStatic -- Removed: causes NPE in hubitat_ci's static compilation visitor

/*
command "deviceLog", [[name: "level", type: "enum", options: LOG_LEVELS, description: "Level of the message"],
                              [name: "message", type: "STRING", description: "Message"]]
 */
metadata {
    definition(name: "Home Connect Dishwasher", namespace: "rferrazguimaraes", author: "Rangner Ferraz Guimaraes") {
        capability "Sensor"
        capability "Switch"
        capability "ContactSensor"
        capability "Initialize"

        command "deviceLog", [[name: "Level*", type:"STRING", description: "Level of the message"],
                              [name: "Message*", type:"STRING", description: "Message"]]

        command "startProgram"
        command "stopProgram"

        attribute "AvailableProgramsList", "json_object"
        attribute "AvailableOptionsList", "json_object"

        attribute "RemoteControlActive", "enum", ["true", "false"]
        attribute "RemoteControlStartAllowed", "enum", ["true", "false"]

        attribute "OperationState", "enum", [
                "Inactive","Ready","DelayedStart","Run","Pause","ActionRequired","Finished","Error","Aborting"
        ]

        attribute "DoorState", "enum", ["Open","Closed","Locked","Lukket","Åpen","Låst"]

        attribute "ActiveProgram", "string"
        attribute "SelectedProgram", "string"

        attribute "PowerState", "enum", ["Off","On","Standby","Av","På","Hvilemodus"]

        attribute "EventPresentState", "enum", ["Event active","Off","Confirmed","Av","Bekreftet"]

        attribute "ProgramProgress", "number"
        attribute "RemainingProgramTime", "string"     // HH:MM display
        attribute "StartInRelative", "string"

        // Common dishwasher options (string attributes so we can show true/false words)
        attribute "IntensivZone", "string"
        attribute "BrillianceDry", "string"
        attribute "VarioSpeedPlus", "string"
        attribute "SilenceOnDemand", "string"
        attribute "HalfLoad", "string"
        attribute "ExtraDry", "string"
        attribute "HygienePlus", "string"
        attribute "RinseAidNearlyEmpty", "string"
        attribute "SaltNearlyEmpty", "string"

        // Extra attributes for Node-RED SVG flow
        attribute "remainingTime", "number"            // seconds
        attribute "remainingTimeDisplay", "string"     // HH:MM

        attribute "EventStreamStatus", "enum", ["connected", "disconnected"]
        attribute "DriverVersion", "string"
    }

    preferences {
        section {
            List<String> availableProgramsList = getAvailableProgramsList()
            if (availableProgramsList.size() != 0) {
                input name:"selectedProgram", type:"enum", title: "Select Program", options: availableProgramsList
            }

            List<String> availableOptionList = getAvailableOptionsList()
            for (int i = 0; i < availableOptionList.size(); ++i) {
                String titleName = availableOptionList[i]
                String optionName = titleName.replaceAll("\\s","")
                input name: optionName, type:"bool", title: "${titleName}", defaultValue: false
            }

            input name: "logLevel", title: "Log Level", type: "enum", options: LOG_LEVELS, defaultValue: DEFAULT_LOG_LEVEL, required: true
        }
    }
}

/* ---------- Commands ---------- */

void startProgram() {
    if (selectedProgram != null) {
        def programToSelect = state.foundAvailablePrograms.find { it.name == selectedProgram }
        if (programToSelect) {
            parent.startProgram(device, programToSelect.key)
        }
    }
}

void stopProgram() {
    parent.stopProgram(device)
}

void initialize() {
    Utils.toLogger("debug", "initialize()")
    intializeStatus()
}

void installed() {
    Utils.toLogger("debug", "installed()")
    intializeStatus()
}

void updated() {
    Utils.toLogger("debug", "updated()")
    setCurrentProgram()
    updateAvailableOptionsList()
    setCurrentProgramOptions()
}

void uninstalled() {
    disconnectEventStream()
}

/* ---------- Helpers: program & options ---------- */

void setCurrentProgram() {
    if (selectedProgram != null) {
        def programToSelect = state.foundAvailablePrograms.find { it.name == selectedProgram }
        if (programToSelect) {
            parent.setSelectedProgram(device, programToSelect.key)
        }
    }
}

void setCurrentProgramOptions() {
    List<String> availableOptionList = getAvailableOptionsList()
    if (!availableOptionList) return

    for (int i = 0; i < availableOptionList.size(); ++i) {
        String optionTitle = availableOptionList[i]
        String optionName  = optionTitle.replaceAll("\\s", "")

        Boolean optionValue = (settings?."${optionName}" ?: false)

        def programOption = state?.foundAvailableProgramOptions?.find { it.name == optionTitle }
        if (programOption) {
            parent.setSelectedProgramOption(device, programOption.key, optionValue)
        } else {
            Utils.toLogger("debug", "Option not found for '${optionTitle}'")
        }
    }
}

/** Unescape unicode sequences (e.g. \u00B0) in a string
 *
 * @param s Input string
 * @return String with unicode sequences unescaped
 */
// @CompileStatic - Removed: causes NPE(?) in hubitat_ci's static compilation visitor
private static String unescapeUnicode(String s) {
    if (!s) return s
    return s.replaceAll(/\\u([0-9A-Fa-f]{4})/) { String full, String hex ->
        String.valueOf((char) Integer.parseInt(hex, 16))
    }
}

void updateAvailableProgramList() {
    state.foundAvailablePrograms = parent.getAvailableProgramList(device)
    def programList = state.foundAvailablePrograms.collect { it.name }
    // build JSON then unescape unicode sequences so the Hubitat UI shows actual characters (e.g. °)
    def json = new JsonBuilder(programList).toString()
    json = unescapeUnicode(json)
    sendEvent(name: "AvailableProgramsList", value: json, displayed: false)
}

void updateAvailableOptionsList() {
    if (selectedProgram != null) {
        def programToSelect = state.foundAvailablePrograms.find { it.name == selectedProgram }
        if (programToSelect) {
            state.foundAvailableProgramOptions = parent.getAvailableProgramOptionsList(device, programToSelect.key)
            def programOptionsList = state.foundAvailableProgramOptions.collect { it.name }
            def json = new JsonBuilder(programOptionsList).toString()
            json = unescapeUnicode(json)
            sendEvent(name: "AvailableOptionsList", value: json, displayed: false)
            Utils.toLogger("debug", "updateAvailableOptionList programOptionsList: ${programOptionsList}")
            return
        }
    }

    state.foundAvailableProgramOptions = []
    def emptyJson = new JsonBuilder([]).toString()
    emptyJson = unescapeUnicode(emptyJson)
    sendEvent(name: "AvailableOptionsList", value: emptyJson, displayed: false)
}

/* ---------- Switch handling ---------- */

def on()  { safeSetPowerState(true)  }
def off() { safeSetPowerState(false) }

private void safeSetPowerState(Boolean val) {
    if (parent.respondsTo('setPowerState')) {
        parent.setPowerState(device, val)
    } else if (parent.respondsTo('setPowertate')) { // legacy typo
        parent.setPowertate(device, val)
    } else {
        Utils.toLogger("error", "Parent has no setPowerState/setPowertate method")
    }
}

/* ---------- Init & Event Stream ---------- */

void intializeStatus() {
    Utils.toLogger("debug", "Initializing the status of the device")

    updateAvailableProgramList()
    updateAvailableOptionsList()
    parent.intializeStatus(device)

    try {
        disconnectEventStream()
        connectEventStream()
    } catch (Exception e) {
        Utils.toLogger("error", "intializeStatus() failed: ${e.message}")
        setEventStreamStatusToDisconnected()
    }
}

void connectEventStream() {
    Utils.toLogger("debug", "connectEventStream()")
    parent.getHomeConnectAPI().connectDeviceEvents(device.deviceNetworkId, interfaces)
}

void reconnectEventStream(Boolean notIfAlreadyConnected = true) {
    Utils.toLogger("debug", "reconnectEventStream(notIfAlreadyConnected=$notIfAlreadyConnected)")
    if (device.currentValue("EventStreamStatus") == "connected" && notIfAlreadyConnected) {
        Utils.toLogger("debug", "already connected; skipping reconnection")
    } else {
        connectEventStream()
    }
}

void disconnectEventStream() {
    Utils.toLogger("debug", "disconnectEventStream()")
    parent.getHomeConnectAPI().disconnectDeviceEvents(device.deviceNetworkId, interfaces)
}

void setEventStreamStatusToConnected() {
    Utils.toLogger("debug", "setEventStreamStatusToConnected()")
    unschedule("setEventStreamStatusToDisconnected")
    if (device.currentValue("EventStreamStatus") == "disconnected") {
        sendEvent(name: "EventStreamStatus", value: "connected", displayed: true, isStateChange: true)
    }
    state.connectionRetryTime = 15
}

void setEventStreamStatusToDisconnected() {
    Utils.toLogger("debug", "setEventStreamStatusToDisconnected()")
    sendEvent(name: "EventStreamStatus", value: "disconnected", displayed: true, isStateChange: true)
    if (state.connectionRetryTime) {
        state.connectionRetryTime *= 2
        if (state.connectionRetryTime > 900) state.connectionRetryTime = 900
    } else {
        state.connectionRetryTime = 15
    }
    Utils.toLogger("debug", "reconnecting EventStream in ${state.connectionRetryTime} seconds")
    runIn(state.connectionRetryTime, "reconnectEventStream")
}

void eventStreamStatus(String text) {
    Utils.toLogger("debug", "Received eventstream status message: ${text}")
    def (String type, String message) = text.split(':', 2)
    switch (type) {
        case 'START':
            atomicState.oStartTokenExpires = now() + 60_000 // 60 seconds
            setEventStreamStatusToConnected()
            break
        case 'STOP':
            if (now() >= atomicState.oStartTokenExpires) {
                Utils.toLogger("debug", "eventStreamDisconnectGracePeriod: ${eventStreamDisconnectGracePeriod}")
                runIn(eventStreamDisconnectGracePeriod, "setEventStreamStatusToDisconnected")
            } else {
                Utils.toLogger("debug", "stream started recently so ignore STOP event")
            }
            break
        default:
            Utils.toLogger("error", "Received unhandled Event Stream status message: ${text}")
            atomicState.oStartTokenExpires = now()
            runIn(eventStreamDisconnectGracePeriod, "setEventStreamStatusToDisconnected")
            break
    }
}

/* ---------- Parse incoming SSE lines ---------- */

// @CompileStatic - Removed: causes NPE in hubitat_ci's static compilation visitor
private static String formatHHMMFromSeconds(Integer secs) {
    if (secs == null || secs < 0) secs = 0
    Integer h = (int)(secs / 3600)
    Integer m = (int)((secs % 3600) / 60)
    return "${String.format('%02d', h)}:${String.format('%02d', m)}"
}

void parse(String text) {
    Utils.toLogger("debug", "Received eventstream message: ${text}")

    // Lightweight JSON sniffing so we can immediately reflect common attributes
    try {
        if (text?.startsWith('data:')) {
            String payload = text.substring(5).trim()
            if (payload && payload.startsWith('{')) {
                def obj = new JsonSlurper().parseText(payload)
                def items = (obj?.items instanceof List) ? obj.items : []

                items.each { item ->
                    String key = item?.key as String
                    def valObj = item?.value
                    String valStr = (valObj != null) ? valObj.toString() : null

                    switch (key) {
                    /* --- Door / Contact --- */
                        case 'BSH.Common.Status.DoorState':
                            String rawDoor = valStr?.tokenize('.')?.last()
                            String door = normalizeDoor(rawDoor)
                            if (door in ['Open','Closed','Locked']) {
                                sendEvent(name: "DoorState", value: door, isStateChange: true)
                                // Hubitat contact capability expects 'open' or 'closed' (lowercase)
                                if (door == 'Open') {
                                    sendEvent(name: "contact", value: "open", isStateChange: true)
                                } else if (door == 'Closed') {
                                    sendEvent(name: "contact", value: "closed", isStateChange: true)
                                }
                            }
                            break

                            /* --- Operation state --- */
                        case 'BSH.Common.Status.OperationState':
                            String rawOp = valStr?.tokenize('.')?.last()
                            String op = normalizeOperation(rawOp)
                            if (op) sendEvent(name: "OperationState", value: op, isStateChange: true)
                            break


                            /* --- Power (often reported as a Setting) --- */
                        case 'BSH.Common.Setting.PowerState':
                        case 'BSH.Common.Status.PowerState':
                            String rawPwr = valStr?.tokenize('.')?.last()
                            String pwr = normalizePower(rawPwr)
                            if (pwr) sendEvent(name: "PowerState", value: pwr, isStateChange: true)
                            break

                            /* --- Progress (may come as Status.* or Option.*) --- */
                        case 'BSH.Common.Status.ProgramProgress':
                        case 'BSH.Common.Option.ProgramProgress':
                            Integer pct = (valObj instanceof Number) ? (valObj as Integer)
                                    : (valStr?.isInteger() ? valStr.toInteger() : null)
                            if (pct != null) sendEvent(name: "ProgramProgress", value: pct, isStateChange: true)
                            break

                            /* --- Remaining time (may come as Status.* or Option.*) --- */
                        case 'BSH.Common.Status.RemainingProgramTime':
                        case 'BSH.Common.Option.RemainingProgramTime':
                            Integer secs = (valObj instanceof Number) ? (valObj as Integer)
                                    : (valStr?.isInteger() ? valStr.toInteger() : null)
                            if (secs != null) {
                                String op2raw = (device.currentValue("OperationState") ?: "")
                                String op2 = normalizeOperation(op2raw)
                                Integer prog2 = ((device.currentValue("ProgramProgress") ?: 0) as Integer)
                                String pwr2raw = (device.currentValue("PowerState") ?: "")
                                String pwr2 = normalizePower(pwr2raw)
                                boolean acceptZero = ['Finished','Inactive','Ready','Error','Aborting'].contains(op2) || prog2 >= 100 || pwr2 == 'Off'
                                if (secs == 0 && !acceptZero) {
                                    Utils.toLogger("debug", "Ignoring transient RemainingProgramTime=0 during Run (op=$op2, prog=$prog2, pwr=$pwr2)")
                                    break
                                }
                                String hhmm = formatHHMMFromSeconds(secs)
                                sendEvent(name: "RemainingProgramTime", value: hhmm, isStateChange: true)
                                sendEvent(name: "remainingTime", value: secs, isStateChange: true)
                                sendEvent(name: "remainingTimeDisplay", value: hhmm, isStateChange: false)
                            }
                            break
                    } // switch
                } // each
            }
        }
    } catch (e) {
        Utils.toLogger("error", "STATUS/OPTION payload parse error: ${e}")
    }

    // Always let the parent handle the full message
    parent.processMessage(device, text)
    sendEvent(name: "DriverVersion", value: driverVersion)
}

/* ---------- Misc ---------- */

def deviceLog(level, msg) {
    Utils.toLogger(level, msg)
}

/* ---------- Utilities ---------- */

private static Map Utils_create() {
    def instance = [:]
    instance.toLogger = { level, msg ->
        if (level && msg) {
            Integer levelIdx = LOG_LEVELS.indexOf(level)
            Integer setLevelIdx = LOG_LEVELS.indexOf(logLevel)
            if (setLevelIdx < 0) setLevelIdx = LOG_LEVELS.indexOf(DEFAULT_LOG_LEVEL)
            if (levelIdx <= setLevelIdx) {
                log."${level}" "${device.displayName} ${msg}"
            }
        }
    }
    return instance
}

/* ---------- Lists helpers ---------- */

List<String> getAvailableProgramsList() {
    String json = device?.currentValue("AvailableProgramsList")
    if (json != null) return parseJson(json)
    return []
}

List<String> getAvailableOptionsList() {
    String json = device?.currentValue("AvailableOptionsList")
    if (json != null) return parseJson(json)
    return []
}

/**
 * Normalize various DoorState values including some "other language" synonyms
 * @param raw Raw DoorState value
 * @return Normalized DoorState value
 */
// @CompileStatic - Removed: causes NPE in hubitat_ci's static compilation visitor
private static String normalizeDoor(String raw) {
    if (!raw) return raw
    def map = [
            // Norwegian synonyms
            'Åpen': 'Open',
            'Lukket': 'Closed',
            'Låst'  : 'Locked',
            // Defaults (English)
            'Open'   : 'Open',
            'Closed' : 'Closed',
            'Locked' : 'Locked'
    ]
    return map.get(raw, raw)
}

/**
 * Normalize various PowerState values including some "other language" synonyms
 *
 * @param raw Raw PowerState value
 * @return Normalized PowerState value
 */
// @CompileStatic - Removed: causes NPE in hubitat_ci's static compilation visitor
private static String normalizePower(String raw) {
    if (!raw) return raw
    def map = [
            // Norwegian synonyms
            'På'  : 'On',
            'Av'  : 'Off',
            'Hvilemodus': 'Standby',
            // Defaults (English)
            'On'   : 'On',
            'Off'  : 'Off',
            'Standby': 'Standby'
    ]
    return map.get(raw, raw)
}

/**
 * Normalize various OperationState values including some "other language" synonyms
 *
 * @param raw Raw OperationState value
 * @return Normalized OperationState value
 */
// @CompileStatic - Removed: causes NPE in hubitat_ci's static compilation visitor
private static String normalizeOperation(String raw) {
    if (!raw) return raw
    def map = [
            // Norwegian synonyms
            'Klar'        : 'Ready',
            'Kjør' 	      : 'Run',
            'Ferdig'      : 'Finished',
            // Defaults (English)
            'Inactive'    : 'Inactive',    // Inactive
            'Ready'       : 'Ready',
            'DelayedStart': 'DelayedStart',
            'Run'         : 'Run',
            'Pause'       : 'Pause',
            'ActionRequired' : 'ActionRequired',
            'Finished'    : 'Finished',
            'Error'       : 'Error',
            'Aborting'    : 'Aborting'
    ]
    return map.get(raw, raw)
}
