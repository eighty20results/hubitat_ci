package me.biocomp.hubitat_ci.util

import groovy.transform.CompileStatic

import java.lang.reflect.Method

@CompileStatic
class ScriptUtil {
    // A static map for early property initialization (before userSettingsMap is available).
    // WeakHashMap allows script object keys to be garbage-collected once no longer
    // strongly referenced, preventing memory leaks across test runs.
    // synchronizedMap guards all compound read-modify-write operations.
    private static final Map<Object, Map<String, Object>> earlyInitStore =
        Collections.synchronizedMap(new WeakHashMap<Object, Map<String, Object>>())

    static def handleGetProperty(String property, def scriptObject, Map userSettingsMap, Map globals = null)
    {
        switch (property) {
            case "metaClass":
                return scriptObject.getMetaClass()

        // default: - continue processing below
        }

        final def getterMethodName = "get${property.capitalize()}"
        try {
            // Simple implementation of redirecting getter back to script class (if present)
            final def getter = scriptObject.getClass().getMethod(getterMethodName, [] as Class[])
            return getter.invoke(scriptObject);
        } catch (NoSuchMethodException e) {
            // It's OK, it might be handled below
        }

        // Allow explicit globals to mimic hub parent/child wiring
        if (globals != null && globals.containsKey(property)) {
            return globals.get(property)
        }

        final def scriptMetaClass = scriptObject.getMetaClass()

        // There's a property, return it.
        // This is to support overriding inputs via meta classes
        if (scriptMetaClass.hasProperty(scriptObject, property)) {
            return scriptMetaClass.getProperty(scriptObject as GroovyObjectSupport, property)
        }
        // It's a method. Hubitat returns string in this case.
        else if (scriptMetaClass.methods.find{ it.name == property } != null) {
            return property
        }

        // Use settings map if available
        if (userSettingsMap != null) {
            return userSettingsMap.get(property)
        }

        // Fall back: check earlyInitStore for properties set before userSettingsMap was available
        synchronized (earlyInitStore) {
            Map<String, Object> earlyProps = earlyInitStore.get(scriptObject)
            if (earlyProps != null && earlyProps.containsKey(property)) {
                return earlyProps.get(property)
            }
        }

        return null
    }

    // Just pick first method with same name and one parameter.
    // We can't distinguish parameters because we may not know newValue class if it's null.
    private static Method findSetterMethod(String methodName, def scriptObject)
    {
        final def foundMethods = scriptObject.getClass().methods.findAll{ it.name == methodName && it.parameters.size() == 1 }
        if (!foundMethods.empty)
        {
            return foundMethods.first()
        }

        null
    }

    static void handleSetProperty(String property, def scriptObject, def newValue, Map userSettingsMap)
    {
        // Handle special cases
        switch (property)
        {
            case "metaClass":
                scriptObject.setMetaClass((MetaClass) newValue);
                return;

        // default: - continue processing below
        }

        // Simple implementation of redirecting getter back to the script class (if present)
        final def setterMethodName = "set${property.capitalize()}"
        final def setter = findSetterMethod(setterMethodName, scriptObject)
        if (setter != null)
        {
            setter.invoke(scriptObject, newValue)
            return
        }

        // If userSettingsMap is not available yet (early initialization), use earlyInitStore
        if (userSettingsMap == null) {
            synchronized (earlyInitStore) {
                if (!earlyInitStore.containsKey(scriptObject)) {
                    earlyInitStore.put(scriptObject, new HashMap<String, Object>())
                }
                earlyInitStore.get(scriptObject).put(property, (Object) newValue)
            }
            return
        }

        // Migrate any early-init properties into userSettingsMap and release the earlyInitStore entry.
        // The containsKey check avoids acquiring the explicit lock when there is nothing to migrate.
        if (earlyInitStore.containsKey(scriptObject)) {
            synchronized (earlyInitStore) {
                Map<String, Object> earlyProps = earlyInitStore.remove(scriptObject)
                if (earlyProps != null) {
                    userSettingsMap.putAll(earlyProps)
                }
            }
        }

        // Use settings
        userSettingsMap.put(property, newValue)
    }
}
