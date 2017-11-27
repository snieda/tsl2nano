package de.tsl2.nano.structure;

import de.tsl2.nano.core.messaging.IListener;


public class AConnection<T, D> implements IConnection<T, D>, IListener<INode<T, D>> {
    /** content */
    protected INode<T, D> destination;
    /** description or extension */
    protected D descriptor;

    /**
     * constructor
     * @param destination
     * @param descriptor
     */
    public AConnection(INode<T, D> destination, D descriptor) {
        super();
        this.destination = destination;
        this.descriptor = descriptor;
    }

    @Override
    public INode<T, D> getDestination() {
        return destination;
    }

    @Override
    public D getDescriptor() {
        return descriptor;
    }

    @Override
    public void handleEvent(INode<T, D> event) {
    }
}
