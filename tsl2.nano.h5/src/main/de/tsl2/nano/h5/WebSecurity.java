package de.tsl2.nano.h5;

import java.net.InetAddress;
import java.util.Date;
import java.util.Map;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.secure.Crypt;
import de.tsl2.nano.core.util.DateUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.h5.NanoHTTPD.Response;

/**
 * tries to resolve simple OWASP security aspects.<p/>
 * asks ENV for properties starting with "app.session."
 * 
 * @author ts
 */
public class WebSecurity {
	private String antiCSRFKey;
	
	private static final String ENV_PREF = "app.session.";

	private static final String PREF_ANTICSRF = ENV_PREF + "anticsrf";
	public static final String HIDDEN_NAME = "hiddentoken";
	public static final String DEF_ALG = "AES";
	
    public static boolean useAntiCSRFToken() {
    	return ENV.get(PREF_ANTICSRF, true);
    }
    public String createAntiCSRFToken(NanoH5Session session) {
    	return Crypt.encrypt(session.getKey() + "-" 
    		+ (session.getWorkingObject() != null ? session.getWorkingObject().getId() : "NOTHING") + "-" 
    			+ System.currentTimeMillis(), getAntiCSRFKey(), ENV.get(PREF_ANTICSRF + ".algorithm", DEF_ALG));
    }
	private String getAntiCSRFKey() {
		if (antiCSRFKey == null)
			antiCSRFKey = Crypt.generatePassword(16);
		return antiCSRFKey;
	}
	public void checkAntiCSRFToken(NanoH5Session session, String token) {
		if (!useAntiCSRFToken() || antiCSRFKey == null)
			return;
		String sessionInfo = Crypt.decrypt(token, getAntiCSRFKey(), ENV.get(PREF_ANTICSRF + ".algorithm", DEF_ALG));
		String[] splitInfo = sessionInfo.split("[-]");
		boolean attack = false;
		if (splitInfo[0].equals(session.getKey())) {
			Date now = new Date();
			Date tokenAge = new Date(Long.valueOf(splitInfo[2]) + ENV.get(PREF_ANTICSRF + ".maxage.milliseconds", 300*1000));
			if (tokenAge.before(now))
				attack = true;
			else {
				if (ENV.get(PREF_ANTICSRF + ".check.form", false)) {
					if (!splitInfo[1].equals(session.getWorkingObject().getId()))
						attack = true;
				}
			}
		}
		else
			attack = true;
		
		if (attack)
			throw new IllegalStateException("anti CSRF token failure: possible CSRF attact");
	}

	public void addSessionHeader(NanoH5Session session, Response response) {
		if (session != null)
			addSessionID(session, response);
		
		String header = ENV.get(ENV_PREF + "httpheader", "X-XSS-Protection: 1; mode=block, X-Frame-Options: sameorigin;");
		String[] keyValues = header.split("\\s*[,:]\\s*");
		for (int i = 0; i < keyValues.length; i+=2) {
			response.addHeader(keyValues[i].trim(), keyValues[i+1].trim());
		}
	}
    public static Object getSessionID(Map<String, String> header, InetAddress requestor) {
        //header keys are case insenstive and are stored by NanoHttpd in lower case!
        return header.containsKey("if-none-match") ? header.get("if-none-match") 
            : header.containsKey("cookie") ? StringUtil.substring(header.get("cookie"), "session-id=", ";")
                : requestor;
    }

	public void addSessionID(NanoH5Session session, Response response) {
        if (session.getKey() != null) {
            String sessionMode = ENV.get(ENV_PREF + "id", "Cookie");
            String maxAge = "Max-Age=" + (ENV.get("session.timeout.millis", 30 * DateUtil.T_MINUTE) / 1000) + ";";
            if (sessionMode.equals("Cookie")) {
                String secure = ENV.get("app.ssl.activate", false) ? "secure; " : ";";
                response.addHeader("Set-Cookie", "session-id=" + session.getKey() + ";" + secure + maxAge 
                		+ ENV.get(ENV_PREF + "cookie.parameter", "SameSite=Strict; HttpOnly;"));                    
            } else if (sessionMode.equals("ETag")) {
                addETag(session.getKey(), response, maxAge);
            } else { // client-ip
            }
        }
	}
	public void addETag(String name, Response response, String maxAge) {
		response.addHeader("ETag", "\"" + name + "\"");
		// response.addHeader("Vary", "User-Agent");
		response.addHeader("Cache-Control",maxAge);
	}

}
