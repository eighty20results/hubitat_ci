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

package me.biocomp.hubitat_ci.device.child

import me.biocomp.hubitat_ci.api.common_api.ChildDeviceWrapper

class ChildDeviceRegistry {
    ChildRecord add(String dni, ChildDeviceWrapper device, Object script = null) {
        if (!dni) {
            throw new IllegalArgumentException("deviceNetworkId is required")
        }
        if (byDni.containsKey(dni)) {
            throw new IllegalStateException("Child device with dni ${dni} already exists")
        }
        def rec = new ChildRecord(device, script)
        byDni[dni] = rec
        order << rec
        return rec
    }

    ChildDeviceWrapper getByDni(String dni) { byDni[dni]?.wrapper }

    Object getScript(String dni) { byDni[dni]?.script }

    List<ChildDeviceWrapper> listAll() { order.collect { it.wrapper }.asImmutable() }

    void delete(String dni) {
        def rec = byDni.remove(dni)
        if (!rec) {
            throw new IllegalStateException("Child device with dni ${dni} does not exist")
        }
        try {
            def script = rec.script
            if (script && script.metaClass.respondsTo(script, 'uninstalled')) {
                script.uninstalled()
            }
        } finally {
            order.remove(rec)
        }
    }

    static class ChildRecord {
        final ChildDeviceWrapper wrapper
        final Object script
        ChildRecord(ChildDeviceWrapper wrapper, Object script) {
            this.wrapper = wrapper
            this.script = script
        }
    }

    private final Map<String, ChildRecord> byDni = [:]
    private final List<ChildRecord> order = []
}

