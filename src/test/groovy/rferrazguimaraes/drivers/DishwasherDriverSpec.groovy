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

package rferrazguimaraes.drivers

import me.biocomp.hubitat_ci.api.device_api.DeviceExecutor
import me.biocomp.hubitat_ci.api.common_api.DeviceWrapper
import me.biocomp.hubitat_ci.device.HubitatDeviceSandbox
import me.biocomp.hubitat_ci.validation.Flags
import spock.lang.Specification
import static org.junit.Assume.assumeTrue

/**
 * Starter spec for Home Connect Dishwasher child driver (forked MyDishwasherDriverSpec).
 * Focus: command delegation to parent and basic lifecycle wiring.
 */
class DishwasherDriverSpec extends Specification {

    private DeviceExecutor makeApi(List<Map> events, List<Map> runInCalls, Map userSettings) {
        def state = [:]

        // Must be a DeviceWrapper (DeviceChildExecutor/MetadataReader expects that type).
        DeviceWrapper device = [
                getDisplayName: { -> 'Device' },
                getLabel      : { -> 'Device' },
                getName       : { -> 'Device' },
                currentValue  : { String attr, boolean skipCache = false ->
                    if (attr == "AvailableOptionsList") {
                        return '["IntensivZone","ExtraDry"]'
                    }
                    return userSettings[attr]
                }
        ] as DeviceWrapper

        Mock(DeviceExecutor) {
            _ * getState() >> state
            _ * getDevice() >> device
            _ * sendEvent(_ as Map) >> { args -> events << args[0]; args[0] }
            _ * runIn(_ as Number, _ as String) >> { args -> runInCalls << [secs: (args[0] as Number).intValue(), method: args[1]] }
            _ * unschedule(_ as String) >> { args -> runInCalls << [unschedule: args[0]] }
            _ * parseJson(_ as String) >> { args -> new groovy.json.JsonSlurper().parseText(args[0]) }
        }
    }

    private Object makeParentStub(List<String> calls) {
        def stub = new Expando()
        stub.supportPowerState = true
        stub.supportPowertate = true
        stub.availablePrograms = [[name: 'Eco', key: 'Dishcare.Dishwasher.Program.Eco']]
        stub.programOptionsByKey = [Eco: [[name: 'IntensivZone', key: 'IntensivZone'], [name: 'ExtraDry', key: 'ExtraDry']]]
        stub.respondsTo = { String name -> (name == 'setPowerState' && stub.supportPowerState) || (name == 'setPowertate' && stub.supportPowertate) }
        stub.startProgram = { dev, key -> calls << "startProgram:${key}" }
        stub.stopProgram = { dev -> calls << "stopProgram" }
        stub.setPowerState = { dev, val -> calls << "setPowerState:${val}" }
        stub.setPowertate = { dev, val -> calls << "setPowertate:${val}" }
        stub.setSelectedProgram = { dev, key -> calls << "setSelectedProgram:${key}" }
        stub.setSelectedProgramOption = { dev, key, val -> calls << "setSelectedProgramOption:${key}:${val}" }
        stub.getAvailableProgramList = { dev -> stub.availablePrograms }
        stub.getAvailableProgramOptionsList = { dev, key -> stub.programOptionsByKey.Eco }
        stub.intializeStatus = { dev -> calls << "intializeStatus" }
        stub.getHomeConnectAPI = { -> [connectDeviceEvents: { id, interfaces -> calls << "connect:${id}" }, disconnectDeviceEvents: { id, interfaces -> calls << "disconnect:${id}" }] }
        stub.processMessage = { dev, text -> calls << "processMessage:${text}" }
        return stub
    }

    def "startProgram delegates to parent with selected program key"() {
        given:
        def driverFile = new File('SubmodulesWithScripts/hubitat-homeconnect/drivers/Dishwasher.groovy')
        assumeTrue(driverFile.exists())

        def sandbox = new HubitatDeviceSandbox(driverFile)
        def events = []
        def runInCalls = []
        def calls = []
        def parent = makeParentStub(calls)
        def api = makeApi(events, runInCalls, [parent: parent, selectedProgram: 'Eco'])

        when:
        def driver = sandbox.run(
                api: api,
                parent: parent,
                globals: [parent: parent],
                userSettingValues: [parent: parent, selectedProgram: 'Eco'],
                validationFlags: [Flags.AllowSectionsInDevicePreferences, Flags.DontValidatePreferences, Flags.DontRunScript],
                withLifecycle: true,
                customizeScriptBeforeRun: { script ->
                    script.metaClass.propertyMissing = { String name ->
                        if (name == 'runIn') return { Number secs, String methodName -> runInCalls << [secs: secs as Integer, method: methodName] }
                        if (name == 'unschedule') return { String methodName -> runInCalls << [unschedule: methodName] }
                        throw new MissingPropertyException(name, delegate.class)
                    }
                }
        )
        driver.state.foundAvailablePrograms = [[name: 'Eco', key: 'Dishcare.Dishwasher.Program.Eco']]
        driver.startProgram()

        then:
        // Use contains on stringified calls list to avoid false negatives due to Expando String/GString differences.
        calls*.toString().contains("startProgram:Dishcare.Dishwasher.Program.Eco")
    }

    def "setCurrentProgramOptions delegates option bools to parent"() {
        given:
        def driverFile = new File('SubmodulesWithScripts/hubitat-homeconnect/drivers/Dishwasher.groovy')
        assumeTrue(driverFile.exists())

        def sandbox = new HubitatDeviceSandbox(driverFile)
        def events = []
        def runInCalls = []
        def calls = []
        def parent = makeParentStub(calls)
        def api = makeApi(events, runInCalls, [parent: parent, selectedProgram: 'Eco', IntensivZone: true, ExtraDry: false])
        when:
        def driver = sandbox.run(
                api: api,
                parent: parent,
                globals: [parent: parent],
                userSettingValues: [parent: parent, selectedProgram: 'Eco', IntensivZone: true, ExtraDry: false],
                validationFlags: [Flags.AllowSectionsInDevicePreferences, Flags.DontValidatePreferences, Flags.DontRunScript],
                withLifecycle: true,
                customizeScriptBeforeRun: { script ->
                    script.metaClass.propertyMissing = { String name ->
                        if (name == 'runIn') return { Number secs, String methodName -> runInCalls << [secs: secs as Integer, method: methodName] }
                        if (name == 'unschedule') return { String methodName -> runInCalls << [unschedule: methodName] }
                        throw new MissingPropertyException(name, delegate.class)
                    }
                }
        )
        driver.state.foundAvailablePrograms = [[name: 'Eco', key: 'Dishcare.Dishwasher.Program.Eco']]
        driver.state.foundAvailableProgramOptions = [
                [name: 'IntensivZone', key: 'IntensivZone'],
                [name: 'ExtraDry', key: 'ExtraDry']
        ]
        driver.setCurrentProgramOptions()

        then:
        calls*.toString().containsAll([
                'setSelectedProgramOption:IntensivZone:true',
                'setSelectedProgramOption:ExtraDry:false'
        ])
    }
}
