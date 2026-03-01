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

import groovy.transform.CompileStatic
import me.biocomp.hubitat_ci.api.app_api.AppExecutor
import me.biocomp.hubitat_ci.api.common_api.ChildDeviceWrapper
import me.biocomp.hubitat_ci.api.common_api.InstalledAppWrapper
import me.biocomp.hubitat_ci.device.child.ChildDeviceRegistry

/**Decorator implementing child app/device behaviors on top of a provided AppExecutor*/
@CompileStatic
class AppChildExecutor implements AppExecutor {
    AppChildExecutor(AppExecutor delegate,
                     InstalledAppWrapper parent,
                     ChildDeviceRegistry childDeviceRegistry,
                     Closure childDeviceBuilder,
                     Closure childAppBuilder,
                     ChildAppRegistry childAppRegistry) {
        this.delegate = delegate
        this.parent = parent
        this.childDeviceRegistry = childDeviceRegistry
        this.childDeviceBuilder = childDeviceBuilder
        this.childAppBuilder = childAppBuilder
        this.childAppRegistry = childAppRegistry
    }

    @Delegate(excludes = ['addChildDevice', 'deleteChildDevice', 'getChildDevice', 'getChildDevices', 'addChildApp', 'getChildApps', 'getChildAppById', 'getChildAppByLabel', 'deleteChildApp', 'getParent', 'getAllChildApps', 'getAllChildDevices'])
    private final AppExecutor delegate

    private final InstalledAppWrapper parent
    private final ChildDeviceRegistry childDeviceRegistry
    private final Closure childDeviceBuilder
    private final Closure childAppBuilder
    private final ChildAppRegistry childAppRegistry

    @Override
    ChildDeviceWrapper addChildDevice(String namespace, String typeName, String deviceNetworkId, Long hubId, Map options) {
        return (ChildDeviceWrapper) childDeviceBuilder?.call(namespace, typeName, deviceNetworkId, hubId, options)
    }

    @Override
    ChildDeviceWrapper addChildDevice(String namespace, String typeName, String deviceNetworkId, Map properties) {
        return addChildDevice(namespace, typeName, deviceNetworkId, null, properties)
    }

    @Override
    ChildDeviceWrapper addChildDevice(String namespace, String typeName, String deviceNetworkId) {
        return addChildDevice(namespace, typeName, deviceNetworkId, null, [:])
    }

    @Override
    ChildDeviceWrapper addChildDevice(String namespace, String typeName, String deviceNetworkId, Long hubId) {
        return addChildDevice(namespace, typeName, deviceNetworkId, hubId, [:])
    }

    @Override
    void deleteChildDevice(String deviceNetworkId) {
        childDeviceRegistry.delete(deviceNetworkId)
    }

    @Override
    ChildDeviceWrapper getChildDevice(String deviceNetworkId) {
        return childDeviceRegistry.getByDni(deviceNetworkId)
    }

    @Override
    List getChildDevices() {
        return mergeChildDeviceLists(delegate.getChildDevices(), childDeviceRegistry.listAll())
    }

    @Override
    List getAllChildDevices() {
        List delegateDevices = delegate.getAllChildDevices()
        if (delegateDevices == null) {
            delegateDevices = delegate.getChildDevices()
        }
        return mergeChildDeviceLists(delegateDevices, childDeviceRegistry.listAll())
    }

    @Override
    InstalledAppWrapper addChildApp(String namespace, String smartAppVersionName, String label, Map properties) {
        return (InstalledAppWrapper) childAppBuilder?.call(namespace, smartAppVersionName, label, properties)
    }

    @Override
    InstalledAppWrapper addChildApp(String namespace, String smartAppVersionName, String label) {
        return addChildApp(namespace, smartAppVersionName, label, [:])
    }

    @Override
    List getChildApps() {
        return childAppRegistry.listAll()
    }

    @Override
    List getAllChildApps() { return getChildApps() }

    @Override
    InstalledAppWrapper getChildAppById(Long childAppId) { childAppRegistry.getById(childAppId) }

    @Override
    InstalledAppWrapper getChildAppByLabel(String childAppLabel) { childAppRegistry.getByLabel(childAppLabel) }

    @Override
    void deleteChildApp(Long id) {
        childAppRegistry.delete(id)
    }

    @Override
    InstalledAppWrapper getParent() {
        return parent
    }

    private static List mergeChildDeviceLists(List existingChildren, List createdChildren) {
        LinkedHashSet result = new LinkedHashSet()
        if (existingChildren != null) {
            result.addAll(existingChildren)
        }
        if (createdChildren != null) {
            result.addAll(createdChildren)
        }
        return new ArrayList(result)
    }
}
