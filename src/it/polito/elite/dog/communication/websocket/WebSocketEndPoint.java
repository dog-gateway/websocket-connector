/*
 * Dog - WebSocket Endpoint
 * 
 * Copyright (c) 2013-2014 Teodoro Montanaro and Luigi De Russis
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package it.polito.elite.dog.communication.websocket;

//import it.polito.elite.dog.communication.rest.device.api.DeviceRESTApi;
//import it.polito.elite.dog.communication.rest.environment.api.EnvironmentRESTApi;
//import it.polito.elite.dog.communication.rest.ruleengine.api.RuleEngineRESTApi;
import it.polito.elite.dog.core.library.util.LogHelper;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocket.Connection;
import org.eclipse.jetty.websocket.WebSocketServlet;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.http.HttpService;
import org.osgi.service.log.LogService;

/**
 * 
 * @author <a href="mailto:teo.montanaro@gmail.com">Teodoro Montanaro</a>
 * @author <a href="mailto:luigi.derussis@polito.it">Luigi De Russis</a>
 * @see <a href="http://elite.polito.it">http://elite.polito.it</a>
 * 
 */
public class WebSocketEndPoint extends WebSocketServlet implements ManagedService, WebSocketConnector
{
	// reference for the DeviceRESTApi
	// private AtomicReference<DeviceRESTApi> deviceRestApi;
	// reference for the EnvironmentRESTApi
	// private AtomicReference<EnvironmentRESTApi> environmentRestApi;
	// reference for the RuleEngineRESTApi
	// private AtomicReference<RuleEngineRESTApi> ruleEngineRESTApi;
	// reference for the WebSocketImplementation
	private WebSocketConnection webSocketImplementation;
	// reference for the Http service
	private AtomicReference<HttpService> http;
	
	// list of connected users (by instances)
	// private List<WebSocketConnection> users;
	private Map<String, ConnectedClientInfo> connectedClients;
	
	// list of notifications per users
	// the first key contains the clientId, the second contains the
	// controllable name and the last Array contains the list of notifications
	// subscribed for the specific user and the specific deviceUri
	// private HashMap<String, HashMap<String, ArrayList<String>>>
	// listOfNotificationsPerUser;
	
	// the service registration handle
	private ServiceRegistration<?> serviceRegManagedService;
	
	// the bundle context reference
	private BundleContext context;
	
	// the service logger
	private LogHelper logger;
	
	// the instance-level mapper
	private ObjectMapper mapper;
	
	// path at which the server will be accessible
	private String webSocketPath;
	
	// serial id (the class is serializable)
	private static final long serialVersionUID = 1L;
	
	// TODO extend to handle multiple endpoints
	protected Class<?> registeredEndpoint;
	protected Object restEndpoint;
	protected String[] endpointPackages;
	
	public WebSocketEndPoint()
	{
		// init data structures for referenced services
		// this.deviceRestApi = new AtomicReference<DeviceRESTApi>();
		// this.environmentRestApi = new AtomicReference<EnvironmentRESTApi>();
		// this.ruleEngineRESTApi = new AtomicReference<RuleEngineRESTApi>();
		this.http = new AtomicReference<HttpService>();
		
		// init the list of notifications per users
		// this.listOfNotificationsPerUser = new HashMap<String, HashMap<String,
		// ArrayList<String>>>();
		
		// init the list of users (by instances)
		// this.users = Collections.synchronizedList(new
		// ArrayList<WebSocketConnection>());
		this.connectedClients = new ConcurrentHashMap<String, ConnectedClientInfo>();
		
		// init default value for the path at which the server will be
		// accessible (it is the part that follow server-name.ext:port-number)
		this.webSocketPath = "/dogws";
	}
	
