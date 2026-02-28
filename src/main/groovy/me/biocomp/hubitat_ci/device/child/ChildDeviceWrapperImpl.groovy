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
    ChildDeviceWrapperImpl(DeviceWrapper delegate, String deviceNetworkId, Long parentAppId, Long parentDeviceId) {
        this.delegate = delegate
        this.deviceNetworkId = deviceNetworkId
        this.parentAppId = parentAppId
        this.parentDeviceId = parentDeviceId
    }

    @Delegate
    private final DeviceWrapper delegate

    String getDeviceNetworkId() { deviceNetworkId }
    Long getParentAppId() { parentAppId }
    Long getParentDeviceId() { parentDeviceId }

    private final String deviceNetworkId
    private final Long parentAppId
    private final Long parentDeviceId

    // Forward dynamic command/method calls (e.g. on(), off()) to the underlying delegate
    @SuppressWarnings(['unused'])
    public def methodMissing(String name, args) {
        // delegate may implement dynamic method handling; forward via invokeMethod
        return delegate?.invokeMethod(name, args)
    }

    @SuppressWarnings(['unused'])
    public def invokeMethod(String name, args) {
        // Prefer calling the actual method if present, otherwise forward to delegate
        def mm = this.metaClass.getMetaMethod(name, args)
        if (mm) {
            return mm.invoke(this, args)
        }
        return delegate?.invokeMethod(name, args)
    }
}
