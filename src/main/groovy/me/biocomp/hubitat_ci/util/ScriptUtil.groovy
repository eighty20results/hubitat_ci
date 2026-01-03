package me.biocomp.hubitat_ci.util

import groovy.transform.CompileStatic

import java.lang.reflect.Method

@CompileStatic
class ScriptUtil {

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

        // Fall back: check for an internalState map attached to the script (used by tests/sandbox)
        try {
            if (scriptMetaClass.hasProperty(scriptObject, 'internalState')) {
                def internal = scriptMetaClass.getProperty(scriptObject as GroovyObjectSupport, 'internalState')
                if (internal instanceof Map && internal.containsKey(property)) {
                    return internal.get(property)
                }
            }
        } catch (Throwable ignored) {
            // best-effort fallback; don't fail if we can't read internalState
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

        // If userSettingsMap is not available yet (early initialization), try safe fallbacks
        if (userSettingsMap == null) {
            try {
                final def scriptMetaClass = scriptObject.getMetaClass()
                // If metaClass already exposes the property, set it there (this avoids going through setProperty again)
                if (scriptMetaClass.hasProperty(scriptObject, property)) {
                    scriptMetaClass.setProperty(scriptObject as GroovyObjectSupport, property, newValue)
                    return
                }

                // Otherwise, maintain a small internalState map on the script object and store the value there.
                if (!scriptMetaClass.hasProperty(scriptObject, 'internalState')) {
                    scriptMetaClass.setProperty(scriptObject as GroovyObjectSupport, 'internalState', [:])
                }
                def internal = scriptMetaClass.getProperty(scriptObject as GroovyObjectSupport, 'internalState') as Map
                internal.put(property, newValue)
                return
            } catch (Throwable t) {
                // best-effort; if anything fails fall through to the userSettingsMap path below (which may NPE if still null).
            }
        }

        // Use settings
        userSettingsMap.put(property, newValue)
    }
}
