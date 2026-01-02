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

package rferrazguimaraes.apps

import me.biocomp.hubitat_ci.app.HubitatAppSandbox
import me.biocomp.hubitat_ci.validation.Flags
import me.biocomp.hubitat_ci.api.device_api.DeviceExecutor
import spock.lang.Specification
import static org.junit.Assume.assumeTrue

/**
 * App-level spec: create a child dishwasher driver via childDeviceResolver and ensure lifecycle wiring works.
 */
class DishwasherAppChildDeviceSpec extends Specification {
    def "parent app creates dishwasher child via resolver"() {
        given:
        def deviceFile = new File('SubmodulesWithScripts/hubitat-homeconnect/drivers/Dishwasher.groovy')
        assumeTrue(deviceFile.exists())

        def appFile = File.createTempFile("parentApp", ".groovy")
        appFile.text = """
            definition(name: \"Parent App\", namespace: \"test\", author: \"me\") { }
            preferences { }
            def installed() { addChildDevice('rferrazguimaraes', 'Home Connect Dishwasher', 'dni-1') }
            def getAvailableProgramList(device) { [[name: 'Eco', key: 'Dishcare.Dishwasher.Program.Eco']] }
            def intializeStatus(device) { }
            def getHomeConnectAPI() { [connectDeviceEvents: { id, interfaces -> }, disconnectDeviceEvents: { id, interfaces -> }] }
        """.stripIndent()

        def sandbox = new HubitatAppSandbox(appFile)
        def parentApi = Stub(me.biocomp.hubitat_ci.api.app_api.AppExecutor)

        when:
        def script = sandbox.run(
                api: parentApi,
                validationFlags: [Flags.DontValidateDefinition, Flags.DontValidatePreferences, Flags.DontRunScript],
                withLifecycle: true,
                globals: [:],
                childDeviceApi: Stub(DeviceExecutor),
                childDeviceResolver: { ns, type -> deviceFile })

        then:
        script.getChildDevices().size() == 1
        script.getChildDevice('dni-1') != null

        cleanup:
        appFile.delete()
    }
}
