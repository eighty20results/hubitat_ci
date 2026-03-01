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

package me.biocomp.hubitat_ci.capabilities

import spock.lang.Specification

class CapabilitiesCoverageSpec extends Specification {
    private static final Set<String> unsupportedCatalogCapabilities = ["VideoCapture"] as Set<String>

    private static Set<String> catalogCapabilities() {
        def file = new File("src/main/groovy/me/biocomp/hubitat_ci/capabilities/HubitatCapabilities.csv")
        assert file.exists(): "Capability catalog file not found: ${file}"

        Set<String> parsed = [] as Set<String>

        file.eachLine { raw ->
            def line = raw?.trim()
            if (!line || line.toLowerCase().startsWith("cabability,")) {
                return
            }
            if (line.startsWith("[Only SmartThings]")) {
                return
            }

            def head = line.contains(",") ? line.split(",", 2)[0].trim() : line
            head = head.replaceFirst(/^\d+\s+/, "").trim()
            head = head.replaceFirst(/\s*\(.*\)$/, "").trim()

            if (head) {
                parsed << head
            }
        }

        parsed.removeAll(unsupportedCatalogCapabilities)
        return parsed
    }

    def "new capabilities are discoverable"() {
        expect:
        Capabilities.findCapabilityByName("AirQuality")
        Capabilities.findCapabilityByName("PM2_5")
        Capabilities.findCapabilityByName("PM10")
        Capabilities.findCapabilityByName("PushableButton")
        Capabilities.findCapabilityByName("ReleasableButton")
        Capabilities.findCapabilityByName("VoltageMeasurement")
        Capabilities.findCapabilityByName("MediaPlayback")
        Capabilities.findCapabilityByName("PowerSource")
        Capabilities.findCapabilityByName("Variable")
        Capabilities.findCapabilityByName("WindowShade")
    }

    def "capability catalog entries are discoverable"() {
        when:
        def missing = catalogCapabilities().findAll { Capabilities.findCapabilityByName(it) == null }.sort()

        then:
        assert missing.isEmpty(): "Catalog capabilities not discoverable: ${missing}"
    }

    def "code capabilities are represented in capability catalog"() {
        given:
        def catalog = catalogCapabilities() + unsupportedCatalogCapabilities

        when:
        def missingFromCatalog = Capabilities.capabilitiesByDriverDefinition.keySet().findAll { !catalog.contains(it) }.sort()

        then:
        assert missingFromCatalog.isEmpty(): "Capabilities missing from HubitatCapabilities.csv: ${missingFromCatalog}"
    }
}
