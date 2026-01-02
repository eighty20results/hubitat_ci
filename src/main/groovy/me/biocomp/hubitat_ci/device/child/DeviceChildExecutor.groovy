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

import groovy.transform.CompileStatic
import me.biocomp.hubitat_ci.api.common_api.ChildDeviceWrapper
import me.biocomp.hubitat_ci.api.common_api.DeviceWrapper
import me.biocomp.hubitat_ci.api.device_api.DeviceExecutor

/**Decorator implementing component child device behaviors*/
@CompileStatic
class DeviceChildExecutor implements DeviceExecutor {
    DeviceChildExecutor(DeviceExecutor delegate,
                        DeviceWrapper parent,
                        ChildDeviceRegistry registry,
                        Closure childDeviceBuilder) {
        this.delegate = delegate
        this.parent = parent
        this.registry = registry
        this.childDeviceBuilder = childDeviceBuilder
    }

    @Delegate(excludes = ['addChildDevice', 'deleteChildDevice', 'getChildDevice', 'getChildDevices', 'getParent'])
    private final DeviceExecutor delegate

    private final DeviceWrapper parent
    private final ChildDeviceRegistry registry
    private final Closure childDeviceBuilder

    @Override
    ChildDeviceWrapper addChildDevice(String typeName, String deviceNetworkId, Map properties) {
        return (ChildDeviceWrapper) childDeviceBuilder?.call(null, typeName, deviceNetworkId, properties)
    }

    @Override
    ChildDeviceWrapper addChildDevice(String namespace, String typeName, String deviceNetworkId, Map properties) {
        return (ChildDeviceWrapper) childDeviceBuilder?.call(namespace, typeName, deviceNetworkId, properties)
    }

    @Override
    void deleteChildDevice(String deviceNetworkId) {
        registry.delete(deviceNetworkId)
    }

    @Override
    ChildDeviceWrapper getChildDevice(String deviceNetworkId) {
        return registry.getByDni(deviceNetworkId)
    }

    @Override
    List<ChildDeviceWrapper> getChildDevices() {
        return registry.listAll() as List<ChildDeviceWrapper>
    }

    @Override
    Object getParent() {
        return parent
    }
}
