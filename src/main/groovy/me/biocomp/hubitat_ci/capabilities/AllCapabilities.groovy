package me.biocomp.hubitat_ci.capabilities


/**
 * Base capability traits, all capabilities derive from it.
 */
interface Capability
{
}

interface AccelerationSensor extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        enumAttribute("acceleration", ["active", "inactive"]) // Called 'ActivityState' in SmartThing
    }
}

// Deprecated in SmartThings
interface Actuator extends Capability
{
}

// Only in SmartThings: interface AirConditionerMode {}
// Only in SmartThings: interface AirQualitySensor {}

interface Alarm extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        enumAttribute("alarm", ["strobe", "both", "off", "siren"])
    }


    abstract both()


    abstract off()


    abstract siren()


    abstract strobe()
}

// Only in SmartThings: interface AudioMute{}

interface AudioNotification extends Capability
{
    /**
     * @param text required (STRING) - Text to play
     * @param volumeLevel optional (NUMBER) - Volume level (0 to 100)
     */
    abstract void playText(String text, Double volumeLevel)

    /**
    * @param text required (STRING) - Text to play
    * @param volumeLevel optional (NUMBER) - Volume level (0 to 100)
     */
    abstract playTextAndRestore(String text, Double volumeLevel)

    /**
    * @param text required (STRING) - Text to play
    * @param volumeLevel optional (NUMBER) - Volume level (0 to 100)
    * */
    abstract playTextAndResume(String text, Double volumeLevel)

    /**
    * @param trackUri required (STRING) - URI/URL of track to play
    * @param volumeLevel optional (NUMBER) - Volume level (0 to 100)
    * */
    abstract playTrack(String trackUri, Double volumeLevel)

    /**
    * @param trackUri required (STRING) - URI/URL of track to play
    * @param volumeLevel optional (NUMBER) - Volume level (0 to 100)
    * */
    abstract playTrackAndRestore(String trackUri, Double volumeLevel)

    /**
    * @param trackUri required (STRING) - URI/URL of track to play
    * @param volumeLevel optional (NUMBER) - Volume level (0 to 100)
    * */
    abstract playTrackAndResume(String trackUri, Double volumeLevel)
}

// SmartThings only: Audio Track Data

interface AudioVolume extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        enumAttribute("mute", ["unmuted", "muted"])
        number("volume") // Double
    }


    abstract void mute()

    /**
    * @param volumeLevel required (NUMBER) - Volume level (0 to 100)
    */

    abstract void setVolume(Double volumeLevel)


    abstract void unmute()


    abstract void volumeDown()


    abstract void volumeUp()
}

interface Battery extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        number("battery", min: 0, max: 100) // 0-100% battery charge
    }
}

// Deprecated in SmartThings
interface Beacon extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        enumAttribute("presence", ["present", "not present"])
    }
}

// SmartThings only: Bridge


interface Bulb extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        enumAttribute("switch", ["on", "off"])
    }


    abstract void on()


    abstract void off()
}

// Deprecated in both SmartThings and Hubitat
interface Button extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        number("button")
        enumAttribute("holdableButton", ["true", "false"])
        number("numberOfButtons")
    }
}

interface CarbonDioxideMeasurement extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        number("carbonDioxide") // Double
    }
}

interface CarbonMonoxideDetector extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        enumAttribute("carbonMonoxide", ["detected", "tested", "clear"])
    }
}

interface ChangeLevel extends Capability
{
    /**
     * @param direction required (ENUM) - Direction for level change request
     */
    abstract void startLevelChange(String direction)
    abstract void stopLevelChange()
}

interface Chime extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        jsonObject("soundEffects")
        stringAttribute("soundName")
        enumAttribute("status", ["playing", "stopped"])
    }


    abstract void playSound(Number soundNumber)


    abstract void stop()
}

