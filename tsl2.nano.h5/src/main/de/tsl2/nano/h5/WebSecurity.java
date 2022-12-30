package de.tsl2.nano.h5;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.secure.Crypt;
import de.tsl2.nano.core.util.DateUtil;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.h5.NanoHTTPD.Response;

/**
 * tries to resolve simple OWASP security aspects.<p/>
 * asks ENV for properties starting with "app.session."
 * 
 * see: https://cheatsheetseries.owasp.org/IndexTopTen.html
 * see: https://infosec.mozilla.org/guidelines/web_security#cross-origin-resource-sharing
 * 
 * @author ts
 */
public class WebSecurity {
	private String antiCSRFKey;
	
	private static final String ENV_PREF = "app.session.";

	private static final String PREF_ANTICSRF = ENV_PREF + "anticsrf";
	public static final String CSRF_TOKEN = "csrftoken";
	public static final String DEF_ALG = "AES";
	
	private static final String ETAG = "ETag";
	private static final String REQUEST_COOKIE = "cookie";
	private static final String SET_COOKIE = "Set-Cookie";
	private static final String SEP = "---";
	private static final String SESSION_ID = "session-id";

	private static final String STANDARD_HEADER = 
	  "Referrer-Policy: same-origin\n"
	+ "X-XSS-Protection: 1;mode=block\n"
	+ "X-Permitted-Cross-Domain-Policies: master-only\n"
	+ "X-Frame-Options: sameorigin\n"
	+ "Content-Security-Policy: default-src 'self';\n"
	+ "X-Content-Type-Options: nosniff;\n"
	+ "Strict-Transport-Security: maxage=31536000;\n"
	+ "IncludeSubDomains: true;\n"
	+ "Content-Security-Policy: script-src 'self' 'unsafe-inline' 'XnXoXnXcXe-${requestId}';\n"
	+ "Content-Security-Policy: frame-src 'self';\n"
	+ "Content-Security-Policy: default-src 'self' 'unsafe-inline' filesystem ${service.url} ${websocket.url};\n"; 

    public static boolean useAntiCSRFToken() {
    	return ENV.get(PREF_ANTICSRF, true);
    }
    public static boolean useAntiCSRFTokenInContent() {
    	return useAntiCSRFToken() && ENV.get(PREF_ANTICSRF + ".incontent", true);
    }
    public static boolean useAntiCSRFTokenInHeader() {
    	return useAntiCSRFToken() && ENV.get(PREF_ANTICSRF + ".inheader", true);
    }
    public String createAntiCSRFToken(NanoH5Session session) {
    	try {
			return Crypt.encrypt(session.getKey() + SEP 
				+ (session.getWorkingObject() != null ? session.getWorkingObject().getId() : "NOTHING") + SEP 
					+ System.currentTimeMillis(), getAntiCSRFKey(), ENV.get(PREF_ANTICSRF + ".algorithm", DEF_ALG));
		} catch (Exception e) {
			if (session != null)
				session.close();
			ManagedException.forward(e);
			return null;
		}
    }
	private String getAntiCSRFKey() {
		if (antiCSRFKey == null)
			antiCSRFKey = Crypt.generatePassword((byte)16);
		return antiCSRFKey;
	}

	private void checkAntCSRFToken(NanoH5Session session, String token) {
		if (!useAntiCSRFToken())
			return;
		if (token == null)
			throw new IllegalStateException("request is missing anti-csrf token");
		String sessionInfo = Crypt.decrypt(token, getAntiCSRFKey(), ENV.get(PREF_ANTICSRF + ".algorithm", DEF_ALG));
		String[] splitInfo = sessionInfo.split("[-]{3}");
		boolean attack = false;
		if (splitInfo[0].equals(session.getKey())) {
			Date now = new Date();
			Date tokenAge = new Date(Long.valueOf(splitInfo[2]) + ENV.get(PREF_ANTICSRF + ".maxage.milliseconds", 3600*1000));
			if (tokenAge.before(now))
				attack = true;
			else {
				if (ENV.get(PREF_ANTICSRF + ".check.request", true)) {
					if (!splitInfo[1].equals(String.valueOf(session.getWorkingObject().getId())))
						attack = true;
				}
			}
		}
		else
			attack = true;
		
		if (attack) {
			session.close();
			throw new IllegalStateException("request outdated or unauthorized! closing session!");
		}
	}

