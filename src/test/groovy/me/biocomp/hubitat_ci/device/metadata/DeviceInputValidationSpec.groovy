/*
 * Copyright 2025 Eighty/20 Results by Thomas Sjolshagen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
