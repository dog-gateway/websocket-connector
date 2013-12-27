package it.polito.elite.dog.communication.ws;

import it.polito.elite.dog.communication.rest.device.api.DeviceRESTApi;
import it.polito.elite.dog.communication.rest.environment.api.EnvironmentRESTApi;
import it.polito.elite.dog.core.library.util.LogHelper;

import java.util.ArrayList;
import java.util.Dictionary;
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
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.service.log.LogService;

public class WebsocketEndPoint extends WebSocketServlet implements EventHandler, ManagedService
{
	
	// reference for the DeviceRESTApi
	private AtomicReference<DeviceRESTApi> deviceRestApi;
	// reference for the EnvironmentRESTApi
	private AtomicReference<EnvironmentRESTApi> environmentRestApi;
	// reference for the WebsocketImplementation
	private WebsocketImplementation websocketImplementation;
	// list of users (by instances)
	private List<WebsocketImplementation> users;
	// list of notifications per users
	private Map<String, Map<String, ArrayList<String>>> listOfNotificationsPerUser;
	
	// the service registration handle
	private ServiceRegistration<?> serviceRegManagedService;
	
	// the bundle context reference
	private BundleContext context;
	
	// the service logger
	private LogHelper logger;
	
	// the instance-level mapper
	private ObjectMapper mapper;
	
	// path at which the server will be accessible
	private String websocketPath;
	
	private static final long serialVersionUID = 1L;
	private HttpService http;
	
	public WebsocketEndPoint()
	{
		
		// init the Device Rest Api atomic reference
		this.deviceRestApi = new AtomicReference<>();
		// init the Environment Rest Api atomic reference
		this.environmentRestApi = new AtomicReference<>();
		
		// init the list of notifications per users
		this.listOfNotificationsPerUser = new HashMap<>();
		
		// init the list of users (by instances)
		this.users = new ArrayList<WebsocketImplementation>();
		
		// init default value for the path at which the server will be
		// accessible (it is the part that follow server.com:8080)
		this.websocketPath = "/dogws";
		
	}
	
	/**
	 * 
	 * @param context
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
		
		// init the logger with a null logger
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
	 * 
	 * @see
	 * org.eclipse.jetty.websocket.WebSocketFactory.Acceptor#doWebSocketConnect
	 * (javax.servlet.http.HttpServletRequest, java.lang.String)
	 */
	@Override
	public WebSocket doWebSocketConnect(HttpServletRequest req, String arg1)
	{
		// Method used every time a user try to connect to the server
		this.logger.log(LogService.LOG_INFO, "IP: " + req.getRemoteAddr());
		
		// create an instance of WebsocketImplementation
		websocketImplementation = new WebsocketImplementation(this.context, this, this.deviceRestApi,
				this.environmentRestApi);
		return websocketImplementation;
	}
	
