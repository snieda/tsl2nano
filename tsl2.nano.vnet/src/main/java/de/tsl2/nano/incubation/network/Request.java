package de.tsl2.nano.incubation.network;

import java.io.Serializable;
import java.util.concurrent.Future;

import de.tsl2.nano.core.ManagedException;

/**
 * Objects of this class will be sent from {@link JobServer}s {@link Work} to the remote {@link Worker} to check the
 * current state of the executing Job (see {@link JobContext#callable}.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class Request implements Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = -6312981186212043234L;
    private byte type;
    private Object response;

    public static final byte DONE = 1;
    public static final byte CANCELED = 2;
    public static final byte CANCEL = 3;
    public static final byte RESULT = 4;

    /**
     * constructor
     * 
     * @param type
     */
    public Request(byte type) {
        this(type, null);
    }

    /**
     * constructor
     * 
     * @param type
     * @param response
     */
    public Request(byte type, Object value) {
        super();
        this.type = type;
        this.response = value;
    }

    /**
     * @return Returns the type.
     */
    public byte getType() {
        return type;
    }

    /**
     * @return Returns the response.
     */
    public Object getResponse() {
        return response;
    }

    /**
     * @param response The response to set.
     */
    void setResponse(Object response) {
        this.response = response;
    }

    /**
     * evaluates the given task and fills the {@link #response} through informations of {@link Future}.
     * 
     * @param task server side working task
     */
    public void createResponse(Future<?> task) {
        switch (type) {
        case CANCEL:
            task.cancel(true);
            response = task.isCancelled();
            break;
        case DONE:
            response = task.isDone();
            break;
        case CANCELED:
            response = task.isCancelled();
            break;
        case RESULT:
            try {
                response = task.get();
            } catch (Exception e) {
                ManagedException.forward(e);
            }
            break;
        default:
            break;
        }
    }

    @Override
    public String toString() {
        return "Request(" + type + ": " + (response != null ? response : "no response available") + ")";
    }
}