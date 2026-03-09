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

package me.biocomp.hubitat_ci.device

import spock.lang.Specification

/**
 * Tests for settings, state, and atomicState property assignment behavior.
 * These tests verify that the hubitat_ci implementation matches the Hubitat platform
 * behavior where these objects support both property and map-style assignment.
 */
class StateAndSettingsAssignmentTest extends Specification {

    def "state supports property assignment syntax"() {
        given: "a device script that uses state property assignment"
            def script = new HubitatDeviceSandbox("""
metadata {
    definition(name: "Test", namespace: "test", author: "test") {
        capability "Sensor"
    }
}

def installed() {
    state.myValue = "test"
    state.counter = 1
}

def testRead() {
    return [value: state.myValue, counter: state.counter]
}

Map parse(String s) { [:] }
""")

        when: "the script is run and installed() is called"
            def device = script.run(noValidation: true)
            device.installed()

        then: "state values are accessible"
            def result = device.testRead()
            result.value == "test"
            result.counter == 1
    }

    def "state supports map-style assignment syntax"() {
        given: "a device script that uses state map-style assignment"
            def script = new HubitatDeviceSandbox("""
metadata {
    definition(name: "Test", namespace: "test", author: "test") {
        capability "Sensor"
    }
}

def installed() {
    state["myValue"] = "test"
    state["counter"] = 1
}

def testRead() {
    return [value: state["myValue"], counter: state["counter"]]
}

Map parse(String s) { [:] }
""")

        when: "the script is run and installed() is called"
            def device = script.run(noValidation: true)
            device.installed()

        then: "state values are accessible"
            def result = device.testRead()
            result.value == "test"
            result.counter == 1
    }

    def "atomicState and state use the same backing store"() {
        given: "a device script that writes to state and reads from atomicState"
            def script = new HubitatDeviceSandbox("""
metadata {
    definition(name: "Test", namespace: "test", author: "test") {
        capability "Sensor"
    }
}

def installed() {
    state.myValue = "fromState"
    atomicState.myValue2 = "fromAtomicState"
}

def testRead() {
    return [
        stateValue: state.myValue,
        atomicStateValue: atomicState.myValue,
        stateValue2: state.myValue2,
        atomicStateValue2: atomicState.myValue2
    ]
}

Map parse(String s) { [:] }
""")

        when: "the script is run and installed() is called"
            def device = script.run(noValidation: true)
            device.installed()

        then: "values written to state are readable from atomicState and vice versa"
            def result = device.testRead()
            result.stateValue == "fromState"
            result.atomicStateValue == "fromState"
            result.stateValue2 == "fromAtomicState"
            result.atomicStateValue2 == "fromAtomicState"
    }

    def "settings supports property assignment syntax"() {
        given: "a device script that uses settings property assignment"
            def script = new HubitatDeviceSandbox("""
metadata {
    definition(name: "Test", namespace: "test", author: "test") {
        capability "Sensor"
    }
    preferences {
        input "configParam", "text", title: "Config", required: false
    }
}

def testWrite() {
    settings.customValue = "test"
    settings.customNumber = 42
}

def testRead() {
    return [value: settings.customValue, number: settings.customNumber]
}

Map parse(String s) { [:] }
""")

        when: "the script is run and settings are assigned"
            def device = script.run(noValidation: true)
            device.testWrite()

        then: "settings values are accessible"
            def result = device.testRead()
            result.value == "test"
            result.number == 42
    }

    def "settings supports map-style assignment syntax"() {
        given: "a device script that uses settings map-style assignment"
            def script = new HubitatDeviceSandbox("""
metadata {
    definition(name: "Test", namespace: "test", author: "test") {
        capability "Sensor"
    }
    preferences {
        input "configParam", "text", title: "Config", required: false
    }
}

def testWrite() {
    settings["customValue"] = "test"
    settings["customNumber"] = 42
}

def testRead() {
    return [value: settings["customValue"], number: settings["customNumber"]]
}

Map parse(String s) { [:] }
""")

        when: "the script is run and settings are assigned"
            def device = script.run(noValidation: true)
            device.testWrite()

        then: "settings values are accessible"
            def result = device.testRead()
            result.value == "test"
            result.number == 42
    }

    def "state assignment works with custom properties like _ensureSettingsPresent"() {
        given: "a device script that uses custom boolean properties in state"
            def script = new HubitatDeviceSandbox("""
metadata {
    definition(name: "Test", namespace: "test", author: "test") {
        capability "Sensor"
    }
}

def installed() {
    state._ensureSettingsPresent = true
}

def testRead() {
    return state._ensureSettingsPresent
}

Map parse(String s) { [:] }
""")

        when: "the script is run and custom property is assigned"
            def device = script.run(noValidation: true)
            device.installed()

        then: "custom property is accessible"
            device.testRead() == true
    }
}

