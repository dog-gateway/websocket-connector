package it.polito.elite.dog.communication.ws;

import it.polito.elite.dog.communication.rest.device.api.DeviceRESTApi;
import it.polito.elite.dog.communication.rest.environment.api.EnvironmentRESTApi;
import it.polito.elite.dog.core.library.util.LogHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketServlet;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.service.log.LogService;

public class WebsocketEndPoint extends WebSocketServlet implements EventHandler
{
	
	// reference for the DeviceRESTApi
	private AtomicReference<DeviceRESTApi> deviceRestApi;
	// reference for the EnvironmentRESTApi
	private AtomicReference<EnvironmentRESTApi> environmentRestApi;
	// reference for the WebsocketImplementation
	private WebsocketImplementation websocketImplementation;
	// list of users (by instances)
	private List<WebsocketImplementation> users = new ArrayList<WebsocketImplementation>();
	// list of notifications per users
	private Map<String, ArrayList<String>> listOfNotificationsPerUser = new HashMap<>();
	
	// the bundle context reference
	private BundleContext context;
	
	// the service logger
	private LogHelper logger;
	
	// the instance-level mapper
	private ObjectMapper mapper;
	
	private static final long serialVersionUID = 1L;
	HttpService http;
	
	public WebsocketEndPoint()
	{
		
		// init the Device Rest Api atomic reference
		this.deviceRestApi = new AtomicReference<>();
		// init the Environment Rest Api atomic reference
		this.environmentRestApi = new AtomicReference<>();
		
	}
	
	/**
	 * Add a user (by its instance) to the list of users connected to the system
	 * 
	 * @param instance
	 *            the instance of WebsocketImplementation dedicated to the user
	 */
	public void addUser(WebsocketImplementation instance)
	{
		this.users.add(instance);
	}
	
	/**
	 * Remove a user (by its instance) to the list of users connected to the
	 * system
	 * 
	 * @param instance
	 *            the instance of WebsocketImplementation dedicated to the user
	 */
	public void removeUser(WebsocketImplementation instance)
	{
		this.users.remove(instance);
	}
	
	/**
	 * Get the list of all users (obtaining their instance)
	 * 
	 * @return a {List<WebsocketImplementation>} object with all the users'
	 *         instance
	 */
	public List<WebsocketImplementation> getUsers()
	{
		return this.users;
	}
	
	/**
	 * Add one or more notification to the list of Notification subscribed by a
	 * user
	 * 
	 * @param clientId
	 *            the id of a user (it is the last part of the instance (after
	 *            the @))
	 * @param notificationsList
	 *            the list of notification that has to be subscribed
	 */
	public void putListOfNotificationsPerUser(String clientId, ArrayList<String> notificationsList)
	{
		ArrayList<String> existingList = this.listOfNotificationsPerUser.get(clientId);
		// if the user has already subscribed other notifications, we have to
		// copy them with the new one
		// and then we insert the notification required only if it has not
		// already been inserted
		if (existingList != null)
		{
			for (String notification : notificationsList)
			{
				if (!existingList.contains((String) notification))
					existingList.add(notification);
			}
		}
		else
			existingList = notificationsList;
		this.listOfNotificationsPerUser.put(clientId, existingList);
	}
	
	/**
	 * Get the list of all the notifications subscribed by a user
	 * 
	 * @param clientId
	 *            the id of a user (it is the last part of the instance (after
	 *            the @))
	 * 
	 * @return a {ArrayList<String>} object with all the notifications
	 *         subscribed by a user
	 * 
	 */
	public ArrayList<String> getListOfNotificationsPerUser(String clientId)
	{
		return this.listOfNotificationsPerUser.get(clientId);
	}
	
	/**
	 * Remove a notification subscribed from the list of the notifications
	 * subscribed by a user
	 * 
	 * @param clientId
	 *            the id of the user (it is the last part of the instance (after
	 *            the @))
	 * @param notificationToRemove
	 *            the notification that has to be removed
	 */
	public void removeListOfNotificationsPerUser(String clientId, String notificationToRemove)
	{
		ArrayList<String> existingList = this.listOfNotificationsPerUser.get(clientId);
		if (existingList != null)
		{
			if (existingList.contains((String) notificationToRemove))
				existingList.remove(existingList.indexOf((String) notificationToRemove));
		}
	}
	
