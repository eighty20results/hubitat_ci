package me.biocomp.hubitat_ci.util.integration

import me.biocomp.hubitat_ci.api.common_api.DeviceWrapper
import java.util.Locale

/**
 * The IntegrationDeviceWrapper is a partial implementation of the DeviceWrapper trait.
 * The IntegrationDeviceSpecification will create a Spy of this class, and provide
 * it to its IntegrationDeviceExecutor.  It then handles storing the device's attribute
 * values.
 *
 * Boolean attributes are coerced on write when the caller includes
 * {@code isBooleanAttribute: true} in the event properties map.
 * Boolean, numeric (0/1), and string ("true"/"false", case-insensitive) values
 * are all normalised to a Groovy {@code Boolean}.
 */
abstract class IntegrationDeviceWrapper implements DeviceWrapper {
    private Map attributeValues = [:]
    private Map settingValues = [:]

    /**
     * Bind this wrapper to the script's settings map so that calls to
     * {@code device.updateSetting(...)} are reflected in {@code settings.*}.
     */
    void bindSettingsMap(Map settingsMap) {
        this.settingValues = (settingsMap != null) ? settingsMap : [:]
    }

    /**
     * Coerce a raw value to a {@code Boolean}.
     * Accepts: Boolean, Integer/Long (0 → false, anything else → true),
     * and String "true"/"false" (case-insensitive).
     *
     * @param raw the value to coerce
     * @return the coerced Boolean
     * @throws AssertionError if the value cannot be interpreted as a boolean
     */
    static Boolean coerceToBooleanValue(Object raw) {
        if (raw instanceof Boolean) {
            return raw
        }
        if (raw instanceof Number) {
            return raw.intValue() != 0
        }
        if (raw instanceof String) {
            def lower = raw.toLowerCase(Locale.ROOT)
            assert lower == 'true' || lower == 'false':
                "Cannot coerce value '${raw}' to boolean – expected 'true', 'false', 0, 1, or a Boolean."
            return lower == 'true'
        }
        throw new AssertionError(
            "Cannot coerce value '${raw}' (${raw?.class?.simpleName}) to boolean." as Object)
    }

    @Override
    void sendEvent(Map properties) {
        // The main result of sending an event is that the device's attributes are updated.
        // When the caller marks an event as boolean (isBooleanAttribute: true), the value is
        // coerced to a Groovy Boolean so that code using currentValue() sees a proper boolean.
        def value = properties.value
        if (properties.isBooleanAttribute) {
            value = coerceToBooleanValue(value)
        }
        attributeValues[properties.name] = value
    }

    @Override
    def currentValue(String attributeName, boolean skipCache) {
        return attributeValues[attributeName]
    }

    @Override
    void updateSetting(String name, Boolean value) {
        setSettingValue(name, value)
    }

    @Override
    void updateSetting(String name, Date value) {
        setSettingValue(name, value)
    }

    @Override
    void updateSetting(String name, Double value) {
        setSettingValue(name, value)
    }

    @Override
    void updateSetting(String name, List value) {
        setSettingValue(name, value)
    }

    @Override
    void updateSetting(String name, Long value) {
        setSettingValue(name, value)
    }

    @Override
    void updateSetting(String name, String value) {
        setSettingValue(name, value)
    }

    @Override
    void updateSetting(String name, Map value) {
        setSettingValue(name, unpackTypedSetting(value))
    }

    @Override
    void clearSetting(String settingName) {
        settingValues?.remove(settingName)
    }

    @Override
    void removeSetting(String settingName) {
        settingValues?.remove(settingName)
    }

    @Override
    Object getSetting(String settingName) {
        return settingValues?.get(settingName)
    }

    protected void setSettingValue(String name, Object value) {
        if (settingValues == null) {
            settingValues = [:]
        }
        settingValues.put(name, value)
    }

    protected static Object unpackTypedSetting(Map value) {
        if (value == null || !value.containsKey('value')) {
            return value
        }

        Object rawValue = value.get('value')
        String settingType = value.get('type')?.toString()?.toLowerCase(Locale.ROOT)
        if (settingType in ['bool', 'boolean']) {
            return coerceToBooleanValue(rawValue)
        }

        return rawValue
    }
}