	/**
	 * Register the Http Servlet after acquiring its value
	 */
	private void registerHttpServlet()
	{
		try
		{
			this.http.registerServlet(this.websocketPath, this, null, null);
		}
		catch (ServletException | NamespaceException e)
		{
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
	 * @param controllable
	 *            the id of the device for which we want to subscribe the notifications
	 * @param notificationsList
	 *            the list of notification that has to be subscribed
	 */
	public boolean putListOfNotificationsPerControllableAndUser(String clientId, String controllable, ArrayList<String> notificationsList)
	{
		boolean result = false;
		try
		{
			Map<String, ArrayList<String>> existingControllableList = this.listOfNotificationsPerUser.get(clientId);
			if (existingControllableList != null)
			{
				//if the user ask to subscribe the notifications for all the devices, it is not necessary to store the name of all the notifications already stored
				if (controllable.equals("all"))
				{
					existingControllableList.clear();
				}
				else
				{
					//it is necessary to control if in the list there is already a "all" value: if we are trying to set a value for a single controllable device but there is a "all" value the method return false to say that it is not possible
					ArrayList<String> existingList = existingControllableList.get("all");
					if (existingList != null)
						return false;
				}
				// if the user has already subscribed other notifications, we have to
				// copy them with the new one
				// and then we insert the notification required only if it has not
				// already been inserted
				ArrayList<String> existingList = existingControllableList.get(controllable);
				if (existingList != null)
				{
					if (existingList.contains("all"))
					{
						//it is necessary to control if in the list there is already a "all" value: if we are trying to set a single value but there is a "all" value the method return false to say that it is not possible
						return false;
					}
					else
					{
						for (String notification : notificationsList)
						{
							if (notification.equals("all"))
							{
								//clear the list and add the value "all": if the user ask to check all the notifications it is necessary to delete all the list of notification, otherwise we would have problems to unsubscribe in another moment 
								existingList.clear();
								existingList.add(notification);
								break;
							}
							else
							{
								if (!existingList.contains((String) notification))
									existingList.add(notification);
							}
						}
						existingControllableList.put(controllable, existingList);
					}
				}
				else
				{
					existingList = new ArrayList<String>();
					for (String notification : notificationsList)
					{
						if (notification.equals("all"))
						{
							//clear the list and add the value "all": if the user ask to check all the notifications it is necessary to delete all the list of notification, otherwise we would have problems to unsubscribe in another moment 
							existingList.clear();
							existingList.add(notification);
							break;
						}
						else
						{
							if (!existingList.contains((String) notification))
								existingList.add(notification);
						}
					}
					existingControllableList.put(controllable, existingList);
				}
			}
			else
			{
				existingControllableList = new HashMap<String, ArrayList<String>>();
				ArrayList<String> newList = new ArrayList<String>();
				for (String notification : notificationsList)
				{
					if (notification.equals("all"))
					{
						//clear the list and add the value "all": if the user ask to check all the notifications it is necessary to delete all the list of notification, otherwise we would have problems to unsubscribe in another moment 
						newList.clear();
						newList.add(notification);
						break;
					}
					else
					{
						if (!newList.contains((String) notification))
							newList.add(notification);
					}
				}
				existingControllableList.put(controllable, newList);
			}
			this.listOfNotificationsPerUser.put(clientId, existingControllableList);
			result = true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			result = false;
		}
		return result;
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
	public Map<String, ArrayList<String>> getListOfNotificationsAndControllablesPerUser(String clientId)
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
	 * @param controllableToRemove
	 *            the id of the device for which we want to unsubscribe the notifications
	 * @param notificationToRemove
	 *            the notification that has to be removed
	 */
	public boolean removeNotificationsFromListOfNotificationsPerControllableAndUser(String clientId, String controllableToRemove, String notificationToRemove)
	{
		boolean result = false;
		try
		{
			Map<String, ArrayList<String>> existingControllableList = this.listOfNotificationsPerUser.get(clientId);
			if (existingControllableList != null)
			{
				ArrayList<String> existingList = existingControllableList.get(controllableToRemove);
				if (existingList != null)
				{
					if (notificationToRemove.equals("all") && (!controllableToRemove.equals("all")))
					{
						existingControllableList.remove(controllableToRemove);
					}
					else if (notificationToRemove.equals("all") && (controllableToRemove.equals("all")))
					{
						existingControllableList.clear();
					}
					else if (!(notificationToRemove.equals("all")) && (controllableToRemove.equals("all")))
					{
						existingList.clear();
					}
					else
					{
						if (existingList.contains((String) notificationToRemove))
						{
							existingList.remove(existingList.indexOf((String) notificationToRemove));
							result = true;
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			result = false;
		}
		return result;
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
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event
	 * .Event)
	 */
	@Override
	public void handleEvent(Event event)
	{
		// method that handle the event generated for notification
		if (websocketImplementation != null && users.size() != 0)
			websocketImplementation.sendNotification(event);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
	 */
	@Override
	public void updated(Dictionary<String, ?> properties) throws ConfigurationException
	{
		// maybe the received configuration is not for me...
		if (properties != null)
		{
			String websocketPathTemp = "";
			// maybe the reading process from the file could have some troubles
			try
			{
				websocketPathTemp = (String) properties.get("WEBSOCKETPATH");
				if ((!websocketPathTemp.isEmpty()) && (websocketPathTemp != null))
				{
					this.websocketPath = websocketPathTemp;
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		// even if we cannot read the value from the file we instantiate the
		// server because there is a default value
		registerHttpServlet();
		
	}
	
}