package de.tsl2.nano.h5;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;

import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.BeanValueMap;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.http.EHttpClient;
import de.tsl2.nano.core.util.parser.JSon;
import de.tsl2.nano.persistence.Persistence;

/**
 * superclasses NanoH5 to provide functionality to use h5 as simple frontend without own o/r mapping
 * and database access and generated beans.
 * An external backend has to provide services giving all bean (ui + data) informations inside the
 * the resonse payload. This should be done through a json represention of Bean, holding its data in
 * a map of maps. 
 */
public class NanoH5ExternalBackend extends NanoHTTPD {
    String backendUrl;
    String user;
    String passwd;

    public NanoH5ExternalBackend(int port, File wwwroot) throws IOException {
        super(port, wwwroot);
    }

    boolean isExternalBackendRequired() {
        return backendUrl == null ? (backendUrl = ENV.get("app.external.backend.url", null)) != null : true;
    }

    public void setBackendUrl(String backendUrl) {
        this.backendUrl = backendUrl;
    }

    public synchronized BeanDefinition<?> connect(Persistence persistence) {
        Objects.requireNonNull(backendUrl);
        Objects.requireNonNull(persistence.getConnectionUserName());
        Objects.requireNonNull(persistence.getConnectionPassword());

        String serviceUrl = backendUrl.equals("perssistence.connectionUrl") ? persistence.getConnectionUrl() : backendUrl;
        // TODO set header and inital query string
        // String json = NetUtil.get(backendUrl);
        EHttpClient httpClient = new EHttpClient(serviceUrl, new HashMap<>(), persistence.getConnectionUserName(), persistence.getConnectionPassword().toCharArray());
        String json = getResponse(httpClient);
        return new JSon().toObject(BeanValueMap.class, json);
    }

    protected String getResponse(EHttpClient httpClient) {
        return httpClient.rest(backendUrl, "GET", "application/json", null);
    }
}
