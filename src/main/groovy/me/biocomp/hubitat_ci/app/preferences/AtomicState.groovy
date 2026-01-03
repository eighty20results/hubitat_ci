package me.biocomp.hubitat_ci.app.preferences

trait AtomicState
{
    String getId() { return metaClass.theClass.toString() }

    void finalValidation() {}

    @Override
    boolean equals(def otherState)
    {
        return id == (otherState as AtomicState).id
    }

    @Override
    int hashCode()
    {
        return id.hashCode()
    }

    abstract int getChildrenCount()
    abstract String toString()
    abstract void addChild(AtomicState state)
}