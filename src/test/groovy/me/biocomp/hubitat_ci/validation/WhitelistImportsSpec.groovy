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

package me.biocomp.hubitat_ci.validation

import me.biocomp.hubitat_ci.device.HubitatDeviceSandbox
import spock.lang.Specification

class WhitelistImportsSpec extends Specification {
    def "allows forum/docs imports"() {
        expect:
        new HubitatDeviceSandbox('''
            import groovy.transform.Field
            import groovy.json.JsonSlurperClassic
            import groovy.xml.StreamingMarkupBuilder
            import groovy.sql.Sql
            import java.security.MessageDigest
            import java.security.SecureRandom
            import java.util.concurrent.ConcurrentHashMap
            import java.util.regex.Pattern
            import java.net.URL
            import hubitat.helper.ColorUtils

            metadata { definition(name: "T", namespace: "n", author: "a") { capability "Switch" } }
            def on() {}
            def off() {}
            def parse(String s) { }
        ''').run(validationFlags: [Flags.DontRestrictGroovy, Flags.DontRequireCapabilityImplementationMethods])
    }

    def "blocks disallowed import unless legacy flag"() {
        when:
        new HubitatDeviceSandbox('''
            import java.lang.ProcessBuilder
            metadata { definition(name: "T", namespace: "n", author: "a") { capability "Switch" } }
            def on() {}
            def off() {}
            def parse(String s) { }
            def pb() { new ProcessBuilder() }
        ''').run(validationFlags: [Flags.DontRequireCapabilityImplementationMethods])
        then:
        noExceptionThrown()

        when:
        new HubitatDeviceSandbox('''
            import java.lang.ProcessBuilder
            metadata { definition(name: "T", namespace: "n", author: "a") { capability "Switch" } }
            def on() {}
            def off() {}
            def parse(String s) { }
            def pb() { new ProcessBuilder() }
        ''').run(validationFlags: [Flags.AllowLegacyImports, Flags.DontRequireCapabilityImplementationMethods])
        then:
        noExceptionThrown()
    }
}