interface ColorControl extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        stringAttribute("rGB")
        stringAttribute("color")
        stringAttribute("colorName")
        number("hue") // Double
        number("saturation") // Double
    }


    /**
     *
     * @param colormap required (COLOR_MAP) - Color map settings [hue*:(0 to 100), saturation*:(0 to 100), level:(0 to 100)]
     * @return
     */

    abstract void setColor(Map colorMap)

    /**
     * @param hue required (NUMBER) - Color Hue (0 to 100)
     */

    abstract void setHue(Double hue)

    /**
     * @param saturation required (NUMBER) - Color Saturation (0 to 100)
     */

    abstract void setSaturation(Double saturation)
}

interface ColorMode extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        enumAttribute("colorMode", ["CT", "RGB"])
    }
}

interface ColorTemperature extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        stringAttribute("colorName")
        number("colorTemperature") // Double
    }
}

interface Configuration extends Capability
{
    abstract void configure()
}

interface Consumable extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        enumAttribute("consumableStatus", ["missing", "order", "maintenance_required", "good", "replace"])
    }


    abstract void setConsumableStatus(String status)
}

// SmartThings only: Color

interface ContactSensor extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        enumAttribute("contact", ["closed", "open"])
    }
}

// SmartThings only: Demand Response Load Control
// SmartThings only: Dishwasher Mode
// SmartThings only: Dishwasher Operating State

interface DoorControl extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        enumAttribute("door", ["unknown", "closed", "open", "closing", "opening"])
    }


    abstract void open()


    abstract void close()
}

// SmartThings only: Dryer Mode
// SmartThings only: Dryer Operating State
// Dust Sensor

interface DoubleTapableButton extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        number("doubleTapped")
    }
}

interface EnergyMeter extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        number("energy") // Double, // in kW
    }
}

interface EstimatedTimeOfArrival extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        date("eta")
    }
}

// Execute

// SmartThings calls it Fan Speed
interface FanControl extends Capability
{


    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        enumAttribute("speed", ["low", "medium-low", "medium", "medium-high", "high", "on", "off", "auto"])
    }



    abstract void setSpeed(String speed)
}

interface FilterStatus extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        enumAttribute("filterStatus", ["normal", "replace"])
    }
}

interface GarageDoorControl extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        enumAttribute("door", ["unknown", "open", "closing", "closed", "opening"])
    }


    abstract  void close()


    abstract  void open()
}

// Geolocation

interface HealthCheck extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        number("checkInterval")
    }


    abstract void ping()
}

interface HoldableButton extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        number("held")
    }
}

interface IlluminanceMeasurement extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        number("illuminance") // Double
    }
}

interface ImageCapture extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        stringAttribute("image")
    }


    abstract void take()
}

interface Indicator extends Capability
{

    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        enumAttribute("indicatorStatus", ["never", "when on", "when off"])
    }


    abstract void indicatorNever()


    abstract void indicatorWhenOff()


    abstract void indicatorWhenOn()
}

// Infrared Level

interface Initialize extends Capability
{
    abstract static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        enumAttribute("switch", ["on", "off"])
    }
}

interface Light extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        enumAttribute("switch", ["on", "off"])
    }


    abstract void on()


    abstract void off()
}

interface LightEffects extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        stringAttribute("effectName")
        jsonObject("lightEffects")
    }

    /**
     * @param effectNumber required (NUMBER) - Effect number to enable
     */

    abstract void setEffect(Number effectNumber)


    abstract void setNextEffect()


    abstract void setPreviousEffect()
}

interface LocationMode extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        stringAttribute("mode") // DYNAMIC_ENUM with mode
    }
}

// Lock only

interface Lock extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        enumAttribute("lock", ["locked", "unlocked with timeout", "unlocked", "unknown"])
    }


    abstract void lock()


    abstract void unlock()
}

interface LockCodes extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        enumAttribute("codeChanged", ["added", "changed", "deleted", "failed"])
        number("codeLength")
        jsonObject("lockCodes")
        number("maxCodes")
    }

    /**
     * @param codePosition required (NUMBER) - Code position number to delete
     */

    abstract void deleteCode(codePosition)



    abstract void getCodes()

    /**
     * @param codePosition - required (NUMBER) - Code position number
     * @param pinCode - required (STRING) - Numeric PIN code
     * @param name - optional (STRING) - Name for this lock code
     */

    abstract void setCode(Number codePosition, String pinCode, String name)

    /**
     * @param pinCodeLength required (NUMBER) - Maximum pin code length for this lock
     */

    abstract void setCodeLength(Number pinCodeLength)
}

