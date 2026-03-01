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

import groovy.transform.AutoImplement
import me.biocomp.hubitat_ci.api.common_api.InstalledAppWrapper

@AutoImplement
class InstalledAppWrapperImpl implements InstalledAppWrapper {
    InstalledAppWrapperImpl(Long id, String label, String name, Long parentAppId, script = null) {
        this.id = id
        this.label = label
        this.name = name
        this.parentAppId = parentAppId
        this.script = script
    }

    Long getId() { id }
    String getLabel() { label }
    String getName() { name }
    Long getParentAppId() { parentAppId }
    String getInstallationState() { "complete" }

    Object getScript() { script }

    void setScript(Object script) {
        this.script = script
    }

    def methodMissing(String name, args) {
        if (script && script.metaClass.respondsTo(script, name, args as Object[])) {
            return script."$name"(*args)
        }
        throw new MissingMethodException(name, this.class, args)
    }

    private final Long id
    private final String label
    private final String name
    private final Long parentAppId
    private Object script
}
