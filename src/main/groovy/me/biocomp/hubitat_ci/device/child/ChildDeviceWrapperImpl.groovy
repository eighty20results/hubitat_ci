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

import groovy.transform.AutoImplement
import me.biocomp.hubitat_ci.api.common_api.ChildDeviceWrapper
import me.biocomp.hubitat_ci.api.common_api.DeviceWrapper

@AutoImplement
class ChildDeviceWrapperImpl implements ChildDeviceWrapper {
    ChildDeviceWrapperImpl(DeviceWrapper delegate, String deviceNetworkId, Long parentAppId, Long parentDeviceId, Object scriptDelegate = null) {
        this.delegate = delegate
        this.deviceNetworkId = deviceNetworkId
        this.parentAppId = parentAppId
        this.parentDeviceId = parentDeviceId
        this.scriptDelegate = scriptDelegate
    }

    @Delegate
    private final DeviceWrapper delegate

    // Forward dynamic command methods (for example, "on"/"off") to the wrapped device.
    def methodMissing(String name, Object args) {
        try {
            return delegate.invokeMethod(name, args)
        } catch (MissingMethodException ignored) {
            if (scriptDelegate != null) {
                return scriptDelegate.invokeMethod(name, args)
            }
            throw ignored
        }
    }

    def propertyMissing(String name) {
        try {
            return delegate.getProperty(name)
        } catch (MissingPropertyException ignored) {
            if (scriptDelegate != null) {
                return scriptDelegate.getProperty(name)
            }
            throw ignored
        }
    }

    void propertyMissing(String name, Object value) {
        try {
            delegate.setProperty(name, value)
        } catch (MissingPropertyException ignored) {
            if (scriptDelegate != null) {
                scriptDelegate.setProperty(name, value)
                return
            }
            throw ignored
        }
    }

    String getDeviceNetworkId() { deviceNetworkId }
    Long getParentAppId() { parentAppId }
    Long getParentDeviceId() { parentDeviceId }
    Object getScript() { scriptDelegate }

    private final String deviceNetworkId
    private final Long parentAppId
    private final Long parentDeviceId
    private final Object scriptDelegate
}
