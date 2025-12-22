/*
 *  Copyright (c) 2025. - Eighty / 20 Results by Wicked Strong Chicks.
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

package me.biocomp.hubitat_ci.device.metadata

import me.biocomp.hubitat_ci.device.HubitatDeviceSandbox
import me.biocomp.hubitat_ci.validation.Flags
import spock.lang.Specification

class DeviceInputValidationSpec extends Specification {
    def "accepts capability and device selectors structurally"() {
        when:
        new HubitatDeviceSandbox('''
metadata {
    definition(name: "T", namespace: "n", author: "a") {
        capability "Switch"
        capability "Initialize"
    }
    preferences {
        input name: "sel1", type: "capability.switch"
        input name: "sel2", type: "device.switch"
    }
}

def on() {}
def off() {}
def parse(String s) { }
''').run(validationFlags: [Flags.AllowMissingDeviceInputNameOrType, Flags.DontRequireCapabilityImplementationMethods, Flags.DontValidateDefinition])
        then:
        notThrown(AssertionError)
    }

    def "validates enum requires options unless flag"() {
        when:
        new HubitatDeviceSandbox('''
metadata {
    definition(name: "T", namespace: "n", author: "a") { capability "Switch" }
    preferences { input name: "badEnum", type: "enum" }
}

def on() {}
def off() {}
def parse(String s) { }
''').run(validationFlags: [Flags.StrictEnumInputValidation])
        then:
        thrown(AssertionError)

        when:
        new HubitatDeviceSandbox('''
metadata {
    definition(name: "T", namespace: "n", author: "a") { capability "Switch" }
    preferences { input name: "okEnum", type: "enum", options: ["a","b"] }
}

def on() {}
def off() {}
def parse(String s) { }
''').run(validationFlags: [Flags.StrictEnumInputValidation])
        then:
        noExceptionThrown()

        when:
        new HubitatDeviceSandbox('''
metadata {
    definition(name: "T", namespace: "n", author: "a") { capability "Switch" }
    preferences { input name: "okEnum", type: "enum" }
}

def on() {}
def off() {}
def parse(String s) { }
''').run(validationFlags: [])
        then:
        thrown(AssertionError)
    }

    def "accepts additional types like date, textarea, mode, hub"() {
        expect:
        new HubitatDeviceSandbox('''
metadata {
    definition(name: "T", namespace: "n", author: "a") { capability "Switch" }
    preferences {
        input name: "d", type: "date"
        input name: "t", type: "textarea"
        input name: "m", type: "mode"
        input name: "h", type: "hub"
    }
}

def on() {}
def off() {}
def parse(String s) { }
''').run(validationFlags: [Flags.AllowMissingDeviceInputNameOrType])
    }
}
