/*
 * Dog - WebSocket Connector
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

import it.polito.elite.dog.communication.websocket.api.WebSocketConnector;
import it.polito.elite.dog.communication.websocket.info.ConnectedClientInfo;
import it.polito.elite.dog.communication.websocket.info.WebSocketConnectorInfo;
import it.polito.elite.dog.core.library.util.LogHelper;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.http.HttpService;
import org.osgi.service.log.LogService;

/**
 * Main class for the WebSocket Connector. It provide the default implementation
 * of the interface {@link WebSocketConnetor}.
 * 
 * @author <a href="mailto:teo.montanaro@gmail.com">Teodoro Montanaro</a>
 * @author <a href="mailto:luigi.derussis@polito.it">Luigi De Russis</a>
 * @see <a href="http://elite.polito.it">http://elite.polito.it</a>
 * 
 */
public class WebSocketEndPoint extends WebSocketServlet implements ManagedService, WebSocketConnector
{
	// reference for the WebSocket connection
	private WebSocketConnection webSocketConnection;
	// reference for the Http Service
	private AtomicReference<HttpService> http;
	
	// connected applications/clients
	private Map<String, ConnectedClientInfo> connectedClients;
	
	// the bundle context reference
	private BundleContext context;
	
	// the logger
	private LogHelper logger;
	
	// the instance-level mapper
	private ObjectMapper mapper;
	
	// path at which the server will be accessible
	private String webSocketPath;
	
	// list of bundles registered as internal endpoints
	private Set<WebSocketConnectorInfo> registeredEndpoints;
	
	// serial id (the class is serializable)
	private static final long serialVersionUID = 1L;
	
	/**
	 * Constructor
	 */
	public WebSocketEndPoint()
	{
		// init
		this.http = new AtomicReference<HttpService>();
		this.connectedClients = new ConcurrentHashMap<String, ConnectedClientInfo>();
		this.registeredEndpoints = new HashSet<WebSocketConnectorInfo>();
		
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
		// everything is null
		this.context = null;
		this.mapper = null;
		this.logger = null;
	}
	
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
	 * Method used every time a new client try to connect to the server
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jetty.websocket.WebSocketFactory.Acceptor#doWebSocketConnect
	 * (javax.servlet.http.HttpServletRequest, java.lang.String)
	 */
	@Override
	public WebSocket doWebSocketConnect(HttpServletRequest req, String arg1)
	{
		// debug
		this.logger.log(LogService.LOG_DEBUG, "New connection from IP: " + req.getRemoteAddr());
		
		// create a WebSocketConnection
		this.webSocketConnection = new WebSocketConnection(this.context, this);
		
		return this.webSocketConnection;
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
				this.logger.log(LogService.LOG_WARNING, "Error in parsing the required WebSocket path...", e);
			}
		}
		// even if we cannot read the value from the file we instantiate the
		// server because there is a default value
		this.registerHttpServlet();
	}
	
	/**
	 * Register the Http Servlet after acquiring its value, to provide WebSocket
	 * functionalities.
	 */
	private void registerHttpServlet()
	{
		try
		{
			// register a servlet for WebSocket
			this.http.get().registerServlet(this.webSocketPath, this, null, null);
		}
		catch (Exception e)
		{
			// it was not possible to register the servlet
			this.logger.log(LogService.LOG_ERROR, "Impossible to register the servlet", e);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see it.polito.elite.dog.communication.websocket.api.WebSocketConnector#
	 * registerEndpoint(java.lang.Object, java.lang.Object)
	 */
	@Override
	public void registerEndpoint(Object webSocketEndpoint, Object restEndpoint)
	{
		WebSocketConnectorInfo newRegistration = new WebSocketConnectorInfo(webSocketEndpoint, restEndpoint);
		this.registeredEndpoints.add(newRegistration);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see it.polito.elite.dog.communication.websocket.api.WebSocketConnector#
	 * registerEndpoint(java.lang.Object, java.lang.Object, java.lang.String[])
	 */
	@Override
	public void registerEndpoint(Object webSocketEndpoint, Object restEndpoint, String... packages)
	{
		WebSocketConnectorInfo newRegistration = new WebSocketConnectorInfo(webSocketEndpoint.getClass(), restEndpoint,
				webSocketEndpoint, packages);
		this.registeredEndpoints.add(newRegistration);
		
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see it.polito.elite.dog.communication.websocket.api.WebSocketConnector#
	 * getConnectedClients()
	 */
	@Override
	public Set<String> getConnectedClients()
	{
		return this.connectedClients.keySet();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see it.polito.elite.dog.communication.websocket.api.WebSocketConnector#
	 * isWebSocketAvailable()
	 */
	@Override
	public boolean isWebSocketAvailable()
	{
		if (this.webSocketConnection != null)
			return true;
		else
			return false;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see it.polito.elite.dog.communication.websocket.api.WebSocketConnector#
	 * sendMessage(java.lang.String, java.lang.String)
	 */
	@Override
	public void sendMessage(String message, String recipient) throws IOException
	{
		if (message != null && !message.isEmpty() && this.connectedClients.containsKey(recipient))
		{
			this.connectedClients.get(recipient).getConnection().sendMessage(message);
		}
		
	}
	
	/**
	 * Getter for registeredEndpoints
	 * 
	 * @return the list of registeredEndpoints as {@link WebSocketConnectorInfo}
	 */
	public Set<WebSocketConnectorInfo> getRegisteredEndpoints()
	{
		return this.registeredEndpoints;
	}
	
	/**
	 * Add a client to the list of users connected to the server
	 * 
	 * @param instance
	 *            the client ID
	 * @param connection
	 *            the connection object associated to the given client
	 */
	public synchronized void addUser(String instance, Connection connection)
	{
		this.connectedClients.put(instance, new ConnectedClientInfo(instance, connection));
	}
	
	/**
	 * Remove a client from the list of users connected to the server
	 * 
	 * @param instance
	 *            the client ID to remove
	 */
	public synchronized void removeUser(String instance)
	{
		this.connectedClients.remove(instance);
	}
	
}