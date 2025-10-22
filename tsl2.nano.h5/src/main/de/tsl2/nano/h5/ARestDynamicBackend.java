package de.tsl2.nano.h5;

import java.util.Map;

import org.apache.commons.logging.Log;

import de.tsl2.nano.bean.def.BeanValueMap;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.parser.JSon;
import de.tsl2.nano.h5.rest.NanoBackendJaxrs;

public abstract class ARestDynamicBackend<RESPONSE> extends ARESTDynamic<RESPONSE> {
	private static final Log LOG = LogFactory.getLog(ARestDynamicBackend.class);

    @Override
	protected RESPONSE doServe(String url, String method, Map<String, String> header, Map<String, String> parms, Map<String, String> payload, boolean internalCall) throws IllegalAccessException {
        if (url.equals(BASE_PATH + "/backend")) {
            return createResponse(Status.OK, printBackendHelp());
		} else if (isBackendRequest(url)) {
			if (!internalCall) 
				checkSessionToken(url, method, header);
			checkMethod(method);
			
            return doBackendRequest(url, method, payload, header);
        } else {
            return super.doServe(url, method, header, parms, payload, internalCall);
        }

    }

    protected boolean isBackendRequest(String url) {
		return url.contains(BASE_PATH + "/backend");
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
    protected RESPONSE doBackendRequest(String url, String method, Map<String, String> payload, Map<String, String> header) {
		String identity = header.get("identity");
		if (identity == null)
			return createResponse(Status.BAD_REQUEST, "no identity given for backend request");
		Status status = Status.OK;
		// StringBuilder result = new StringBuilder();
		// result.append(evalBackendDataForGivenIdentity(identity));

        BeanValueMap resultBean = callNanoBackendApi(payload, identity);
		String json = new JSon().serialize(resultBean);
		LOG.info("REST (" + method + ") " + url + " --> " + json);
        return createResponse(status, json);
	}

    BeanValueMap callNanoBackendApi(Map<String, String> payload, String identity) {
        return new NanoBackendJaxrs().backend(identity, identity, (Map)payload);
    }

	public static String printBackendHelp() {
        String example = FileUtil.getFileString("doc/generated/example-beanvaluemap.json");
		return 
		"provides responses containing full dynamic beans with presentation+mapped-data.\n" +
		"the h5 framework itself is able to use this backend as base for its html5 presentation instead of\n" +
		"a direct connection to a database through its persistence informations on login\n" +
		"the login persistence informations have to provide the connectionUrl, connectionUser and connectionPassword\n" +
		"to connect and use the data given by such a backend request provider\n\n" +
		"base object is Bean.class with a structure like: " + example;
	}

    @Override
    String printManual() {
        String help = super.printManual();
        return help 
            + "\n------------------- RESTDynamic Backend informations ----------------------------\n"
            + "\n\tbackend-help        : prints a help for use as backend (see NanoBackendJaxrs)"
            + "\n\tbackend             : calls backend nanoh5 functionality"
            + "\n---------------------------------------------------------------------------------\n";
    }
}
