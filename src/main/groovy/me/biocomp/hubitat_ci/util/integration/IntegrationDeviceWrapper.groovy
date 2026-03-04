package me.biocomp.hubitat_ci.util.integration

import me.biocomp.hubitat_ci.api.common_api.DeviceWrapper

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
            assert lower == 'true' || lower == 'false': \
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
}