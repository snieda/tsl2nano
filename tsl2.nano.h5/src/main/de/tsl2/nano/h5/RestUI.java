package de.tsl2.nano.h5;

import java.util.Arrays;
import java.util.Map;

import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.h5.ARESTDynamic.Status;
import de.tsl2.nano.h5.NanoHTTPD.Response;

public class RestUI extends ARestUI<NanoHTTPD.Response> {
    @Override
    protected Response createResponse(Status status, String mimeType, String html) {
        return new RESTDynamic().createResponse(status, mimeType, html);
    }

    @Override
    protected Status getStatus(Response restResponse) {
        return Arrays.stream(Status.values()).filter(s -> restResponse.getStatus().getRequestStatus() == s.http()).findFirst().get();
    }
    @Override
    protected String getData(Response restResponse) {
        return StringUtil.fromInputStream(restResponse.getData(), "\n");
    }
    @Override
    protected Response callRestService(String url, String method, Map<String, String> header, Map<String, String> parms,
            Map<String, String> payload) {
        return new RESTDynamic().serve(restUrl(url), method, header, parms, payload, true);
    }
    
}