interface MediaController extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        jsonObject("activities")
        stringAttribute("currentActivity")
    }
}

// Media Input Source
// Media Playback Repeat
// Media Playback Shuffle
// Media Playback
// Media Presets
// Media Track Control

interface Momentary extends Capability
{
    abstract void push()
}

interface MotionSensor extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        enumAttribute("motion", ["inactive", "active"])
    }
}

interface MusicPlayer extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        number("level") // Double
        enumAttribute("mute", ["unmuted", "muted"])
        stringAttribute("status")
        jsonObject("trackData")
        stringAttribute("trackDescription")
    }


    abstract void mute()


    abstract void nextTrack()


    abstract void pause()


    abstract void play()

    /**
     * @param text required (STRING) - Text to play
     */

    abstract void playText(String text)


    /**
     * @param trackUri (STRING) - URI/URL of track to play
     */

    abstract void playTrack(String trackUri)



    abstract void previousTrack()

    /**
     *
     * @param trackUri required (STRING) - URI/URL of track to restore
     */

    abstract void restoreTrack(trackUri)

    /**
     *
     * @param trackUri required (STRING) - URI/URL of track to play
     */

    abstract void resumeTrack(trackUri)


    /**
     * @param volumeLevel required (NUMBER) - Volume level (0 to 100)
     * @return
     */

    abstract void setLevel(Double volumeLevel)


    /**
     * @param trackUri required (STRING) - URI/URL of track to set
     */

    abstract void setTrack(trackUri)


    abstract void stop()


    abstract void unmute()
}

/**
 * Allows for displaying notifications on devices that allow notifications to be displayed
 */
interface Notification extends Capability
{
    abstract void deviceNotification(String text)
}

// Odor Sensor

interface Outlet extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        enumAttribute("switch", ["on", "off"])
    }


    abstract void off()


    abstract void on()
}

// Oven Mode
// Oven Operating State
// Oven Setpoint

// Deprecated in SmartThings
interface Polling extends Capability
{
    abstract void poll()
}

// Power Consumption Report

interface PowerMeter extends Capability, VoltageMeasurement
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        number("power") // Double // In Watt
    }
}

/**
 * Gives the ability to determine the current power source of the device
 */
interface PowerSource extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        enumAttribute("powerSource", ["mains","battery","dc","unknown"])
    }
}

interface PresenceSensor extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        enumAttribute("presence", ["present", "not present"])
    }

}

// Rapid Cooling

interface PressureMeasurement extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        number("pressure") // Double
    }
}

interface PushableButton extends Capability {
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        number("pushed")
        number("numberOfButtons")
    }
}

interface Refresh extends Capability
{
    abstract void refresh()
}

interface RelativeHumidityMeasurement extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        number("humidity") // Double
    }
}

interface RelaySwitch extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        enumAttribute("switch", ["on", "off"])
    }


    abstract void on()


    abstract void off()
}

// Robot Cleaner Cleaning Mode
// Robot Cleaner Movement
// Robot Cleaner Turbo Mode

interface ReleasableButton extends Capability {
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        number("released")
        number("numberOfButtons")
    }
}

@CustomDeviceSelector(deviceSelector = 'samsungTV')
@CustomPrettyName(prettyName = 'Samsung TV')
interface SamsungTV extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList {
        jsonObject("messageButton")
        enumAttribute("mute", ["muted", "unknown", "unmuted"])
        enumAttribute("pictureMode", ["unknown", "standard", "movie", "dynamic"])
        enumAttribute("soundMode", ["speech", "movie", "unknown", "standard", "music"])
        enumAttribute("switch", ["on", "off"])
        number("volume") // Double
    }


    abstract void mute()


    abstract void off()


    abstract void on()


    abstract void setPictureMode(String mode)


    abstract void setSoundMode(String mode)


    abstract void setVolume(Double volume)


    abstract void showMessage(String a, String b, String c, String d)


    abstract void unmute()


    abstract void volumeDown()


    abstract void volumeUp()
}

