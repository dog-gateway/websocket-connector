package it.polito.elite.dog.communication.ws;

import it.polito.elite.dog.communication.rest.device.api.DeviceRESTApi;
import it.polito.elite.dog.communication.rest.environment.api.EnvironmentRESTApi;
import it.polito.elite.dog.core.library.util.LogHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
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
	private HashMap<String, HashMap<String, ArrayList<String>>> listOfNotificationsPerUser;
	
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
		this.listOfNotificationsPerUser.remove(instance.toString().substring(instance.toString().indexOf("@") + 1));
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
	 *            the id of the device for which we want to subscribe the
	 *            notifications
	 * @param notificationsList
	 *            the list of notification that has to be subscribed
	 */
	public boolean putListOfNotificationsPerControllableAndUser(String clientId, String controllable,
			ArrayList<String> notificationsList)
	{
		HashMap<String, HashMap<String, ArrayList<String>>> listOfNotificationsPerUserBackup = new HashMap<String, HashMap<String, ArrayList<String>>>();
		listOfNotificationsPerUserBackup = this.copyHashMapByValue(this.listOfNotificationsPerUser);
		boolean result = false;
		try
		{
			HashMap<String, ArrayList<String>> existingControllableList = this.listOfNotificationsPerUser.get(clientId);
			result = true;
			if (existingControllableList != null && !existingControllableList.isEmpty())
			{
				// if the user has already subscribed other notifications, we
				// have to
				// copy them with the new one
				// and then we insert the notification required only if it has
				// not
				// already been inserted
				ArrayList<String> existingList = existingControllableList.get(controllable);
				
				// if the user asks to subscribe the notifications for all the
				// devices, it is not necessary to store the name of all the
				// notifications already stored
				if (controllable.equals("all"))
				{
					if (notificationsList.contains("all"))
					{
						existingControllableList.clear();
					}
					else
					{
						// if in the list doesn't exist an "all" element, but
						// exist other elements, we cannot modify it (if for
						// example there is a controllable device with one or
						// more notifications enabled (not all) and we try to
						// submit some notifications for all the devices, it
						// return false)
						ArrayList<String> existingListAll = existingControllableList.get("all");
						if (existingListAll == null || existingListAll.isEmpty())
						{
							return false;
						}
						
					}
				}
				else
				{
					// it is necessary to control if in the list there is
					// already a "all" value: if we are trying to set a value
					// for a single controllable device but there is a "all"
					// value the method return false to say that it is not
					// possible
					if (existingControllableList.get("all") != null)
					{
						return false;
					}
				}
				if (existingList != null)
				{
					if (existingList.contains("all"))
					{
						// it is necessary to control if in the list there is
						// already a "all" value: if we are trying to set a
						// single value but there is a "all" value the method
						// return false to say that it is not possible
						result = false;
						existingList.clear();
						existingList.add("all");
						existingControllableList.put(controllable, existingList);
					}
					else
					{
						for (String notification : notificationsList)
						{
							if (notification.equals("all"))
							{
								// clear the list and add the value "all": if
								// the user ask to check all the notifications
								// it is necessary to delete all the list of
								// notification, otherwise we would have
								// problems to unsubscribe in another moment
								existingList.clear();
								if (!existingList.add(notification))
									result = false;
								break;
							}
							else
							{
								if (!existingList.contains((String) notification))
								{
									if (!(existingList.add(notification)))
										result = false;
								}
							}
						}
						// TODO forse qui devi mettere un
						// if (!existingList.isEmpty())
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
							// clear the list and add the value "all": if the
							// user ask to check all the notifications it is
							// necessary to delete all the list of notification,
							// otherwise we would have problems to unsubscribe
							// in another moment
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
					// TODO forse qui devi mettere un
					// if (!existingList.isEmpty())
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
						// clear the list and add the value "all": if the user
						// ask to check all the notifications it is necessary to
						// delete all the list of notification, otherwise we
						// would have problems to unsubscribe in another moment
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
				// TODO forse qui devi mettere un
				// if (!existingList.isEmpty())
				existingControllableList.put(controllable, newList);
			}
			this.listOfNotificationsPerUser.put(clientId, existingControllableList);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			result = false;
		}
		if (!result)
		{
			this.listOfNotificationsPerUser.clear();
			this.listOfNotificationsPerUser = this.copyHashMapByValue(listOfNotificationsPerUserBackup);
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
	public HashMap<String, ArrayList<String>> getListOfNotificationsAndControllablesPerUser(String clientId)
	{
		return this.listOfNotificationsPerUser.get(clientId);
	}
	
	/**
	 * Get the entire list of all the notifications subscribed
	 * 
	 * @return a {ArrayList<String>} object with all the notifications
	 *         subscribed by a user
	 * 
	 */
	public HashMap<String, HashMap<String, ArrayList<String>>> getListOfNotificationsAndControllables()
	{
		return this.listOfNotificationsPerUser;
	}
	
	/**
	 * Get the entire list of all the notifications subscribed
	 * 
	 * @return a {ArrayList<String>} object with all the notifications
	 *         subscribed by a user
	 * 
	 */
	public void setListOfNotificationsAndControllables(HashMap<String, HashMap<String, ArrayList<String>>> oldList)
	{
		try
		{
			this.listOfNotificationsPerUser = this.copyHashMapByValue(oldList);
		}
		catch (Exception e)
		{
			// if the list is null it has to continue without copying the list
		}
	}
	
	/**
	 * Remove a notification subscribed from the list of the notifications
	 * subscribed by a user
	 * 
	 * @param clientId
	 *            the id of the user (it is the last part of the instance (after
	 *            the @))
	 * @param controllableToRemove
	 *            the id of the device for which we want to unsubscribe the
	 *            notifications
	 * @param notificationToRemove
	 *            the notification that has to be removed
	 */
	public boolean removeNotificationFromListOfNotificationsPerControllableAndUser(String clientId,
			String controllableToRemove, String notificationToRemove)
	{
		boolean result = false;
		try
		{
			HashMap<String, ArrayList<String>> existingControllableList = this.listOfNotificationsPerUser.get(clientId);
			if ((existingControllableList != null) && !existingControllableList.isEmpty())
			{
				ArrayList<String> existingList = existingControllableList.get(controllableToRemove);
				if (existingList != null
						|| ((controllableToRemove.equals("all") && notificationToRemove.equals("all"))))
				{
					// if we want to remove all the notifications of all the
					// controllables we can simply clear all the list
					if ((controllableToRemove.equals("all") && notificationToRemove.equals("all")))
					{
						existingControllableList.clear();
						result = true;
					}
					// if existingList != null and controllableToRemove =
					// "all" it means that in the list of controllables
					// there is only "all", so we can simply remove the
					// value from the list addresses at "all"
					else if (controllableToRemove.equals("all") && (!notificationToRemove.equals("all")))
					{
						result = existingList.remove(notificationToRemove);
						// if the list is empty we remove completly the record
						if (existingList.isEmpty())
							existingControllableList.remove(controllableToRemove);
					}
					// if the controllable to remove is different from "all"
					// but we want to remove all the notifications about it,
					// we can simply remove its list of notifications
					else if ((!controllableToRemove.equals("all")) && notificationToRemove.equals("all"))
					{
						existingControllableList.remove(controllableToRemove);
						// it is not necessary to check if the command did what
						// it would do because there is an exception interceptor
						// at the end of the method that set the result to false
						result = true;
						
					}
					else
					{
						// if both the controllable and the notification to
						// remove are not "all" we have to remove a single
						// value from the list
						if (existingList.contains((String) notificationToRemove))
						{
							existingList.remove(existingList.indexOf((String) notificationToRemove));
							// if the list is empty we remove completly the
							// record
							if (existingList.isEmpty())
								existingControllableList.remove(controllableToRemove);
							// it is not necessary to check if the command did
							// what it would do because there is an exception
							// interceptor at the end of the method that set the
							// result to false
							result = true;
						}
					}
				}
				// if there isn't an "all" value in the list of
				// controllables, but the command says us to remove a
				// particular notification from all the controllables we
				// have to scroll down all the list of controllables
				else if (controllableToRemove.equals("all") && (!notificationToRemove.equals("all")))
				{
					Collection<String> allExistingKeys = existingControllableList.keySet();
					if (!allExistingKeys.isEmpty())
					{
						result = false;
						ArrayList<String> listOfKeysToRemove = new ArrayList<String>();
						//we remove a particular notification from all the controllables
						//if we can remove at least one notification the result is true
						for (String singleKey : allExistingKeys)
						{
							if (existingControllableList.get(singleKey).remove(notificationToRemove))
							{
								result = true;
							}
							// if the list is empty we remove completely the
							// record
							if (existingControllableList.get(singleKey).isEmpty())
							{
								// it is necessary because if I remove an element from
								// the list used in the for an exception will be
								// generated
								listOfKeysToRemove.add(singleKey);
							}
						}
						// remove all the empty lists from the list of
						// notification subscribed
						// it is necessary because if I remove an element from
						// the list used in the for an exception will be
						// generated
						if (!listOfKeysToRemove.isEmpty() && listOfKeysToRemove != null)
						{
							for (String singleKeyToRemove : listOfKeysToRemove)
								existingControllableList.remove(singleKeyToRemove);
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
	
	@SuppressWarnings("unchecked")
	public HashMap<String, HashMap<String, ArrayList<String>>> copyHashMapByValue(
			HashMap<String, HashMap<String, ArrayList<String>>> hashMapToCopy)
	{
		HashMap<String, HashMap<String, ArrayList<String>>> newHashMap = new HashMap<String, HashMap<String, ArrayList<String>>>();
		Collection<String> allExistingKeys = hashMapToCopy.keySet();
		if (!allExistingKeys.isEmpty())
		{
			for (String singleKey : allExistingKeys)
			{
				HashMap<String, ArrayList<String>> newMap = new HashMap<String, ArrayList<String>>();
				Collection<String> allExistingKeysInSecondMap = hashMapToCopy.get(singleKey).keySet();
				if (!allExistingKeysInSecondMap.isEmpty())
				{
					for (String singleKeyInSecondMap : allExistingKeysInSecondMap)
					{
						newMap.put(singleKeyInSecondMap,
								(ArrayList<String>) hashMapToCopy.get(singleKey).get(singleKeyInSecondMap).clone());
					}
					newHashMap.put(singleKey, newMap);
				}
			}
		}
		return newHashMap;
	}
	
}