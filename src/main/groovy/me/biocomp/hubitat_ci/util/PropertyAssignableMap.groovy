/*
 *  Copyright (c) 2026. - Eighty / 20 Results by Wicked Strong Chicks.
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

package me.biocomp.hubitat_ci.util

/**
 * A Map wrapper that supports Groovy property assignment syntax.
 * On the Hubitat platform, settings, state, and atomicState support both:
 * - Direct property assignment: settings.mysetting = "value"
 * - Map-style assignment: settings["mysetting"] = "value"
 *
 * This wrapper delegates all Map operations to a backing map and intercepts
 * property missing calls to enable the property assignment syntax.
 *
 * Note: This class cannot use @CompileStatic because propertyMissing needs
 * dynamic dispatch to intercept property access.
 */
class PropertyAssignableMap implements Map<String, Object> {
    @Delegate
    private final Map<String, Object> backingMap

    PropertyAssignableMap(Map<String, Object> backingMap) {
        this.backingMap = backingMap ?: new LinkedHashMap<String, Object>()
    }

    /**
     * Intercept property reads to delegate to map access.
     */
    def propertyMissing(String name) {
        return backingMap.get(name)
    }

    /**
     * Intercept property writes to delegate to map put.
     */
    void propertyMissing(String name, Object value) {
        backingMap.put(name, value)
    }
}


