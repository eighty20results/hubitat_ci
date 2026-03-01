package me.biocomp.hubitat_ci.app.preferences

trait AtomicState
{
    String getId() { return metaClass.theClass.toString() }

    void finalValidation() {}

    @Override
    boolean equals(Object otherState)
    {
        if (this.is(otherState)) {
            return true
        }
        if (!(otherState instanceof AtomicState)) {
            return false
        }
        AtomicState other = (AtomicState) otherState
        return this.id == other.id
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