interface SecurityKeypad extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        enumAttribute("codeChanged", ["added", "changed", "deleted", "failed"])
        number("codeLength")
        jsonObject("lockCodes")
        number("maxCodes")
        enumAttribute("securityKeypad", ["disarmed","armed home","armed away","unknown"])
    }


    abstract void armAway()


    abstract void armHome()

    /**
     *
     * @param codePosition required (NUMBER) - Code position number to delete
     * @return
     */

    abstract void deleteCode(Number codePosition)


    abstract void disarm()


    abstract void getCodes()


    /**
     * @param codePosition required (NUMBER) - Code position number
     * @param pinCode required (STRING) - Numeric PIN code
     * @param name optional (STRING) - Name for this lock code
     */

    abstract void setCode(codePosition, pinCode, name)

    /**
     * @param pinCodeLength required (NUMBER) - Maximum pin code length for this keypad
     */

    abstract void setCodeLength(pinCodeLength)



    abstract void setEntryDelay(Number entranceDelayInSeconds)

    /**
     * @param exitDelay required (NUMBER) - Exit delay in seconds
     */

    abstract void setExitDelay(exitDelay)
}

interface Sensor extends Capability
{}

interface ShockSensor extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        enumAttribute("shock", ["clear", "detected"])
    }
}

interface SignalStrength extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        number("lqi", min: 0, max: 255) // Double
        number("rssi", min: -200, max: 0) // Double
    }
}

interface SleepSensor extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        enumAttribute("sleeping", ["not sleeping", "sleeping"])
    }
}

interface SmokeDetector extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        enumAttribute("smoke", ["clear", "tested", "detected"])
    }
}

interface SoundPressureLevel extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        number("soundPressureLevel") // Double
    }
}

interface SoundSensor extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        enumAttribute("sound", ["not detected", "detected"])
    }
}

interface SpeechRecognition extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        stringAttribute("phraseSpoken")
    }
}

interface SpeechSynthesis extends Capability
{
    abstract void speak(String text)
}

interface StepSensor extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        number("goal") // Double
        number("steps") // Double
    }
}

interface Switch extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        enumAttribute("switch", ["on", "off"])
    }


    abstract void on()


    abstract void off()
}

interface SwitchLevel extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        number("level", min: 0, max: 100) // Double
    }

    /**
    * @param level required (NUMBER) - Level to set (0 to 100)
    * @param duration optional (NUMBER) - Transition duration in seconds
    */

    abstract void setLevel(Double level, Number duration)
}

@CustomDeviceSelector(deviceSelector = 'tv')
@CustomPrettyName(prettyName = 'TV')
interface TV extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        number("channel")
        stringAttribute("movieMode")
        stringAttribute("picture")
        stringAttribute("power")
        stringAttribute("sound")
        number("volume") // Double
    }


    abstract void channelDown()


    abstract void channelUp()


    abstract void volumeDown()


    abstract void volumeUp()
}

interface TamperAlert extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        enumAttribute("tamper", ["clear", "detected"])
    }
}

interface Telnet extends Capability
{
}

interface TemperatureMeasurement extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
            number("temperature") // Double
    }
}

interface TestCapability extends Capability {}

interface Thermostat extends
        ThermostatCoolingSetpoint,
        ThermostatHeatingSetpoint,
        ThermostatFanMode,
        ThermostatMode,
        ThermostatOperatingState,
        ThermostatSchedule,
        ThermostatSetpoint
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        jsonObject("supportedThermostatFanModes") // Array of modes
        jsonObject("supportedThermostatModes") // Array of modes
        number("temperature") // Double
    }
}

interface ThermostatCoolingSetpoint extends Capability
{
    static def _internalAttributes = CapabilityAttributeInfo.makeList {
        number("coolingSetpoint")
    }

    /**
     *
     * @param temperature required (NUMBER) - Cooling setpoint in degrees
     */

    abstract void setCoolingSetpoint(Number temperature)
}

