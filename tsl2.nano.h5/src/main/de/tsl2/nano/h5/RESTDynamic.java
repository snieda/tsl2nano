package de.tsl2.nano.h5;

import java.util.Map;

import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.http.EHttpClient;
import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.h5.NanoHTTPD.Response;
import de.tsl2.nano.serviceaccess.Authorization;
import de.tsl2.nano.serviceaccess.IAuthorization;

/**
 * @author Thomas Schneider
 */
public class RESTDynamic extends ARestDynamicBackend<NanoHTTPD.Response> {
	
	@Override
	void checkSessionToken(String url, String method, Map<String, String> header) throws SecurityException {
		if (ENV.get("app.login.administration", true))
			return;
		super.checkSessionToken(url, method, header);
		if (preAuthenticatedSession(header))
			return;
		getAuthentication(header);
	}

	private boolean preAuthenticatedSession(Map header) {
		NanoH5Session session = (NanoH5Session) header.get(H5SESSION);
		return session != null && WebSecurity.getSessionID(header, session.getInetAddress()).equals(session.getKey());
	}

	private IAuthorization getAuthentication(Map<String, String> header) {
		// if (!BeanContainer.isInitialized() || !BeanContainer.instance().hasPermission(BASE_PATH, null))
		// 	throw new SecurityException("not authenticated");

		String[] auth = EHttpClient.getBasicAuthorization(header);
		if (auth.length < 1)
			throw new SecurityException("unauthorized");
		String password = auth.length > 1 ? auth[1] : "";
		if (Users.load().auth(auth[0], password) == null)
			throw new SecurityException("unknown user or password");
		return Authorization.create(auth[0], Boolean.getBoolean("app.login.secure"));
	}

	@Override
	public void checkAuthorization(String beanName, String actionOrAttribute, Map<String, String> header) throws IllegalAccessException {
		// if (ENV.get("app.login.administration", true))
		// 	return;
		IAuthorization auth;
		if (!(auth = getAuthentication(header)).hasAccess(beanName, actionOrAttribute))
			throw new IllegalAccessException("not authorized");
		ConcurrentUtil.setCurrent(auth, BeanContainer.instance());
	}

	@Override
	Response createResponse(Status status, String message) {
		return createResponse(status, NanoHTTPD.MIME_PLAINTEXT, message);
	}

	Response createResponse(Status status, String mimeType, String message) {
		return NanoH5.createResponse(Response.Status.lookup(status.http()), mimeType, message);
	}

}