	/**
	 * Bundle activation, stores a reference to the context object passed by the
	 * framework to get access to system data, e.g., installed bundles, etc.
	 * 
	 * @param context
	 *            the OSGi bundle context
	 */
	public void activate(BundleContext context)
	{
		// store the bundle context
		this.context = context;
		
		// initialize the instance-wide object mapper
		this.mapper = new ObjectMapper();
		// set the mapper pretty printing
		this.mapper.enable(SerializationConfig.Feature.INDENT_OUTPUT);
		// avoid empty arrays and null values
		this.mapper.configure(SerializationConfig.Feature.WRITE_EMPTY_JSON_ARRAYS, false);
		this.mapper.setSerializationInclusion(Inclusion.NON_NULL);
		
		// init the logger
		this.logger = new LogHelper(this.context);
		
		// log the activation
		this.logger.log(LogService.LOG_INFO, "Activated....");
	}
	
	/**
	 * Deactivate this component (before its unbind)
	 */
	public void deactivate()
	{
		// unregister the services
		if (this.serviceRegManagedService != null)
		{
			this.serviceRegManagedService.unregister();
		}
		this.serviceRegManagedService = null;
	}
	
	/**
	 * Bind the DeviceRESTApi service (before the bundle activation)
	 * 
	 * @param deviceRestApi
	 *            the DeviceRestApi service to add
	 */
	/*
	 * public void addedDeviceRESTApi(DeviceRESTApi deviceRestApi) { // store a
	 * reference to the DeviceRESTApi service
	 * //this.deviceRestApi.set(deviceRestApi); }
	 */
	
	/**
	 * Unbind the DeviceRESTApi service
	 * 
	 * @param deviceRestApi
	 *            the DeviceRESTApi service to remove
	 */
	/*
	 * public void removedDeviceRESTApi(DeviceRESTApi deviceRestApi) {
	 * //this.deviceRestApi.compareAndSet(deviceRestApi, null); }
	 */
	
	/**
	 * Bind the EnvironmentRESTApi service (before the bundle activation)
	 * 
	 * @param environmentRestApi
	 *            the EnvironmentRestApi service to add
	 */
	/*
	 * public void addedEnvironmentRESTApi(EnvironmentRESTApi
	 * environmentRestApi) { // store a reference to the EnvironmentRESTApi
	 * service //this.environmentRestApi.set(environmentRestApi); }
	 */
	
	/**
	 * Unbind the EnvironmentRESTApi service
	 * 
	 * @param environmentRestApi
	 *            the EnvironmentRESTApi service to remove
	 */
	/*
	 * public void removedEnvironmentRESTApi(EnvironmentRESTApi
	 * environmentRestApi) {
	 * //this.environmentRestApi.compareAndSet(environmentRestApi, null); }
	 */
	
	/**
	 * Bind the RuleEngineRESTApi service (before the bundle activation)
	 * 
	 * @param ruleEngineRESTApi
	 *            the RuleEngineRESTApi service to add
	 */
	
	/*
	 * public void addedRuleEngineRESTApi(RuleEngineRESTApi ruleEngineRESTApi) {
	 * // store a reference to the EnvironmentRESTApi service
	 * //this.ruleEngineRESTApi.set(ruleEngineRESTApi); }
	 * 
	 * /** Unbind the RuleEngineRESTApi service
	 * 
	 * @param ruleEngineRESTApi the RuleEngineRESTApi service to remove
	 */
	/*
	 * public void removedRuleEngineRESTApi(RuleEngineRESTApi ruleEngineRESTApi)
	 * { //this.ruleEngineRESTApi.compareAndSet(ruleEngineRESTApi, null); }
	 */
	
	/**
	 * Bind the Http Service
	 * 
	 * The Http Service allows other bundles in the OSGi environment to
	 * dynamically register resources and servlets into the URI namespace of
	 * Http Service. A bundle may later unregister its resources or servlets.
	 * 
	 * @param http
	 *            the OSGi Http Service
	 */
	public void addedHttpService(HttpService http)
	{
		this.http.set(http);
	}
	
