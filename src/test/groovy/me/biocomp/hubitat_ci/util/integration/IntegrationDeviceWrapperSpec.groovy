/*
 *  Copyright (c) 2026. - Eighty / 20 Results by Wicked Strong Chicks.
 *  ALL RIGHTS RESERVED
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  You can contact us at mailto:info@eighty20results.com
 */

package me.biocomp.hubitat_ci.util.integration

import spock.lang.Specification
import spock.lang.Unroll

/**
 * Unit tests for {@link IntegrationDeviceWrapper}, focussing on boolean attribute coercion
 * via {@code sendEvent(isBooleanAttribute: true, ...)}.
 */
class IntegrationDeviceWrapperSpec extends Specification {

    /** Concrete stub of the abstract wrapper so we can instantiate it directly. */
    private static abstract class ConcreteWrapper extends IntegrationDeviceWrapper {}

    private ConcreteWrapper wrapper = Spy(ConcreteWrapper)

    // -------------------------------------------------------------------------
    // coerceToBooleanValue – static helper
    // -------------------------------------------------------------------------

    @Unroll
    def "coerceToBooleanValue(#raw) returns #expected"(Object raw, Boolean expected) {
        expect:
            IntegrationDeviceWrapper.coerceToBooleanValue(raw) == expected

        where:
            raw     | expected
            true    | true
            false   | false
            1       | true
            0       | false
            2       | true        // any non-zero int is truthy
            'true'  | true
            'false' | false
            'TRUE'  | true
            'FALSE' | false
    }

    @Unroll
    def "coerceToBooleanValue('#raw') throws for unsupported string '#raw'"(String raw) {
        when:
            IntegrationDeviceWrapper.coerceToBooleanValue(raw)

        then:
            AssertionError e = thrown()
            e.message.contains(raw)

        where:
            raw << ['yes', 'no', '1', '0', '']
    }

    def "coerceToBooleanValue(null) throws AssertionError"() {
        when:
            IntegrationDeviceWrapper.coerceToBooleanValue(null)

        then:
            AssertionError e = thrown()
    }

    // -------------------------------------------------------------------------
    // sendEvent – default behaviour (no isBooleanAttribute flag)
    // -------------------------------------------------------------------------

    def "sendEvent stores value as-is when isBooleanAttribute is not set"() {
        when:
            wrapper.sendEvent([name: 'active', value: 'true'])

        then:
            wrapper.currentValue('active', false) == 'true'   // String, not Boolean
    }

    def "sendEvent stores value as-is when isBooleanAttribute is false"() {
        when:
            wrapper.sendEvent([name: 'active', value: 'true', isBooleanAttribute: false])

        then:
            wrapper.currentValue('active', false) == 'true'   // String, not Boolean
    }

    // -------------------------------------------------------------------------
    // sendEvent – boolean coercion (isBooleanAttribute: true)
    // -------------------------------------------------------------------------

    @Unroll
    def "sendEvent with isBooleanAttribute coerces '#rawValue' to #expected"(
            Object rawValue, Boolean expected)
    {
        when:
            wrapper.sendEvent([name: 'active', value: rawValue, isBooleanAttribute: true])

        then:
            wrapper.currentValue('active', false) == expected

        where:
            rawValue | expected
            true     | true
            false    | false
            1        | true
            0        | false
            'true'   | true
            'false'  | false
            'TRUE'   | true
            'FALSE'  | false
    }

    def "sendEvent with isBooleanAttribute stores a proper Boolean, not a String"() {
        when:
            wrapper.sendEvent([name: 'flag', value: 'true', isBooleanAttribute: true])

        then:
            def stored = wrapper.currentValue('flag', false)
            stored instanceof Boolean
            (stored as Boolean) == true
    }

    def "sendEvent with isBooleanAttribute throws for unsupported value"() {
        when:
            wrapper.sendEvent([name: 'flag', value: 'yes', isBooleanAttribute: true])

        then:
            AssertionError e = thrown()
            e.message.contains('yes')
    }

    // -------------------------------------------------------------------------
    // currentValue with no skipCache overload
    // -------------------------------------------------------------------------

    def "currentValue(attributeName) returns null for unknown attribute"() {
        expect:
            wrapper.currentValue('unknown', false) == null
    }

    def "sendEvent overwrites a previously stored attribute value"() {
        given:
            wrapper.sendEvent([name: 'flag', value: 'false', isBooleanAttribute: true])

        when:
            wrapper.sendEvent([name: 'flag', value: 'true', isBooleanAttribute: true])

        then:
            wrapper.currentValue('flag', false) == true
    }

    def "updateSetting(Map) unwraps typed bool values and updates getSetting()"() {
        given:
            Map settings = [:]
            wrapper.bindSettingsMap(settings)

        when:
            wrapper.updateSetting('enableDebugLogging', [value: 'true', type: 'bool'])

        then:
            wrapper.getSetting('enableDebugLogging') == true
            settings.enableDebugLogging == true
    }

    def "updateSetting(Map) with no typed payload stores original map"() {
        given:
            Map settings = [:]
            wrapper.bindSettingsMap(settings)

        when:
            wrapper.updateSetting('rawSetting', [foo: 'bar'])

        then:
            wrapper.getSetting('rawSetting') == [foo: 'bar']
            settings.rawSetting == [foo: 'bar']
    }
}