interface ThermostatFanMode extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        enumAttribute("thermostatFanMode", ["auto", "circulate", "on"])
    }


    abstract void fanAuto()


    abstract void fanCirculate()


    abstract void fanOn()


    abstract void setThermostatFanMode(String fanMode)
}

interface ThermostatHeatingSetpoint extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        number("heatingSetpoint") // temperature in degree
    }

    /**
     *
     * @param temperature required (NUMBER) - Heating setpoint in degrees
     */

    abstract void setHeatingSetpoint(Number temperature)
}

interface ThermostatMode extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        enumAttribute("thermostatMode", ["auto", "off", "heat", "emergency heat", "cool"])
    }


    abstract void auto()


    abstract void cool()


    abstract void emergencyHeat()


    abstract void heat()


    abstract void off()


    abstract void setThermostatMode(String mode)
}

interface ThermostatOperatingState extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        enumAttribute("thermostatOperatingState", ["heating", "pending cool", "pending_heat", "vent economizer", "idle", "cooling", "fan only"])
    }
}

interface ThermostatSchedule extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        jsonObject("schedule")
    }


    abstract void setSchedule(def jsonObject)
}

interface ThermostatSetpoint extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        number("thermostatSetpoint")
    }
}

interface ThreeAxis extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        stringAttribute("threeAxis") // Seems like string in "x,y,z" format
    }
}

interface TimedSession extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        enumAttribute("sessionStatus", ["stopped", "canceled", "running", "paused"])
        number("timeRemaining")
    }


    abstract void cancel()


    abstract void pause()


    abstract void setTimeRemaining(Number time) // Is it NUMBER though?


    abstract void start()


    abstract void stop()
}

interface Tone extends Capability
{
    abstract void beep()
}

interface TouchSensor extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        enumAttribute("touch", ["touched"])
    }
}

// Tv Channel

interface UltravioletIndex extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        number("ultravioletIndex", min: 0, max: 255)
    }
}

interface Valve extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        enumAttribute("valve", ["open", "closed"])
    }


    abstract void open()


    abstract void close()
}

// Video Stream
// Video Clips

interface VideoCamera extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        enumAttribute("camera", ["on", "off", "restarting", "unavailable"])
        enumAttribute("mute", ["unmuted", "muted"])
        jsonObject("settings")
        stringAttribute("statusMessage")
    }


    abstract void flip()


    abstract void mute()


    abstract void off()


    abstract void on()


    abstract void unmute()
}

//interface VideoCapture extends Capability
//{
//    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
//        attribute("clip", Object)
//    }
//
//    @CompileStatic
//    abstract void capture(Date a, Date b, Date c)
//}

interface VoltageMeasurement extends Capability {
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList {
        number("voltage")
    }
}

// Washer Mode
// Washer Operating State

interface WaterSensor extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        enumAttribute("water", ["wet", "dry"])
    }
}

interface WindowShade extends Capability {
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        enumAttribute("windowShade", ["open","closed","partially open","opening","closing","unknown"])
        number("position")
    }

    abstract void open()

    abstract void close()

    abstract void pause()

    abstract void setPosition(Number position)
}

interface ZwMultichannel extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        stringAttribute("epEvent")
        stringAttribute("epInfo")
    }


    abstract void enableEpEvents(String a)


    abstract void epCmd(Number a, String b)
}

@CustomDeviceSelector(deviceSelector = 'pHMeasurement')
@CustomDriverDefinition(driverDefinition = 'pHMeasurement')
@CustomPrettyName(prettyName = 'Ph Measurement')
interface PhMeasurement extends Capability
{
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        number("PH")
    }
}

interface AirQuality extends Capability {
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList {
        number("airQualityIndex")
        number("pm25")
        number("pm10")
    }
}

interface PM2_5 extends Capability {
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        number("pm25")
    }
}

interface PM10 extends Capability {
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        number("pm10")
    }
}

interface MediaPlayback extends Capability {
    static Map<String, CapabilityAttributeInfo> _internalAttributes = CapabilityAttributeInfo.makeList{
        enumAttribute("playbackStatus", ["stopped","playing","paused"])
    }

    abstract void play()

    abstract void pause()

    abstract void stop()
}