	/**
	 * Unbind the Http Service
	 * 
	 * @param http
	 *            the OSGi Http Service
	 */
	public void removedHttpService(HttpService http)
	{
		this.http.compareAndSet(http, null);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jetty.websocket.WebSocketFactory.Acceptor#doWebSocketConnect
	 * (javax.servlet.http.HttpServletRequest, java.lang.String)
	 */
	@Override
	public WebSocket doWebSocketConnect(HttpServletRequest req, String arg1)
	{
		// Method used every time a user try to connect to the server
		this.logger.log(LogService.LOG_DEBUG, "New connection from IP: " + req.getRemoteAddr());
		
		// create an instance of WebSocketImplementation
		this.webSocketImplementation = new WebSocketConnection(this.context, this/*
																				 * ,
																				 * this
																				 * .
																				 * deviceRestApi
																				 * ,
																				 * this
																				 * .
																				 * environmentRestApi
																				 * ,
																				 * this
																				 * .
																				 * ruleEngineRESTApi
																				 */);
		
		return this.webSocketImplementation;
	}
	
	/**
	 * Register the Http Servlet after acquiring its value
	 */
	private void registerHttpServlet()
	{
		try
		{
			this.http.get().registerServlet(this.webSocketPath, this, null, null);
		}
		catch (Exception e)
		{
			// it was not possible to register the servlet
			this.logger.log(LogService.LOG_ERROR, "Impossible to register the servlet", e);
		}
	}
	
	/**
	 * Add a user (by its instance) to the list of users connected to the system
	 * 
	 * @param instance
	 *            the instance of WebSocketImplementation dedicated to the user
	 */
	public synchronized void addUser(String instance, Connection connection)
	{
		this.connectedClients.put(instance, new ConnectedClientInfo(instance, connection));
	}
	
	/**
	 * Remove a user (by its instance) to the list of users connected to the
	 * system and all its subscribed notifications
	 * 
	 * @param instance
	 *            the instance of WebSocketImplementation dedicated to the user
	 */
	public synchronized void removeUser(String instance)
	{
		// we remove the user from the list of users
		this.connectedClients.remove(instance);
		// remove all the notifications subscribed by the user (we take the
		// userId from instance: we take the last part of the instance: the part
		// that is after the @)
		// String stringifyInstance = instance.toString();
		// this.listOfNotificationsPerUser.remove(stringifyInstance.substring(stringifyInstance.indexOf("@")
		// + 1));
		
	}

	/**
	 * Get the classLoader needed to invoke methods
	 * 
	 * It is necessary because the invocation is possible only from the main
	 * class of the package
	 * 
	 * @return a {ClassLoader} object that allows to access all the other
	 *         classes (with their methods)
	 */
	public ClassLoader getClassloader() throws ClassNotFoundException
	{
		return WebSocketEndPoint.class.getClassLoader();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
	 */
	@Override
	public void updated(Dictionary<String, ?> properties) throws ConfigurationException
	{
		// get the parameters from the configuration file
		// maybe the received configuration is not for me...
		if (properties != null)
		{
			String temporaryPath = "";
			// maybe the reading process from the file could have some troubles
			try
			{
				temporaryPath = (String) properties.get("WEBSOCKETPATH");
				if ((!temporaryPath.isEmpty()) && (temporaryPath != null))
				{
					this.webSocketPath = temporaryPath;
				}
			}
			catch (Exception e)
			{
				this.logger.log(LogService.LOG_WARNING, "Error in parsing the required WebSocket Path...", e);
			}
		}
		// even if we cannot read the value from the file we instantiate the
		// server because there is a default value
		this.registerHttpServlet();
		
	}
	
	@Override
	public void registerEndpoint(Class<?> webSocketEndpoint, Object restEndpoint, String... packages)
	{
		this.registeredEndpoint = webSocketEndpoint;
		this.restEndpoint = restEndpoint;
		this.endpointPackages = packages;
		
	}
	
	@Override
	public Map<String, ConnectedClientInfo> getConnectedClients()
	{
		return this.connectedClients;
	}
	
	@Override
	public boolean isWebSocketAvailable()
	{
		if (this.webSocketImplementation != null)
			return true;
		else
			return false;
	}

	@Override
	public void sendMessage(String message, String recipient) throws IOException
	{
		if(message!=null && !message.isEmpty() && this.connectedClients.containsKey(recipient))
		{
			this.connectedClients.get(recipient).getConnection().sendMessage(message);
		}
		
	}
	
}