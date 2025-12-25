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

package me.biocomp.hubitat_ci.device

import me.biocomp.hubitat_ci.validation.Flags
import spock.lang.Specification
import spock.lang.Unroll

class DeviceValidatorCommandTypesSpec extends Specification {
    // Exercises command() argument validation for map-based definitions, legacy aliases, edge cases, and relevant flags.
    // Coverage matrix: map(type/dataType) positive & invalid; legacy aliases positive; null/empty/unknown scalars; flag interactions for missing methods and arg-count mismatch.
    private static final List<Flags> BASE_FLAGS = [Flags.DontValidateCapabilities, Flags.DontValidateDefinition]

    private static HubitatDeviceScript runScript(String commands, List<Flags> validationFlags = BASE_FLAGS, String methodsBlock = defaultMethods()) {
        new HubitatDeviceSandbox("""
metadata {
    definition(name: 'n', namespace: 'nm', author: 'a') {
        ${commands}
    }
}

${methodsBlock}
""").run(validationFlags: validationFlags)
    }

    private static String defaultMethods() {
        return '''
def cmd() { 'no args' }
def cmd(String s) { "str ${s}" }
def cmd(Integer i) { i * 2 }
def cmd(BigDecimal d) { d + 1 }
def cmd(Boolean b) { b ? 'yes' : 'no' }
def cmd(Map m) { m['k'] ?: 'map' }
def cmd(List l) { l?.size() ?: 0 }
def parse(String s) { }
'''
    }

    @Unroll
    def "accepts map-based type definitions via #key"(String key, String value, Object invokeArg, Class expectedClass, Object expectedResult) {
        when:
        def script = runScript("command 'cmd', [[${key}: '${value}']]")
        def command = script.producedDefinition.commands[0]

        then:
        command.parameterTypes == [[(key): value]]
        command.method != null
        command.method.nativeParameterTypes == [expectedClass] as Class[]
        command.method.invoke(script, invokeArg) == expectedResult

        where:
        key        | value     | invokeArg               | expectedClass | expectedResult
        'type'     | 'number'  | 1                       | Integer       | 2
        'dataType' | 'string'  | 'abc'                   | String        | 'str abc'
    }

    @Unroll
    def "normalizes legacy alias '#alias'"(String alias, Object invokeArg, Class expectedClass, Object expectedResult) {
        when:
        def script = runScript("command 'cmd', ['${alias}']")
        def command = script.producedDefinition.commands[0]

        then:
        command.parameterTypes == [alias]
        command.method != null
        command.method.nativeParameterTypes == [expectedClass] as Class[]
        command.method.invoke(script, invokeArg) == expectedResult

        where:
        alias        | invokeArg                         | expectedClass | expectedResult
        'str'        | 'abc'                             | String        | 'str abc'
        'bool'       | true                              | Boolean       | 'yes'
        'int'        | 2                                 | Integer       | 4
        'double'     | new BigDecimal('1.5')             | BigDecimal    | new BigDecimal('2.5')
        'jsonobject' | [k: 'v']                          | Map           | 'v'
        'json'       | [k: 'v']                          | Map           | 'v'
        'colormap'   | [k: 'v']                          | Map           | 'v'
    }

    @Unroll
    def "rejects invalid map argument types (#desc)"(String desc, Map badDef) {
        when:
        runScript("command 'cmd', [${badDef}]")

        then:
        AssertionError e = thrown()
        e.message.contains('Argument type')
        e.message.contains('not supported')

        where:
        desc                       | badDef
        'empty map'                | [:]
        'missing type/dataType'    | [foo: 'bar']
        'unsupported type value'   | [type: 'bad']
        'unsupported dataType value' | [dataType: 'bad']
    }

    @Unroll
    def "rejects invalid scalar argument types (#desc)"(String desc, Object badDef) {
        when:
        runScript("command 'cmd', [${badDef}]")

        then:
        AssertionError e = thrown()
        e.message.contains('Argument type')
        e.message.contains('not supported')

        where:
        desc                | badDef
        'null type'         | null
        'empty string'      | "''"
        'unsupported alias' | "'unknown'"
    }

    def "allows missing command method when flag set"() {
        when:
        runScript("command 'cmd', ['long']", BASE_FLAGS + Flags.AllowMissingCommandMethod)

        then:
        notThrown(AssertionError)
    }

    def "fails when command method missing and flag not set"() {
        when:
        runScript("command 'cmd', ['string']", BASE_FLAGS, """
 def cmd() { 'no args' }
 def parse(String s) { }
 """)

        then:
        AssertionError e = thrown()
        e.message.contains('matching signature')
    }

    def "allows arg-count mismatch when flag set"() {
        when:
        runScript("command 'cmd', ['string']", BASE_FLAGS + [Flags.AllowCommandDefinitionWithNoArgsMatchAnyCommandWithSameName, Flags.AllowMissingCommandMethod], """
 def cmd() { 'no args' }
 def parse(String s) { }
 """)

        then:
        notThrown(AssertionError)
    }

    def "fails arg-count mismatch without flag"() {
        when:
        runScript("command 'cmd', ['string']", BASE_FLAGS, """
 def cmd() { 'no args' }
 def parse(String s) { }
 """)

        then:
        AssertionError e = thrown()
        e.message.contains('matching signature')
    }
}
