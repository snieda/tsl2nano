package de.tsl2.nano.h5;

import java.util.Map;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ISession;
import de.tsl2.nano.h5.NanoHTTPD.Response;
import de.tsl2.nano.serviceaccess.Authorization;
import de.tsl2.nano.serviceaccess.IAuthorization;

/**
 * @author Thomas Schneider
 */
public class RESTDynamic extends ARESTDynamic<NanoHTTPD.Response> {
	
	@Override
	void checkAuthentication(String url, String method, Map<String, String> header) throws SecurityException {
		if (ENV.get("app.login.administration", true))
			return;
		super.checkAuthentication(url, method, header);
		if (preAuthenticatedSession(header))
			return;
		getAuthentication(header);
	}

	private boolean preAuthenticatedSession(Map header) {
		NanoH5Session session = (NanoH5Session) header.get("session");
		if (session != null && WebSecurity.getSessionID(header, session.getInetAddress()).equals(session.getKey()))
			return true;
		return false;
	}

	private IAuthorization getAuthentication(Map<String, String> header) {
		String user = header.get("user");
		String passwd = header.get("password");
		if (user == null || passwd == null || Users.load().auth(user, passwd) == null)
			throw new SecurityException("not authenticated");
		return Authorization.create(user, Boolean.getBoolean("app.login.secure"));
	}

	public void checkAuthorization(String beanName, String actionOrAttribute, Map<String, String> header) throws IllegalAccessException {
		if (ENV.get("app.login.administration", true))
			return;
//		if (!getAuthentication(header).hasAccess(beanName, actionOrAttribute))
//			throw new IllegalAccessException("not authorized");
	}

	Response createResponse(Status status, String message) {
		return NanoH5.createResponse(Response.Status.lookup(status.http()), NanoHTTPD.MIME_PLAINTEXT, message);
	}

}
