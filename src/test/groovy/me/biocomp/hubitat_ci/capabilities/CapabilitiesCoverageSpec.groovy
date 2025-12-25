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
        Capabilities.findCapabilityByName("WindowShade")
    }
}
