package de.tsl2.nano.h5;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;

import de.tsl2.nano.action.IAction;
import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.IAttributeDefinition;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ISession;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.PrimitiveUtil;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.core.util.parser.JSon;

/**
 * @author Thomas Schneider

 * @param <RESPONSE> type of response depending on used HttpServer
 * 
 *         <pre>
 * provides a cover for bean data access through rest. only JSON is allowed.
 * 
 * example:
 * 
 * POST:
 * http://localhost:8067/rest/address/create
 * 
 * {"city": "Buxdehude", "street": "Berliner Str.1"}
 *  
 * GET:
 * /rest/address/id/0815
 * /rest/address/id/0815/city
 * /rest/address/city/berlin
 * 
 * PUT:
 * /rest/address/id/0815/city/frankfurt
 * 
 * DELETE:
 * /rest/address/id/0815/delete
 * 
 *         </pre>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class ARESTDynamic<RESPONSE> {
	private static final Log LOG = LogFactory.getLog(ARESTDynamic.class);
	static final String BODY = ENV.get("app.rest.payload.key", "postData");
	public static String BASE_PATH = ENV.get("app.rest.basepath", "/rest");
	static String USAGE = BASE_PATH + "/{entity}/{attribute-or-action}/{query}/{optional-output-attribute}/{PUT:value}";
	static final String API_KEY = StringUtil.toHexString(StringUtil.cryptoHash(ENV.get("app.rest.apikey", "ein23einfacherrestdynamickey!")));
	public static final String H5SESSION = "h5session";
	enum Status {
		OK(200), CREATED(201), BAD_REQUEST(400), UNAUTHORIZED(401), FORBIDDEN(403), NOT_FOUND(404), INTERNAL_ERROR(500);
		int s; Status(int s) { this.s = s;} public int http() {return s;}
	}
	enum Methods {
		GET, PUT, DELETE, POST, OPTIONS;
		static String matchingExpression() {
			StringBuilder buf = new StringBuilder();
			Arrays.stream(values()).forEach(m -> buf.append("|" + m));
			return buf.substring(1);
		}
	}
	public static boolean canRest(String uri) {
		return (uri.endsWith(BASE_PATH) || uri.contains(BASE_PATH + "/")) && ENV.get("app.rest.active", true);
	}

	RESPONSE serve(String url, String method, Map<String, String> header) {
		return serve(url, method, header, null);
	}
	RESPONSE serve(String url, String method, Map<String, String> header, Map<String, String> parms) {
		return serve(url, method, header, parms, null);
	}
	RESPONSE serve(String url, String method, Map<String, String> header, Map<String, String> parms, Map<String, String> payload) {
		return serve(url, method, header, parms, payload, false);
	}
	RESPONSE serve(String url, String method, Map<String, String> header, Map<String, String> parms, Map<String, String> payload, boolean internalCall) {
		try {
			if (url.equals(BASE_PATH) || method.equals("OPTIONS"))
				return createResponse(Status.OK, printManual());
			else if (url.equals(BASE_PATH + "/health"))
				return createResponse(Status.OK, "{ build-tsl2.nano.h5: " + ENV.getBuildInformations() + " }");
			else if (url.equals(BASE_PATH + "/entities"))
				return createResponse(Status.OK, printEntities());
			else if (url.equals(BASE_PATH + "/entitiesjson"))
				return createResponse(Status.OK, printEntitiesJSON());
			else if (url.equals(BASE_PATH + "/backend"))
				return createResponse(Status.OK, printBackendHelp());
			
			if (!internalCall) 
				checkAuthentication(url, method, header);
			checkMethod(method);
			String beanName = get(url, BASE_PATH, "entity");
			String actionOrAttribute = get(url, beanName, "attribute-or-action");
			checkAuthorization(beanName, actionOrAttribute, header);
			if (isBackendRequest(url)) {
				return doBackendRequest(url, method, payload, header, beanName, actionOrAttribute);
			} else if (method.equals("POST")) {
				return doPost(url, method, payload, beanName, actionOrAttribute);
			} else {
				return doWithQuery(url, method, parms, beanName, actionOrAttribute);
			}
		} catch (SecurityException ex) {
			LOG.error(ex);
			return createResponse(Status.FORBIDDEN, ex.getMessage());
		} catch (IllegalAccessException ex) {
			LOG.error(ex);
			return createResponse(Status.UNAUTHORIZED, ex.getMessage());
		} catch (IllegalArgumentException ex) {
			LOG.error(ex);
			return createResponse(Status.BAD_REQUEST, ex.getMessage() + "\n" + printManual());
		} catch (Exception ex) {
			LOG.error(ex);
			return createResponse(Status.INTERNAL_ERROR, ex.toString());
		}
	}

	void checkAuthentication(String url, String method, Map<String, String> header) throws SecurityException {
		if (header == null)
			throw new SecurityException("please provide a header map with Authorization");
		String auth = header.get("authorization");
		if (!Util.isEmpty(auth)) {
			String[] split = auth.split("\\s");
			String digest = split[split.length-1];
			StringBuilder buf = new StringBuilder();
			ISession session = (ISession) Util.untyped(header.get(H5SESSION));
			if (session != null)
				buf.append(session.getId().toString());
			for (int i = 0; i < split.length-1; i++) {
				buf.append(split[i]);
			}
			if (digest.equals(createDigest(url, method, buf.toString())))
				return;
		}
		throw new SecurityException("not allowed!");
	}
	abstract void checkAuthorization(String beanName, String actionOrAttribute, Map<String, String> header) throws IllegalAccessException;
	abstract RESPONSE createResponse(Status status, String message);

	RESPONSE doWithQuery(String url, String method, Map<String, String> parms, String beanName, String actionOrAttribute) {
		StringBuilder result = new StringBuilder();
		Status status = Status.OK;
		Collection<Object> instances = null;
		String query = get(url, actionOrAttribute, "query");
		instances = getBeans(beanName, actionOrAttribute, query);
		String output;
		if (!Util.isEmpty(instances)) {
			for (Object instance : instances) {
				switch (method) {
				case "GET":
					output = StringUtil.substring(url, query + "/", null, true, true);
					if (output != null)
						checkUrlEnd(url, output, "optional-output-attribute");
					else
						checkUrlEnd(url, query, "query");
					result = result.append((output != null ? String.valueOf(Bean.getBean(instance).getAttribute(output).getValue()) 
							: BeanUtil.toJSON(instance)) + ",");
					break;
				case "PUT":
					output = get(url, query, "output-attribute");
					String urlEnd = StringUtil.substring(url, query, null);
					doPut(urlEnd, output, instance);
					if (result.length() == 0)
						result.append("changed (id): ");
					result.append(Bean.getBean(instance).getId() + " ");
					break;
				case "DELETE":
					checkUrlEnd(url, query, "query");
					BeanContainer.instance().delete(instance);
					if (result.length() == 0)
						result.append("deleted (id): ");
					result.append(Bean.getBean(instance).getId() + " ");
					break;
				default:
					throw new IllegalArgumentException("unknown method");
				}
			}
		} else {
			status = Status.NOT_FOUND;
			result.append(actionOrAttribute + ":" + query + " not found");
		}
		if (instances.size() > 0)
			result.setLength(result.length()-1);
		LOG.info("REST (" + method + ") " + url + " --> " + result);
		return createResponse(status, result.toString());
	}

	void doPut(String url, String output, Object instance) {
		String value = get(url, output, "value");
		checkUrlEnd(url, value, "value");
		Bean bean = Bean.getBean(instance);
		bean.setValue(output, value);
		bean.save();
	}

	RESPONSE doPost(String url, String method, Map<String, String> payload, String beanName, String action) {
		checkUrlEnd(url, action, "action");
		String body = payload.get(BODY);
		if (body == null)
			throw new IllegalArgumentException("no body (app.rest.payload.key: '" + BODY + "') found in http parameters. must be present as payload!");
		Status status = Status.CREATED;
		Object result;
		BeanDefinition def = getType(beanName);
		Object instance = BeanUtil.fromJSON(def.getClazz(), body);
		Bean bean = Bean.getBean(instance);
		checkBean(bean);
		action = action.equals("create") ? "save" : action;
		
		if (bean.getActionByName(action) == null) {
			status = Status.BAD_REQUEST;
			List<String> available = (List<String>) bean.getActions().stream().map(a -> ((IAction)a).getShortDescription())
					.collect(Collectors.toList());
			result = "unknown post action '" + action + "'. available are: " + StringUtil.toString(available, -1); 
		} else {
			result = bean.getActionByName(action).activate();
			if (result != null && !PrimitiveUtil.isPrimitiveOrWrapper(result.getClass())) {
				Bean<Object> resultBean = Bean.getBean(result);
				result = "'" + action + "' successfull!\n\n" + new JSon().serialize(resultBean.toValueMap(null));
			} else if (result == null) {
				result = "'" + action + "' successfull!\n\n" + new JSon().serialize(bean.toValueMap(null));
			}
		}
		LOG.info("REST (POST) " + url + " --> " + StringUtil.toString(result, 80));
		return createResponse(status, String.valueOf(result));
	}

	void checkBean(Bean bean) {
		if (bean.getAttributeNames().length == 0)
			throw new IllegalStateException(bean.toString() + " has no attributes!");
		if (!BeanContainer.instance().isPersistable(bean.getClazz()))
			throw new IllegalStateException(bean.toString() + " is not persistable!");
	}

	String get(String url, String from, String name) {
		String part = StringUtil.substring(url, from + "/", "/", false, false);
		if (part == null)
			throw new IllegalArgumentException("url part '" + name + "' must be present!");
		return part;
	}

	void checkMethod(String method) {
		ManagedException.assertion(method.matches(Methods.matchingExpression()), "http method must match one of {0}", Methods.matchingExpression());
	}

	void checkUrlEnd(String url, String from, String name) {
		String part = StringUtil.substring(url, from, null, true, true);
		if (!Util.isEmpty(part))
			throw new IllegalArgumentException("url must end up with '" + name + "'!");
	}

	BeanDefinition getType(String beanName) {
		BeanDefinition<?> beandef = BeanDefinition.getBeanDefinition(beanName);
		if (beandef.isVirtual() && Util.isEmpty(beandef.getAttributeNames()))
			throw new IllegalArgumentException("beanName is not a known entity!");
		return beandef;
	}

	Collection<Object> getBeans(String beanName, String actionOrAttribute, String value) {
		BeanDefinition type = getType(beanName);
		Object instance = Bean.createInstance(type.getClazz());
		Bean bean = Bean.getBean(instance);
		checkBean(bean);
		if (actionOrAttribute.equals("id") && bean.getAttribute(actionOrAttribute, false) == null)
			actionOrAttribute = bean.getIdAttribute().getName();
		bean.setParsedValue(actionOrAttribute, value);
		return BeanContainer.instance().getBeansByExample(bean.getInstance());
	}

	public static String createDigest(String url, String method, String args) {
		return StringUtil.toHexString(Util.cryptoHash((method + "+" + url + "+" + "+" + LocalDate.now()  + args + "+" + API_KEY).getBytes()));
	}
	
	String printEntities() {
		StringBuilder buf = new StringBuilder(
				  "\n-------- RESTDynamic available entities --- (back: " + ENV.get("service.url") + "/rest)---");
		List<Class> beanTypes = ENV.get("service.loadedBeanTypes", new LinkedList<Class>());
		BeanDefinition<?> beanDef;
		for (Class b : beanTypes) {
			beanDef = BeanDefinition.getBeanDefinition(b);
			buf.append("\n" + beanDef.getName());
			List<IAttributeDefinition<?>> attributes = beanDef.getBeanAttributes();
			for (IAttributeDefinition attr : attributes) {
				buf.append("\n\t" + StringUtil.fixString(attr.getName(), 20) + ": " + attr.getDescription());
			}
			Collection<IAction> actions = beanDef.getActions();
			if (actions.size() > 0) {
				buf.append("\n\tACTIONS");
				for (IAction action : actions) {
					buf.append("\n\t" + StringUtil.fixString(action.getShortDescription(), 20) + ": " + action.getLongDescription());
				}
			}
		}
		buf.append("\n---------------------------------------------------------------------------------\n");
		return buf.toString();
	}

	String printEntitiesJSON() {
		HashMap<Object, Object> entityMap = new HashMap<>();
		HashMap<Object, Object> attributeMap = new HashMap<>();
		HashMap<Object, Object> actionMap = new HashMap<>();

		List<Class> beanTypes = ENV.get("service.loadedBeanTypes", new LinkedList<Class>());
		BeanDefinition<?> beanDef;
		for (Class b : beanTypes) {
			beanDef = BeanDefinition.getBeanDefinition(b);
			attributeMap.clear();
			List<IAttributeDefinition<?>> attributes = beanDef.getBeanAttributes();
			for (IAttributeDefinition attr : attributes) {
				attributeMap.put(attr.getName(), attr.getType());
			}
			entityMap.put(beanDef.getName(), attributeMap);
			
			Collection<IAction> actions = beanDef.getActions();
			for (IAction attr : actions) {
				actionMap.put(attr.getShortDescription(), attr.getLongDescription());
			}
			attributeMap.put("actions", actionMap);
		}
		return MapUtil.toJSon(entityMap);
	}

	protected boolean isBackendRequest(String url) {
		return url.contains(BASE_PATH + "/backend");
	}

	protected RESPONSE doBackendRequest(String url, String method, Map<String, String> payload, Map<String, String> header, String beanName, String actionOrAttribute) {
		String identity = header.get("identity");
		if (identity == null)
			return createResponse(Status.BAD_REQUEST, "no identity given for backend request");
		Status status = Status.OK;
		StringBuilder result = new StringBuilder();
		result.append(evalBackendDataForGivenIdentity(identity, beanName, actionOrAttribute));
		LOG.info("REST (" + method + ") " + url + " --> " + result);
		return createResponse(status, result.toString());
	}

	protected String evalBackendDataForGivenIdentity(String identity, String beanName, String actionOrAttribute) {
		// List<Class> beanTypes = ENV.get("service.loadedBeanTypes", new LinkedList<Class>());
		// BeanDefinition<?> beanDef;
		// for (Class b : beanTypes) {
		// 	beanDef = BeanDefinition.getBeanDefinition(b);
		// }

		BeanDefinit
	}

	protected String printBackendHelp() {
		// TODO: read help/tutorial from md.html file
		String example = "{\n" + //
						"    \"id\": \"mybean\",\n" + //
						"    \"name\": \"MyBean\",\n" + //
						"    \"instance\": {\n" + //
						"        \"id\": \"mybean\",\n" + //
						"        \"name\": \"MyBean\",\n" + //
						"        \"submap\": {\n" + //
						"            \"ki1\": \"vi1\"\n" + //
						"        }\n" + //
						"    },\n" + //
						"    \"valueExpression\": {\n" + //
						"        \"attributeNames\": [\n" + //
						"            \"id\"\n" + //
						"        ],\n" + //
						"        \"comparator\": {},\n" + //
						"        \"expression\": \"{id}\",\n" + //
						"        \"name\": \"{id}\",\n" + //
						"        \"type\": {\n" + //
						"            \"name\": \"java.util.LinkedHashMap\"\n" + //
						"        }\n" + //
						"    },\n" + //
						"    \"attributeDefs\": [\n" + //
						"        {\n" + //
						"            \"name\": \"id\",\n" + //
						"            \"description\": \"mybeanid\",\n" + //
						"            \"constraint\": {\n" + //
						"                \"format\": \"[x00-xFF€]{0,XXX}\",\n" + //
						"                \"length\": -1,\n" + //
						"                \"precision\": -1,\n" + //
						"                \"scale\": -1,\n" + //
						"                \"type\": {\n" + //
						"                    \"name\": \"java.lang.String\"\n" + //
						"                },\n" + //
						"                \"nullable\": true\n" + //
						"            },\n" + //
						"            \"presentation\": {\n" + //
						"                \"description\": \"mybeanid\",\n" + //
						"                \"enabler\": \"AlwaysActive\",\n" + //
						"                \"gridHeight\": 0,\n" + //
						"                \"gridWidth\": 0,\n" + //
						"                \"height\": -1,\n" + //
						"                \"label\": \"Mybeanid\",\n" + //
						"                \"serialversionuid\":-XXX,\n" + //
						"                \"style\": 4,\n" + //
						"                \"type\": 1,\n" + //
						"                \"width\": -1,\n" + //
						"                \"nesting\": false,\n" + //
						"                \"searchable\": true,\n" + //
						"                \"visible\": true\n" + //
						"            }\n" + //
						"        },\n" + //
						"        {\n" + //
						"            \"name\": \"name\",\n" + //
						"            \"description\": \"mybeanname\",\n" + //
						"            \"constraint\": {\n" + //
						"                \"format\": \"[x00-xFF€]{0,XXX}\",\n" + //
						"                \"length\": -1,\n" + //
						"                \"precision\": -1,\n" + //
						"                \"scale\": -1,\n" + //
						"                \"type\": {\n" + //
						"                    \"name\": \"java.lang.String\"\n" + //
						"                },\n" + //
						"                \"nullable\": true\n" + //
						"            },\n" + //
						"            \"presentation\": {\n" + //
						"                \"description\": \"mybeanname\",\n" + //
						"                \"enabler\": \"AlwaysActive\",\n" + //
						"                \"gridHeight\": 0,\n" + //
						"                \"gridWidth\": 0,\n" + //
						"                \"height\": -1,\n" + //
						"                \"label\": \"Mybeanname\",\n" + //
						"                \"serialversionuid\":-XXX,\n" + //
						"                \"style\": 4,\n" + //
						"                \"type\": 1,\n" + //
						"                \"width\": -1,\n" + //
						"                \"nesting\": false,\n" + //
						"                \"searchable\": true,\n" + //
						"                \"visible\": true\n" + //
						"            }\n" + //
						"        },\n" + //
						"        {\n" + //
						"            \"name\": \"submap\",\n" + //
						"            \"description\": \"mybeansubmap\",\n" + //
						"            \"constraint\": {\n" + //
						"                \"format\": {\n" + //
						"                    \"valueExpression\": {\n" + //
						"                        \"comparator\": {},\n" + //
						"                        \"type\": {\n" + //
						"                            \"name\": \"java.lang.Object\"\n" + //
						"                        }\n" + //
						"                    }\n" + //
						"                },\n" + //
						"                \"length\": -1,\n" + //
						"                \"precision\": -1,\n" + //
						"                \"scale\": -1,\n" + //
						"                \"type\": {\n" + //
						"                    \"name\": \"java.util.LinkedHashMap\"\n" + //
						"                },\n" + //
						"                \"nullable\": true\n" + //
						"            },\n" + //
						"            \"presentation\": {\n" + //
						"                \"description\": \"mybeansubmap\",\n" + //
						"                \"enabler\": \"AlwaysActive\",\n" + //
						"                \"gridHeight\": 0,\n" + //
						"                \"gridWidth\": 0,\n" + //
						"                \"height\": -1,\n" + //
						"                \"label\": \"Mybeansubmap\",\n" + //
						"                \"serialversionuid\":-XXX,\n" + //
						"                \"style\": 4,\n" + //
						"                \"type\":XXX,\n" + //
						"                \"width\": -1,\n" + //
						"                \"nesting\": false,\n" + //
						"                \"searchable\": true,\n" + //
						"                \"visible\": true\n" + //
						"            }\n" + //
						"        }\n" + //
						"    ]\n" + //
						"}";
		return 
		"provides responses containing full dynamic beans with presentation+mapped-data.\n" +
		"the h5 framework itself is able to use this backend as base for its html5 presentation instead of\n" +
		"a direct connection to a database through its persistence informations on login\n" +
		"the login persistence informations have to provide the connectionUrl, connectionUser and connectionPassword\n" +
		"to connect and use the data given by such a backend request provider\n\n" +
		"base object is Bean.class with a structure like: " + example;
	}

	String printManual() {
		return 
				  "\n-------------------- RESTDynamic usage informations ------------------------------\n"
				+ "\nREQUEST FORMAT: " + USAGE
				+ "\n\thealth              : simple application health with build informations"
				+ "\n\t(login)             : to login into a session, open: " + ENV.get("service.url")
				+ "\n\tentities            : metainfo as list of all available entities"
				+ "\n\tentitiesjson        : metainfo as json of all available entities"
				+ "\n\t/restui             : instead of 'rest' use 'restui' to do interaction with rest"
				+ "\nGET,PUT,DELETE:"
				+ "\n\tentity              : simple class lower name of entity to be accessed"
				+ "\n\tattribte-or-action  : entities bean attribute name to be accessed"
				+ "\n\tquery               : query value for attribute"
				+ "\n\toptional-output-attr: if only this attribute should be returned"
				+ "\n\tPUT:value           : value to be set on output-attribute"
				+ "\n\texample-1           : GET:/rest/address/city/Buxde*"
				+ "\n\texample-2           : GET:/rest/address/city/Buxdehude/street"
				+ "\n\texample-3           : PUT:/rest/address/id/1/city/Berlin"
				+ "\n\texample-4           : DELETE:/rest/address/id/1"
				+ "\nPOST:"
				+ "\n\tentity              : simple class lower name of entity to be accessed"
				+ "\n\tattribte-or-action  : action 'create' or entity bean action name"
				+ "\n\tPAYLOAD             : 'postData' entry in payolad map - only JSON!"
				+ "\n\texample-1           : POST:/rest/address/create"
				+"\n\nHEADER:"
				+ "\n\tauthorization       : user date digest for basic authentication"
				+ "\n\tuser                : user for method authorization"
				+ "\n\tpassword            : password for method authorization"
				+"\n\nOPTIONS:             : prints this help"
				+ "\n---------------------------------------------------------------------------------\n";
	}
}
