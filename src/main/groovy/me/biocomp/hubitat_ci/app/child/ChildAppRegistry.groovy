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

package me.biocomp.hubitat_ci.app.child

import me.biocomp.hubitat_ci.api.common_api.InstalledAppWrapper

class ChildAppRegistry {
    InstalledAppWrapper add(Long id, InstalledAppWrapper app, Object script = null) {
        if (byId.containsKey(id)) {
            throw new IllegalStateException("Child app with id ${id} already exists")
        }
        byId[id] = new ChildAppRecord(app, script)
        order << app
        byLabel[app.label] = app
        return app
    }

    InstalledAppWrapper getById(Long id) {
        return byId[id]?.wrapper
    }

    InstalledAppWrapper getByLabel(String label) {
        return byLabel[label]
    }

    Object getScript(Long id) { byId[id]?.script }

    List<InstalledAppWrapper> listAll() { order.asImmutable() }

    void delete(Long id) {
        def rec = byId.remove(id)
        if (!rec) {
            throw new IllegalStateException("Child app with id ${id} does not exist")
        }
        try {
            def script = rec.script
            if (script && script.metaClass.respondsTo(script, 'uninstalled')) {
                script.uninstalled()
            }
        } finally {
            order.remove(rec.wrapper)
            byLabel.values().remove(rec.wrapper)
        }
    }

    static class ChildAppRecord {
        final InstalledAppWrapper wrapper
        final Object script
        ChildAppRecord(InstalledAppWrapper wrapper, Object script) {
            this.wrapper = wrapper
            this.script = script
        }
    }

    private final Map<Long, ChildAppRecord> byId = [:]
    private final Map<String, InstalledAppWrapper> byLabel = [:]
    private final List<InstalledAppWrapper> order = []
}
