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
