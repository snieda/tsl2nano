package de.tsl2.nano.h5.websocket.dialog;

public class WSResponse {
    public String value;

    public WSResponse(String value) {
        this.value = value;
    }
    @Override
    public boolean equals(Object obj) {
        return value != null && value.equals(((WSResponse)obj).value) || value == null && ((WSResponse)obj).value == null;
    }
}