	public void checkSession(NanoH5Session session, String method, Map<String, String> header, Map<String, String> params) {
		if (session.isNew())
			return;
		String sessionTag = header.get(REQUEST_COOKIE);
		String[] hs = sessionTag.split("[;]");
		Map<String, String> sessionValues = new LinkedHashMap<>(hs.length);
		Arrays.stream(hs).forEach(e -> MapUtil.add(sessionValues, e.trim().split("\\s*=\\s*")));

		checkSessionID(session, sessionValues);
		if (session.getUserAuthorization() != null) {
			if (useAntiCSRFTokenInHeader())
				checkAntCSRFToken(session, sessionValues.get(CSRF_TOKEN));
			if (method.equals("POST") && useAntiCSRFTokenInContent())
				checkAntCSRFToken(session, params.get(CSRF_TOKEN));
		}

	}

	private void checkSessionID(NanoH5Session session, Map<String, String> sessionValues) {
		String sessionId = sessionValues.get(SESSION_ID);
		if (sessionId == null)
			throw new IllegalStateException("missing " + SESSION_ID);
		if (!sessionId.equals(session.getKey()))
			throw new IllegalStateException("bad " + SESSION_ID);
	}
	public Response addSessionHeader(NanoH5Session session, Response response) {
		if (session != null) {
			addSessionID(session, response);
			if (session.getUserAuthorization() != null && useAntiCSRFTokenInHeader()) {
				response.addHeader(getSessionTagName(), CSRF_TOKEN + "=" + createAntiCSRFToken(session));
			}
		}
		Properties p = provideProperties(session);
		String header = getStandardHeader();
		String[] keyValues = header.split("\n");
		String k, v, kk = "", vv = "";
		for (int i = 0; i < keyValues.length; i++) {
			k = StringUtil.substring(keyValues[i], null, ":").trim();
			v = (k.equals(kk) ? vv + " ": "") + StringUtil.substring(keyValues[i], ":",null).trim();
			v = StringUtil.insertProperties(v, p);
			response.addHeader(k, v);
			kk = k;
			vv = v;
		}
		return response;
	}
	private Properties provideProperties(NanoH5Session session) {
		Properties p = new Properties(System.getProperties());
		String serviceUrl = ENV.get("service.url", "");
		String websocketUrl = session != null ? StringUtil.substring(serviceUrl.replace("http", "ws"), null, ":", true) 
			+ ":" + session.getWebsocketPort() : "";
		p.put("service.url", serviceUrl);
		p.put("websocket.url", websocketUrl);
		p.put("requestId", session != null ? session.getRequestId() : "");
		return p;
	}
	private String getStandardHeader() {
		String header = ENV.get(ENV_PREF + "httpheader", 
			  STANDARD_HEADER);
		return header;
	}
    public static Object getSessionID(Map<String, String> header, InetAddress requestor) {
        //header keys are case insenstive and are stored by NanoHttpd in lower case!
        return header.containsKey("if-none-match") ? header.get("if-none-match") 
            : header.containsKey(REQUEST_COOKIE) ? StringUtil.substring(header.get(REQUEST_COOKIE), SESSION_ID + "=", ";")
                : requestor;
    }

	protected void addSessionID(NanoH5Session session, Response response) {
		String sessionMode = getSessionTagName();
		String maxAge = getMaxAge();
		if (sessionMode.equals(SET_COOKIE)) {
			String secure = ENV.get("app.ssl.activate", false) ? "secure; " : ";";
			response.addHeader(SET_COOKIE, SESSION_ID + "=" + session.getKey() + ";" + secure + maxAge 
					+ ENV.get(ENV_PREF + "cookie.parameter", "SameSite=Strict; HttpOnly; Path=/"));                    
		} else if (sessionMode.equals(ETAG)) {
			addETag(session.getKey(), response, maxAge);
		} else { // client-ip
		}
	}
	private String getMaxAge() {
		return "Max-Age=" + (ENV.get(ENV_PREF + "timeout.millis", 30 * DateUtil.T_MINUTE) / 1000) + ";";
	}
	private String getSessionTagName() {
		return ENV.get(ENV_PREF + "tag", SET_COOKIE);
	}
	public void addETag(String name, Response response, String maxAge) {
		response.addHeader(ETAG, "\"" + name + "\"");
		// response.addHeader("Vary", "User-Agent");
		response.addHeader("Cache-Control",maxAge);
	}
}
