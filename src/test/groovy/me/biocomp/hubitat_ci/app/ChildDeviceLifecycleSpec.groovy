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

package me.biocomp.hubitat_ci.app

import me.biocomp.hubitat_ci.validation.Flags
import spock.lang.Specification

class ChildDeviceLifecycleSpec extends Specification {
    def "app sandbox creates child device and runs lifecycle"() {
        given:
        def deviceFile = File.createTempFile("childDevice", ".groovy")
        deviceFile.text = """
            import groovy.transform.Field
            @Field def installedCalled = false
            @Field def initializeCalled = false
            metadata { definition(name: \"Test Child\", namespace: \"test\", author: \"me\") { capability \"Initialize\" } }
            def installed() { installedCalled = true }
            def initialize() { initializeCalled = true }
            def parse(String s) {}
        """.stripIndent()

        def appFile = File.createTempFile("parentApp", ".groovy")
        appFile.text = """
            definition(name: \"Parent App\", namespace: \"test\", author: \"me\")
            preferences { }
            def installed() { addChildDevice('test', 'Test Child', 'dni-1') }
            def initialize() { }
        """.stripIndent()

        def sandbox = new HubitatAppSandbox(appFile)

        when:
        def script = sandbox.run(api: Stub(me.biocomp.hubitat_ci.api.app_api.AppExecutor),
                validationFlags: [Flags.DontValidateDefinition, Flags.DontValidatePreferences],
                withLifecycle: true,
                globals: [:],
                childDeviceResolver: { ns, type -> deviceFile })

        then:
        script.getChildDevices().size() == 1
        def child = script.getChildDevice('dni-1')
        child != null
        child.script.installedCalled
        child.script.initializeCalled

        cleanup:
        deviceFile.delete()
        appFile.delete()
    }

    def "default childDeviceResolver finds device file from Scripts/Devices directory"() {
        given: "a device file placed in the Scripts/Devices directory (as the default resolver searches there)"
        def scriptsDevicesDir = new File("Scripts/Devices")
        scriptsDevicesDir.mkdirs()
        def deviceFile = new File(scriptsDevicesDir, "DefaultResolverTestDevice.groovy")
        deviceFile.text = """
            import groovy.transform.Field
            @Field def installedCalled = false
            metadata { definition(name: \"Default Resolver Test Device\", namespace: \"test\", author: \"me\") { capability \"Actuator\" } }
            def installed() { installedCalled = true }
            def initialize() { }
            def parse(String s) {}
        """.stripIndent()

        def appFile = File.createTempFile("parentApp", ".groovy")
        appFile.text = """
            definition(name: \"Parent App\", namespace: \"test\", author: \"me\")
            preferences { }
            def installed() { addChildDevice('test', 'DefaultResolverTestDevice', 'dni-default') }
            def initialize() { }
        """.stripIndent()

        def sandbox = new HubitatAppSandbox(appFile)

        when: "running sandbox without an explicit childDeviceResolver"
        def script = sandbox.run(api: Stub(me.biocomp.hubitat_ci.api.app_api.AppExecutor),
                validationFlags: [Flags.DontValidateDefinition, Flags.DontValidatePreferences],
                withLifecycle: true,
                globals: [:])

        then: "the default resolver finds the device from Scripts/Devices and creates the child"
        script.getChildDevices().size() == 1
        def child = script.getChildDevice('dni-default')
        child != null
        child.getDeviceNetworkId() == 'dni-default'

        cleanup:
        deviceFile.delete()
        appFile.delete()
    }
}