	/**
	 * Bind the DeviceRESTApi service (before the bundle activation)
	 * 
	 * @param deviceRestApi
	 *            the DeviceRestApi service to add
	 */
	public void addedDeviceRESTApi(DeviceRESTApi deviceRestApi)
	{
		// store a reference to the DeviceRESTApi service
		this.deviceRestApi.set(deviceRestApi);
	}
	
	/**
	 * Unbind the DeviceRESTApi service
	 * 
	 * @param deviceRestApi
	 *            the DeviceRESTApi service to remove
	 */
	public void removedDeviceRESTApi(DeviceRESTApi deviceRestApi)
	{
		this.deviceRestApi.compareAndSet(deviceRestApi, null);
	}
	
	/**
	 * Bind the EnvironmentRESTApi service (before the bundle activation)
	 * 
	 * @param environmentRestApi
	 *            the EnvironmentRestApi service to add
	 */
	public void addedEnvironmentRESTApi(EnvironmentRESTApi environmentRestApi)
	{
		// store a reference to the EnvironmentRESTApi service
		this.environmentRestApi.set(environmentRestApi);
	}
	
	/**
	 * Unbind the EnvironmentRESTApi service
	 * 
	 * @param environmentRestApi
	 *            the EnvironmentRESTApi service to remove
	 */
	public void removedEnvironmentRESTApi(EnvironmentRESTApi environmentRestApi)
	{
		this.environmentRestApi.compareAndSet(environmentRestApi, null);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jetty.websocket.WebSocketFactory.Acceptor#doWebSocketConnect(javax.servlet.http.HttpServletRequest, java.lang.String)
	 */
	@Override
	public WebSocket doWebSocketConnect(HttpServletRequest req, String arg1)
	{
		// Method used every time a user try to connect to the server
		this.logger.log(LogService.LOG_INFO, "IP: " + req.getRemoteAddr());
		// System.out.println(req.getPathInfo()); //with this instruction we can
		// obtain the part of the path that follows the part used to answer. If,
		// for example, the server is accessible at the url localhost:8080/chat,
		// and we insert the following path in the browser
		// localhost:8080/chat/bye/path/path2?par=me we obtain "/bye/path/path2"
		// System.out.println(req.getParameter("parameter_name")); //with this
		// instruction we can obtain from the url the content of the parameter
		// named "parameter_name" (the parameters accessible by this instuction
		// have to be specified in the url (for example:
		// localhost:8080/chat?nome_parametro=...&nome2=...)
		
		// create an instance of WebsocketImplementation
		websocketImplementation = new WebsocketImplementation(this.context, this, this.deviceRestApi,
				this.environmentRestApi);
		return websocketImplementation;
	}
	
	/**
	 * Get the classLoader needed to invoke methods It is necessary because the
	 * invocation is possible only from the main class of the package
	 * 
	 * @return a {ClassLoader} object that allows to access all the other
	 *         classes (with their methods)
	 */
	public ClassLoader getClassloader() throws ClassNotFoundException
	{
		return WebsocketEndPoint.class.getClassLoader();
	}
	
	/**
	 * 
	 * @param context
	 */
	public void activate(BundleContext context)
	{
		try
		{
			this.http.registerServlet("/dogws", this, null, null);
			// store the bundle context
			this.context = context;
			
			// initialize the instance-wide object mapper
			this.mapper = new ObjectMapper();
			// set the mapper pretty printing
			this.mapper.enable(SerializationConfig.Feature.INDENT_OUTPUT);
			// avoid empty arrays and null values
			this.mapper.configure(SerializationConfig.Feature.WRITE_EMPTY_JSON_ARRAYS, false);
			this.mapper.setSerializationInclusion(Inclusion.NON_NULL);
			
			// init the logger with a null logger
			this.logger = new LogHelper(this.context);
			
			// log the activation
			this.logger.log(LogService.LOG_INFO, "Activated....");
		}
		catch (ServletException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (NamespaceException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * The Http Service allows other bundles in the OSGi environment to
	 * dynamically register resources and servlets into the URI namespace of
	 * Http Service. A bundle may later unregister its resources or servlets.
	 * This method is called after the registration of the service to store the
	 * instance in our variable
	 * 
	 * @param http
	 *            HttpService
	 */
	public void addHttp(HttpService http)
	{
		this.http = http;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
	 */
	@Override
	public void handleEvent(Event event)
	{
		// method that handle the event generated for notification
		if (websocketImplementation != null && users.size() != 0)
			websocketImplementation.sendNotification(event);
	}
	
}