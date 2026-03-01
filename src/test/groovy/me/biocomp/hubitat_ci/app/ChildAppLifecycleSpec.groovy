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

import me.biocomp.hubitat_ci.api.app_api.AppExecutor
import me.biocomp.hubitat_ci.validation.Flags
import spock.lang.Specification

class ChildAppLifecycleSpec extends Specification {

    def "addChildApp creates child app and runs installed and initialize lifecycle"() {
        given:
        def childAppFile = File.createTempFile("childApp", ".groovy")
        childAppFile.text = """
            import groovy.transform.Field
            @Field boolean installedCalled = false
            @Field boolean initializeCalled = false
            definition(name: "Child App", namespace: "test", author: "me")
            preferences { }
            def installed() { installedCalled = true }
            def initialize() { initializeCalled = true }
        """.stripIndent()

        def parentAppFile = File.createTempFile("parentApp", ".groovy")
        parentAppFile.text = """
            definition(name: "Parent App", namespace: "test", author: "me")
            preferences { }
            def installed() { addChildApp('test', 'Child App', 'My Child') }
            def initialize() { }
        """.stripIndent()

        def sandbox = new HubitatAppSandbox(parentAppFile)

        when:
        def script = sandbox.run(
                api: Stub(AppExecutor),
                validationFlags: [Flags.DontValidateDefinition, Flags.DontValidatePreferences],
                withLifecycle: true,
                childAppResolver: { ns, type -> childAppFile })

        then:
        script.getChildApps().size() == 1
        def child = script.getChildApps().first()
        child.label == 'My Child'
        child.name == 'Child App'
        child.script.installedCalled == true
        child.script.initializeCalled == true

        cleanup:
        childAppFile.delete()
        parentAppFile.delete()
    }

    def "getChildAppById and getChildAppByLabel return the correct child app"() {
        given:
        def childAppFile = File.createTempFile("childApp", ".groovy")
        childAppFile.text = """
            definition(name: "Child App", namespace: "test", author: "me")
            preferences { }
            def installed() { }
            def initialize() { }
        """.stripIndent()

        def parentAppFile = File.createTempFile("parentApp", ".groovy")
        parentAppFile.text = """
            definition(name: "Parent App", namespace: "test", author: "me")
            preferences { }
            def installed() { addChildApp('test', 'Child App', 'My Child') }
            def initialize() { }
        """.stripIndent()

        def sandbox = new HubitatAppSandbox(parentAppFile)

        when:
        def script = sandbox.run(
                api: Stub(AppExecutor),
                validationFlags: [Flags.DontValidateDefinition, Flags.DontValidatePreferences],
                withLifecycle: true,
                childAppResolver: { ns, type -> childAppFile })

        then:
        def child = script.getChildApps().first()
        script.getChildAppById(child.id) != null
        script.getChildAppById(child.id).is(child)
        script.getChildAppByLabel('My Child') != null
        script.getChildAppByLabel('My Child').is(child)

        cleanup:
        childAppFile.delete()
        parentAppFile.delete()
    }

    def "deleteChildApp removes the child app and invokes uninstalled"() {
        given:
        def childAppFile = File.createTempFile("childApp", ".groovy")
        childAppFile.text = """
            import groovy.transform.Field
            @Field boolean uninstalledCalled = false
            definition(name: "Child App", namespace: "test", author: "me")
            preferences { }
            def installed() { }
            def initialize() { }
            def uninstalled() { uninstalledCalled = true }
        """.stripIndent()

        def parentAppFile = File.createTempFile("parentApp", ".groovy")
        parentAppFile.text = """
            definition(name: "Parent App", namespace: "test", author: "me")
            preferences { }
            def installed() { addChildApp('test', 'Child App', 'My Child') }
            def initialize() { }
        """.stripIndent()

        def sandbox = new HubitatAppSandbox(parentAppFile)
        def script = sandbox.run(
                api: Stub(AppExecutor),
                validationFlags: [Flags.DontValidateDefinition, Flags.DontValidatePreferences],
                withLifecycle: true,
                childAppResolver: { ns, type -> childAppFile })

        def child = script.getChildApps().first()
        def childScript = child.script
        def childId = child.id

        when:
        script.deleteChildApp(childId)

        then:
        script.getChildApps().isEmpty()
        script.getChildAppById(childId) == null
        childScript.uninstalledCalled == true

        cleanup:
        childAppFile.delete()
        parentAppFile.delete()
    }
}
