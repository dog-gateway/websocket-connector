package it.polito.elite.dog.communication.ws;

import it.polito.elite.dog.communication.rest.device.api.DeviceRESTApi;
import it.polito.elite.dog.communication.rest.environment.api.EnvironmentRESTApi;
import it.polito.elite.dog.communication.ws.message.WebsocketJsonInvocationResult;
import it.polito.elite.dog.communication.ws.message.WebsocketJsonNotification;
import it.polito.elite.dog.communication.ws.message.WebsocketJsonRequest;
import it.polito.elite.dog.communication.ws.message.WebsocketJsonResponse;
import it.polito.elite.dog.core.library.model.notification.Notification;
import it.polito.elite.dog.core.library.util.LogHelper;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.codehaus.jackson.node.ArrayNode;
import org.eclipse.jetty.websocket.WebSocket;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.service.log.LogService;

import javax.measure.Measure;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

public class WebsocketImplementation implements WebSocket.OnTextMessage
{
	private WebsocketImplementation connectionInstance;
	// number of initial parameters in the endPoint (ex uri or path) that
	// indicates the class in which the requested action will be performed
	// (devices, environment, rules, ...)
	int numberOfClassParameters;
	// initial parameters in the endPoint (ex uri or path) that indicate the
	// class in which the requested action will be performed (devices,
	// environment, rules, ...)
	String[] endPointParts;
	private WebsocketEndPoint websocketEndPoint;
	
	// connection used to send and receive messages
	private Connection connection;
	
	// reference for the DeviceRESTApi
	private AtomicReference<DeviceRESTApi> deviceRESTApi;
	// reference for the EnvironmentRESTApi
	private AtomicReference<EnvironmentRESTApi> environmentRESTApi;
	
	// the service logger
	private LogHelper logger;
	
	// the bundle context reference
	private BundleContext context;
	
	// the instance-level mapper
	private ObjectMapper mapper;
	
	// variable used to store the clientId for the notification registration
	private String clientIdForRegistration;
	
	// variable used to store the type of received message for the notification
	// registration
	private String typeForRegistration;
	
	public WebsocketImplementation(BundleContext context, WebsocketEndPoint websocketEndPoint,
			AtomicReference<DeviceRESTApi> deviceRESTApi, AtomicReference<EnvironmentRESTApi> environmentRESTApi)
	{
		this.websocketEndPoint = websocketEndPoint;
		// init the Device Rest Api atomic reference
		this.deviceRESTApi = deviceRESTApi;
		// init the Environment Rest Api atomic reference
		this.environmentRESTApi = environmentRESTApi;
		
		// init the variable used to store the type of received message for the
		// notification registration
		this.typeForRegistration = "";
		
		// init the variable used to store the clientId for the notification
		// registration
		this.clientIdForRegistration = "";
		
		this.numberOfClassParameters = 0;
		
		// store the bundle context
		this.context = context;
		this.connectionInstance = this;
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
	 * method called when the user close the connection
	 * 
	 */
	@Override
	public void onClose(int arg0, String arg1)
	{
		// if the connection is closed we remove the user from the list of users
		// connected to the server
		websocketEndPoint.removeUser(this);
	}
	
	/**
	 * method called when the user open the connection
	 * 
	 * @param connection
	 * 
	 */
	@Override
	public void onOpen(Connection connection)
	{
	    connection.setMaxIdleTime(Integer.MAX_VALUE);
		// init the connection
		this.connection = connection;

		// add the user to the list of users connected to the system
		websocketEndPoint.addUser(this);
		this.logger.log(LogService.LOG_INFO, "Connection Protocol: " + connection.getProtocol());
		if (connection.isOpen())
		{
			try
			{
				// extract the userId from the instance name (by taking the last
				// part of the connection instance (after the @))
				String userId = this.connectionInstance.toString().substring(
						this.connectionInstance.toString().indexOf("@") + 1);
				// send the starting message (presentation) with the clientId
				this.connection.sendMessage("{ \"clientId\": \"" + userId
						+ "\",\"messageType\":\"info\",\"type\":\"presentation\" }");
			}
			catch (IOException e)
			{
				e.printStackTrace();
				// if something goes wrong we remove the user from the list of users
				// connected to the server
				websocketEndPoint.removeUser(this);
			}
		}
	}
	
	/**
	 * method called when the user send a message to the server
	 * 
	 * @param data
	 *            Received message
	 * 
	 */
	@Override
	public void onMessage(String data)
	{
		if (!data.isEmpty())
		{
			WebsocketJsonRequest websocketReceivedData; // used to parse all the
														// Json datas
			try
			{
				// parse Json code received
				websocketReceivedData = this.mapper.readValue(data, WebsocketJsonRequest.class);
				this.logger.log(LogService.LOG_INFO, "Received data: " + websocketReceivedData.toString());
				String clientId = websocketReceivedData.getClientId();
				String sequenceNumber = websocketReceivedData.getSequenceNumber();
				String messageType = websocketReceivedData.getMessageType();
				String type = websocketReceivedData.getType();
				String action = websocketReceivedData.getAction();
				String endPoint = websocketReceivedData.getEndPoint();
				String parameters = websocketReceivedData.getParameters();
				// check if the user is the right one and if the message is a
				// request
				if ((clientId != null)
						&& clientId.equals(this.connectionInstance.toString().substring(
								this.connectionInstance.toString().indexOf("@") + 1))
						&& (messageType.equals("request")))
				{
					// if it is a notification registration we have to call the
					// method to register a notification (or a list of
					// notifications)
					// the parameters could be a list of string
					// ["TemperatureMeasurementNotification",
					// "AlertNotification", "BatteryLevelNotification"]
					// or a simple string : "TemperatureMeasurementNotification"
					// but here we set only the clientId that we will use in the
					// invoked method
					if (type != null && type.toLowerCase().contains("notification"))
					{
						// set the clientId for the registration
						this.clientIdForRegistration = clientId;
						// set the type for the registration
						this.typeForRegistration = type;
						// set to 0 the variable by which we usually count the
						// initial parts of path that indicates the class
						this.numberOfClassParameters = 0;
					}
					// it is not a registration or unregistration request
					// for notifications
					String result;
					try
					{
						// obtain the requested information from the method
						// that invoke the right method by Path annotation
						result = this.invokeMethodByAnnotation(endPoint, action, parameters);
						this.logger.log(LogService.LOG_INFO, "Sending data: " + result);
						// transform the result in a Json message
						WebsocketJsonResponse jsonResponse = new WebsocketJsonResponse();
						if (!clientId.isEmpty())
							jsonResponse.setClientId(clientId);
						if (!sequenceNumber.isEmpty())
							jsonResponse.setSequenceNumber(sequenceNumber);
						jsonResponse.setMessageType("response");
						if (!action.isEmpty())
							jsonResponse.setAction(action);
						if (!endPoint.isEmpty())
							jsonResponse.setEndPoint(endPoint);
						if (result.isEmpty())
						{
							WebsocketJsonInvocationResult jsonResult = new WebsocketJsonInvocationResult();
							jsonResult.setResult("Something went wrong");
							// TODO controllare che non possano esserci risposte vuote
							result = this.mapper.writeValueAsString(jsonResult);
						}
						Object resultObject;
						// if the result is not a json object the readTree
						// method generates an exception that we intercept to
						// insert the result as string in the answer
						try
						{
							resultObject = this.mapper.readTree(result);
						}
						catch (Exception e)
						{
							WebsocketJsonInvocationResult jsonResult = new WebsocketJsonInvocationResult();
							jsonResult.setResult(result);
							result = this.mapper.writeValueAsString(jsonResult);
							resultObject = this.mapper.readTree(result);
						}
						jsonResponse.setResponse(resultObject);
						
						String response = this.mapper.writeValueAsString(jsonResponse);
						// send the message just created
						this.connectionInstance.connection.sendMessage(response);
					}
					catch (ClassNotFoundException | SecurityException | NoSuchMethodException
							| IllegalArgumentException | InstantiationException | IllegalAccessException
							| InvocationTargetException e)
					{
						e.printStackTrace();
					}
				}
				else
				{
					// the request has not the right parameters (clientId and
					// messageType)
					// the error message is sent in json format
					WebsocketJsonResponse jsonResponse = new WebsocketJsonResponse();
					if (!clientId.isEmpty())
						jsonResponse.setClientId(clientId);
					if (!sequenceNumber.isEmpty())
						jsonResponse.setSequenceNumber(sequenceNumber);
					jsonResponse.setMessageType("info");
					jsonResponse.setResponse("You forgot to send the clientId or the message type is not request");
					String response = this.mapper.writeValueAsString(jsonResponse);
					this.connectionInstance.connection.sendMessage(response);
				}
			}
			catch (IOException e1)
			{
				e1.printStackTrace();
			}
		}
	}
	
	/**
	 * Send the notification in Json format
	 * 
	 * @param notificationEvent
	 *            Event generated to handle the notification
	 */
	@SuppressWarnings("unchecked")
	public void sendNotification(Event notificationEvent)
	{
		// by the method "getTopic()" we can obtain the name of the notification
		String[] topicParts = notificationEvent.getTopic().split("/");
		String eventName = topicParts[topicParts.length - 1];
		// create the variable that will contain all the fields contained in the
		// notification (deviceUi, notificationTopic ...)
		HashMap<String, String> notificationContent = new HashMap<String, String>();
		// by the method "getProperty()" we can obtain information necessary
		// about the notifications
		Object eventContent = notificationEvent.getProperty("event");
		if (eventContent != null)
		{
			// we can received a single notification or more than one
			// notification so we create a list in every case (containing one
			// ore more notifications)
			ArrayList<Notification> notificationList = new ArrayList<Notification>();
			if (eventContent instanceof HashSet)
				notificationList.addAll((Collection<? extends Notification>) eventContent);
			else
				notificationList.add((Notification) eventContent);
			// we scroll through all items of the list of notifications received
			for (Notification singleNotification : notificationList)
			{
				WebsocketJsonNotification notificationResponse = new WebsocketJsonNotification();
				// cause the notification could contain a lot of fields (that we
				// cannot know in advance) we have to scroll trough all of them
				// stroring them in an hashmap (notificationContent)
				for (Field notificationField : singleNotification.getClass().getDeclaredFields())
				{
					notificationField.setAccessible(true);
					String notificationFieldName = notificationField.getName();
					Object notificationFieldValue = null;
					try
					{
						String notificationFieldValueFinal = "";
						notificationFieldValue = notificationField.get(singleNotification);
						// the content could be only a measure or a string,
						// because if we want to send more notifications than
						// only one, we have to send a packet with an array of
						// single notification
						if (notificationFieldValue instanceof Measure<?, ?>)
							notificationFieldValueFinal = notificationFieldValue.toString();
						else if (notificationFieldValue instanceof String)
							notificationFieldValueFinal = (String) notificationFieldValue;
						// we decided to use the notificationTopic to know the
						// content of the notification, but the received
						// notificationTopic contains more information then the
						// ones we need (it/polito/elite/...)
						if (notificationFieldName.equals("notificationTopic"))
						{
							String[] fieldValueFinalParts = notificationFieldValueFinal.split("/");
							notificationFieldValueFinal = fieldValueFinalParts[fieldValueFinalParts.length - 1];
						}
						// insert the information acquired in the list
						notificationContent.put(notificationFieldName, notificationFieldValueFinal);
					}
					catch (IllegalArgumentException | IllegalAccessException e)
					{
						e.printStackTrace();
					}
				}
				// the Event Handler is executed only once (on the last
				// instance), so it is important to do the following things for
				// all the users
				for (WebsocketImplementation user : websocketEndPoint.getUsers())
				{
					if (!notificationContent.isEmpty())
					{
						try
						{
							// here we send the notification only to the users
							// that have submitted to receive the kind of
							// notification just received for the device that
							// sends it
							Map<String, ArrayList<String>> listOfControllable = new HashMap<String, ArrayList<String>>();
							try
							{
								listOfControllable.putAll(websocketEndPoint.getListOfNotificationsAndControllablesPerUser(user
										.toString().substring(user.toString().indexOf("@") + 1)));
							}
							catch (Exception e)
							{
								// if the list is null it has to continue without copying the list
							}
							if (listOfControllable != null && (!notificationContent.get("deviceUri").isEmpty()))
							{
								ArrayList<String> listOfNotification = listOfControllable
										.get((String) notificationContent.get("deviceUri"));
								if (listOfNotification == null)
								{
									listOfNotification = listOfControllable.get("all");
								}
								if (listOfNotification != null
										&& (listOfNotification.contains(eventName.toString()) || listOfNotification
												.contains("all")))
								{
									// transform the notification in Json
									// format,
									// with clienId, messageType, type
									notificationResponse.setNotification(notificationContent);
									notificationResponse.setClientId(user.toString().substring(
											user.toString().indexOf("@") + 1));
									notificationResponse.setMessageType("info");
									notificationResponse.setType("notification");
									String notificationToSend = this.mapper.writeValueAsString(notificationResponse);
									user.connection.sendMessage(notificationToSend);
								}
							}
						}
						catch (IOException e)
						{
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
	
	/**
	 * Unregister from a notification
	 * 
	 * @param clientId
	 *            Id of the client that is requiring the unsubscription
	 * 
	 * @param notifications
	 *            List of notifications from which the user want to be
	 *            unsubscribed
	 */
	public String notificationUnregistration(String clientId, String controllable, Object notificationsAcquired)
			throws JsonParseException, JsonMappingException
	{
		Object notifications;
		try
		{
			notifications = this.mapper.readTree((String) notificationsAcquired);
		}
		catch (Exception e)
		{
			notifications = notificationsAcquired;
		}
		String result = "Unregistration failed";
		String multipleUnregistrationResult ="";
		// The notifications could be a simple string (only one notification) or
		// a list of element
		if (notifications instanceof String)
		{
			// if we receive only one single notification we can call directly
			// the method that does the unregistration
			if (websocketEndPoint.removeNotificationFromListOfNotificationsPerControllableAndUser(clientId,
					controllable, (String) notifications))
				result = "Unregistration completed successfully";
		}
		else if (notifications instanceof ArrayNode)
		{
			// scroll through all the items received and for each of them we
			// call the method that does the unregistration
			ArrayNode notificationsArrayNode = (ArrayNode) notifications;
			Iterator<JsonNode> iterator = notificationsArrayNode.getElements();
			ArrayList<String> failedUnregistrations = new ArrayList<String>();
			ArrayList<String> succededUnregistrations = new ArrayList<String>();
			result ="";
			while (iterator.hasNext())
			{
				JsonNode current = iterator.next();
				if (!websocketEndPoint.removeNotificationFromListOfNotificationsPerControllableAndUser(clientId,
						controllable, (String) current.getTextValue()))
				{
					failedUnregistrations.add((String) current.getTextValue());
				}
				else
				{
					succededUnregistrations.add((String) current.getTextValue());
				}
			}

			WebsocketJsonInvocationResult jsonResult = new WebsocketJsonInvocationResult();
			if (!failedUnregistrations.isEmpty() || !succededUnregistrations.isEmpty())
			{
				if (!failedUnregistrations.isEmpty())
					jsonResult.setFailedUnregistrations(failedUnregistrations);
				if (!succededUnregistrations.isEmpty())
					jsonResult.setSuccededUnregistrations(succededUnregistrations);

				try
				{
					multipleUnregistrationResult = this.mapper.writeValueAsString(jsonResult);
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
			else
			{
				result = "Unregistration failed";
			}
			
		}
		else
		{
			// if the notification list is empty the user wants to unsubscribe
			// all the notifications
			if (websocketEndPoint.removeNotificationFromListOfNotificationsPerControllableAndUser(clientId,
					controllable, "all"))
				result = "Unregistration completed successfully";
		}
		
		try
		{
			if (!result.isEmpty())
			{
				//the user asked to remove only one notification
				WebsocketJsonInvocationResult jsonResult = new WebsocketJsonInvocationResult();
				jsonResult.setResult(result);
				return this.mapper.writeValueAsString(jsonResult);
			}
			else
			{
				Object unregistrationResults;
				// if the multipleUnregistrationResult is not a json object the readTree method generates an exception that we intercept to insert the result as string in the answer
				try
				{
					unregistrationResults = this.mapper.readTree(multipleUnregistrationResult);
					WebsocketJsonInvocationResult jsonResult = new WebsocketJsonInvocationResult();
					jsonResult.setUnregistrationResults(unregistrationResults);
					return this.mapper.writeValueAsString(jsonResult);
				}
				catch (Exception e)
				{
					WebsocketJsonInvocationResult jsonResult = new WebsocketJsonInvocationResult();
					jsonResult.setResult(multipleUnregistrationResult);
					return this.mapper.writeValueAsString(jsonResult);
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return "{\"result\":\"" + result + "\"}";
		}
	}
	
	@PUT
	@Path("/api/devices/notifications")
	public String notificationRegistrationWithoutControllable(Object notifications) throws JsonParseException,
			JsonMappingException
	{
		if (this.typeForRegistration.toLowerCase().contains("notificationregistration"))
		{
			return this.notificationRegistration(this.clientIdForRegistration, "all", notifications);
		}
		if (this.typeForRegistration.toLowerCase().contains("notificationunregistration"))
		{
			return this.notificationUnregistration(this.clientIdForRegistration, "all", notifications);
		}
		
		try
		{
			WebsocketJsonInvocationResult jsonResult = new WebsocketJsonInvocationResult();
			jsonResult
					.setResult("The command sent was not a valid command: you forgot (or wrote a wrong one) the type of message (notificationRegistration or notificationUnregistration)");
			return this.mapper.writeValueAsString(jsonResult);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return "{\"result\":\"The command sent was not a valid command: you forgot (or wrote a wrong one) the type of message (notificationRegistration or notificationUnregistration)\"}";
		}
	}
	
	@PUT
	@Path("/api/devices/{controllable}/notifications")
	public String notificationRegistrationWithControllable(@PathParam("controllable") String controllable,
			Object notifications) throws JsonParseException, JsonMappingException
	{
		if (this.typeForRegistration.toLowerCase().contains("notificationregistration"))
		{
			return this.notificationRegistration(this.clientIdForRegistration, controllable, notifications);
		}
		if (this.typeForRegistration.toLowerCase().contains("notificationunregistration"))
		{
			return this.notificationUnregistration(this.clientIdForRegistration, controllable, notifications);
		}
		
		try
		{
			WebsocketJsonInvocationResult jsonResult = new WebsocketJsonInvocationResult();
			jsonResult
					.setResult("The command sent was not a valid command: you forgot (or wrote a wrong one) the type of message (notificationRegistration or notificationUnregistration)");
			return this.mapper.writeValueAsString(jsonResult);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return "{\"result\":\"The command sent was not a valid command: you forgot (or wrote a wrong one) the type of message (notificationRegistration or notificationUnregistration)\"}";
		}
	}
	
	/**
	 * Register from a notification
	 * 
	 * @param clientId
	 *            Id of the client that is requiring the subscription
	 * @param controllable
	 *            the id of the device for which we want to subscribe the
	 *            notifications
	 * @param notifications
	 *            List of notifications from which the user want to be
	 *            subscribed
	 */
	public String notificationRegistration(String clientId, String controllable, Object notificationsAcquired)
			throws JsonParseException, JsonMappingException
	{
		String result = "Registration failed";
		Object notifications;
		try
		{
			notifications = this.mapper.readTree((String) notificationsAcquired);
		}
		catch (Exception e)
		{
			notifications = notificationsAcquired;
		}
		try
		{
			// list of notification that has to be subscribed
			ArrayList<String> notificationsList = new ArrayList<String>();
			// we insert each notification only once, so if the user send the
			// same
			// notification name twice (or more) we insert only one
			if (notifications instanceof String)
			{
				// if we receive only one single notification we can add it
				// directly
				// to the list of notifications
				// but we do it only if the user has never subscribed the
				// notification just received
				if (!notificationsList.contains((String) notifications))
					notificationsList.add((String) notifications);
				
			}
			else if (notifications instanceof ArrayNode)
			{
				// scroll through all the items received and for each of them,
				// if
				// the user has never subscribed it, we add the specific
				// notification to the list of notifications
				ArrayNode notificationsArrayNode = (ArrayNode) notifications;
				Iterator<JsonNode> iterator = notificationsArrayNode.getElements();
				while (iterator.hasNext())
				{
					JsonNode current = iterator.next();
					if (!notificationsList.contains((String) current.getTextValue()))
						notificationsList.add(current.getTextValue());
				}
			}
			// if the list is empty it means that the user wants to subscribe
			// all the notifications
			if (notificationsList.isEmpty())
			{
				notificationsList.add("all");
			}
			// at the end of the process that chooses which notifications have
			// to be
			// subscribed we can call the method that does the real subscription
			if (websocketEndPoint.putListOfNotificationsPerControllableAndUser(clientId, controllable,
					notificationsList))
				result = "Registration completed successfully";
		}
		catch (Exception e)
		{
			e.printStackTrace();
			result = "Registration failed";
		}
		
		try
		{
			WebsocketJsonInvocationResult jsonResult = new WebsocketJsonInvocationResult();
			jsonResult.setResult(result);
			return this.mapper.writeValueAsString(jsonResult);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return "{\"result\":\"" + result + "\"}";
		}
		
	}
	
	/**
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws IllegalArgumentException
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * 
	 *             Invoke the right method from the right class (DeviceRESTApi
	 *             or EnvironmentRESTApi)
	 * 
	 * @param endPoint
	 *            Path specified as annotation on the method that has we want to
	 *            invoke
	 * 
	 * @param action
	 *            Action (GET, POST, PUT, DELETE) that is specified as
	 *            annotation on the method that we want to invoke
	 * 
	 * @param parameters
	 *            parameters that has to be passed to the method that we want to
	 *            invoke
	 * 
	 */
	public String invokeMethodByAnnotation(String endPoint, String action, String parameters) throws IOException,
			ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException,
			InstantiationException, IllegalAccessException, InvocationTargetException
	{
		
		// first of all we choose the class that contains the method we want to
		// invoke
		Class<?> clazz = null;
		
		// then we divide the endPoint in different parts (in the
		// path are separated by "/")
		
		if (endPoint.startsWith("/"))
		{
			endPoint = endPoint.replaceFirst("/", "");
		}
		this.endPointParts = endPoint.split("/");
		
		if (this.typeForRegistration.contains("notification"))
		{
			clazz = this.getClass();
		}
		else
		{
			ClassLoader cls = this.websocketEndPoint.getClassloader();
			clazz = this.getRightClass(endPoint, cls);
		}
		String result = "";
		
		if (clazz != null)
		{
			// now we scroll trough all methods available in the class looking
			// for
			// the annotations present
			// if there are both "/api/devides/status" and
			// "/api/devices/{device-id}" we have to take care to choose the
			// right
			// one
			Method[] methods = clazz.getDeclaredMethods();
			Method rightMethod = null;
			List<String> rightArguments = new ArrayList<String>();
			for (Method method : methods)
			{
				// we start from the point to which the getRightClass arrived
				// (for
				// example after api/devices/)
				int numberOfAnalizedPartsOfEndPoint = this.numberOfClassParameters;
				// first of all we check if the analyzed method has the right
				// action
				// defined (GET, PUT, POST or DELETE)
				if (method.isAnnotationPresent(GET.class) || method.isAnnotationPresent(PUT.class)
						|| method.isAnnotationPresent(POST.class) || method.isAnnotationPresent(DELETE.class))
				{
					String actionAnnotation = "";
					if (method.isAnnotationPresent(GET.class))
					{
						GET tempAnnotation = (GET) method.getAnnotation(GET.class);
						actionAnnotation = tempAnnotation.annotationType().getSimpleName();
					}
					else if (method.isAnnotationPresent(POST.class))
					{
						POST tempAnnotation = (POST) method.getAnnotation(POST.class);
						actionAnnotation = tempAnnotation.annotationType().getSimpleName();
					}
					else if (method.isAnnotationPresent(PUT.class))
					{
						PUT tempAnnotation = (PUT) method.getAnnotation(PUT.class);
						actionAnnotation = tempAnnotation.annotationType().getSimpleName();
					}
					else if (method.isAnnotationPresent(DELETE.class))
					{
						DELETE tempAnnotation = (DELETE) method.getAnnotation(DELETE.class);
						actionAnnotation = tempAnnotation.annotationType().getSimpleName();
					}
					// if the action is the right one we can check which is the
					// right method (with the right annotation)
					if (actionAnnotation.isEmpty() || actionAnnotation.equals(action))
					{
						// here we acquire the annotation defined for the method
						// analyzed
						if (method.isAnnotationPresent(Path.class))
						{
							Path tempPathAnnotation = (Path) method.getAnnotation(Path.class);
							String methodAnnotation = tempPathAnnotation.value().toString();
							// if there is a "/" at the beginning of the path we
							// will remove it
							if (methodAnnotation.startsWith("/"))
							{
								methodAnnotation = methodAnnotation.replaceFirst("/", "");
							}
							String[] methodAnnotationParts = methodAnnotation.split("/");
							List<String> arguments = new ArrayList<String>();
							// here we check if the method analyzed and the
							// method
							// we are looking for have the same number of
							// annotation
							// parts
							if (methodAnnotationParts.length == (this.endPointParts.length - numberOfAnalizedPartsOfEndPoint))
							{
								for (int k = 0; k < (this.endPointParts.length - this.numberOfClassParameters); k++)
								{
									// if there are both "/api/devides/status"
									// and
									// "/api/devices/{device-id}" we have to
									// take
									// care to choose the right one
									if (methodAnnotationParts[k]
											.compareTo(endPointParts[numberOfAnalizedPartsOfEndPoint]) != 0)
									{
										// if I have already choose a method it
										// means that a method without a
										// parameter
										// exists (because in the other case I
										// say
										// that it is the right method only at
										// the
										// end (when I've analyzed all the
										// parameters))
										if (rightMethod == null)
										{
											// if "{" or "}" is present it means
											// that it is a parameter
											// So we insert it in the list of
											// arguments (we need the parameters
											// for
											// the invoke method)
											if ((methodAnnotationParts[k].indexOf("{") != -1)
													&& (methodAnnotationParts[k].indexOf("}") != -1))
											{
												arguments.add(new String(
														this.endPointParts[numberOfAnalizedPartsOfEndPoint]));
											}
											else
											{
												break;
											}
										}
										else
											// if a method has already chosen
											// and we
											// are here it would means that it
											// is
											// the wrong method or that we have
											// already chosen the method without
											// parameters (/api/devices/status
											// instead of di
											// /api/devices/{devide-id})
											break;
									}
									numberOfAnalizedPartsOfEndPoint++;
								}
								if (numberOfAnalizedPartsOfEndPoint == (this.endPointParts.length))
								{
									// if we arrived at the end of Path and the
									// dimension of endPoint is equal to the
									// dimension of annotation it may be the
									// right
									// method
									// but we have to check if the output of the
									// method is the right one (Json) (if there
									// is
									// not a "Produces" annotation we assumes
									// that
									// it produces Json)
									if (method.isAnnotationPresent(Produces.class))
									{
										Produces tempAnnotation = (Produces) method.getAnnotation(Produces.class);
										methodAnnotation = tempAnnotation.value()[0];
										if (methodAnnotation.compareTo(MediaType.APPLICATION_JSON) == 0)
										{
											rightMethod = method;
											rightArguments = arguments;
										}
									}
									else
									{
										rightMethod = method;
										rightArguments = arguments;
									}
								}
							}
						}
						else
						{
							// if the endPoint has not other parts after the
							// path
							// used to select the class (/api/devices or
							// /api/environment) it means that the method we are
							// looking for has to answer to path /api/devices or
							// /api/environment
							if ((this.endPointParts.length - numberOfAnalizedPartsOfEndPoint) == 0)
							{
								// we have to check if the output of the method
								// is
								// the right one (Json) (if there is not a
								// "Produces" annotation we assumes that it
								// produces
								// Json)
								if (method.isAnnotationPresent(Produces.class))
								{
									Produces tempAnnotation = (Produces) method.getAnnotation(Produces.class);
									String methodAnnotation = tempAnnotation.value()[0];
									if (methodAnnotation.compareTo(MediaType.APPLICATION_JSON) == 0)
									{
										rightMethod = method;
										rightArguments = new ArrayList<String>();
									}
								}
								else
								{
									rightMethod = method;
									rightArguments = new ArrayList<String>();
								}
							}
						}
					}
				}
			}
			// if the method requires parameters we insert them in the list of
			// rightArguments (that we will pass to the method)
			// the parameters are specified as a Json string
			if ((parameters != null) && (!parameters.isEmpty()))
			{
				rightArguments.add(new String(parameters));
			}
			if (rightMethod != null)
			{
				// check how many parameters the method needs and if we need one
				// more parameter (there methods that ask for a parameter that
				// could
				// be empty) we will insert an empty one
				Annotation[][] parameterAnnotations = rightMethod.getParameterAnnotations();
				int numMethodParameters = 0;
				numMethodParameters = parameterAnnotations.length;
				if (numMethodParameters == rightArguments.size() + 1)
				{
					rightArguments.add(null);
				}
				// if the method will return a String value we save the result
				// otherwise we have to intercept the WebApplicationException
				// generated to give the result of the action
				if (rightMethod.getGenericReturnType().equals(String.class))
				{
					if (clazz.toString().toLowerCase().indexOf("device") != -1)
					{
						result = (String) rightMethod.invoke(this.deviceRESTApi.get(), rightArguments.toArray());
					}
					if (clazz.toString().toLowerCase().indexOf("environment") != -1)
					{
						result = (String) rightMethod.invoke(this.environmentRESTApi.get(), rightArguments.toArray());
					}
					if (clazz.equals(this.getClass()))
					{
						result = (String) rightMethod.invoke(this, rightArguments.toArray());
					}
				}
				else
				{
					if (clazz.toString().toLowerCase().contains("device"))
					{
						try
						{
							rightMethod.invoke(this.deviceRESTApi.get(), rightArguments.toArray());
						}
						catch (WebApplicationException | InvocationTargetException e)
						{
							// here we intercept the Exception generated to say
							// if
							// the operation has executed correctly
							String resultMessage = "";
							if (e instanceof InvocationTargetException)
							{
								InvocationTargetException exception = (InvocationTargetException) e;
								resultMessage = exception.getTargetException().getMessage();
							}
							else if (e instanceof WebApplicationException)
							{
								WebApplicationException exception = (WebApplicationException) e;
								resultMessage = exception.getResponse().toString();
								if (resultMessage.toLowerCase().contains("ok"))
								{
									resultMessage = "Command executed successfully";
								}
								else
									resultMessage = "Command execution failed";
							}
							
							// we send the result as Json
							WebsocketJsonInvocationResult jsonResult = new WebsocketJsonInvocationResult();
							jsonResult.setResult(resultMessage);
							return this.mapper.writeValueAsString(jsonResult);
						}
					}
					if (clazz.toString().toLowerCase().contains("environment"))
					{
						try
						{
							rightMethod.invoke(this.environmentRESTApi.get(), rightArguments.toArray());
						}
						catch (WebApplicationException | InvocationTargetException e)
						{
							// here we intercept the Exception generated to say
							// if
							// the operation has executed correctly
							String resultMessage = "";
							if (e instanceof InvocationTargetException)
							{
								InvocationTargetException exception = (InvocationTargetException) e;
								resultMessage = exception.getTargetException().getMessage();
							}
							else if (e instanceof WebApplicationException)
							{
								WebApplicationException exception = (WebApplicationException) e;
								resultMessage = exception.getResponse().toString();
								if (resultMessage.toLowerCase().contains("ok"))
								{
									resultMessage = "Command executed successfully";
								}
								else
									resultMessage = "Command execution failed";
							}
							// we send the result as Json
							WebsocketJsonInvocationResult jsonResult = new WebsocketJsonInvocationResult();
							jsonResult.setResult(resultMessage);
							return this.mapper.writeValueAsString(jsonResult);
						}
					}
					if (clazz.equals(this.getClass()))
					{
						try
						{
							rightMethod.invoke(this, rightArguments.toArray());
						}
						catch (Exception e)
						{
							e.printStackTrace();
							String resultMessage = "Command execution failed";
							WebsocketJsonInvocationResult jsonResult = new WebsocketJsonInvocationResult();
							jsonResult.setResult(resultMessage);
							return this.mapper.writeValueAsString(jsonResult);
						}
					}
				}
			}
		}
		return result;
	}
	
	/**
	 * Get the class containing the annotation we are looking for (at this level
	 * we look only for the first part of endPoint (api/devices for
	 * DeviceRESTApi and api/devices for EnvironmentRESTApi)
	 * 
	 * @param endPoint
	 *            Path for which we are looking for (that denote the method that
	 *            has to be invoke)
	 * 
	 * @param cls
	 *            ClassLoader
	 * 
	 * @return a {Class<?>} object containing the right class response to the
	 *         status API
	 * 
	 */
	public Class<?> getRightClass(String endPoint, ClassLoader cls) throws ClassNotFoundException
	{
		Class<?> clazz = null;
		// first of all we check if the method searched is in the DeviceRESTApi
		// class
		try
		{
			clazz = cls.loadClass("it.polito.elite.dog.communication.rest.device.api.DeviceRESTApi");
			if (checkClass(clazz, endPoint))
			{
				return clazz;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		// if the method is not in the DeviceRESTApi class (so if it doesn't
		// return), we look for it
		// in the EnvironmentRESTApi class
		try
		{
			clazz = cls.loadClass("it.polito.elite.dog.communication.rest.environment.api.EnvironmentRESTApi");
			if (checkClass(clazz, endPoint))
			{
				return clazz;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Check if the class indicated contains the method annotated by endPoint
	 * 
	 * @param clazz
	 *            Class in which we have to look for the method searched
	 * 
	 * @param endPoint
	 *            Path for which we are looking for (that denote the method that
	 *            has to be invoke)
	 * 
	 * @return a {boolean} object a true value indicates that the clazz contains
	 *         the method
	 * 
	 */
	
	boolean checkClass(Class<?> clazz, String endPoint) throws ClassNotFoundException
	{
		
		boolean found = true;
		// we divide the class annotation in different parts (that are separated
		// by "/") and we count the number of parameters obtained (so we know
		// when we have to stop the comparison)
		Path tempAnnotation = (Path) clazz.getAnnotation(Path.class);
		if (tempAnnotation != null)
		{
			String classAnnotation = tempAnnotation.value().toString();
			if (classAnnotation.startsWith("/"))
			{
				classAnnotation = classAnnotation.replaceFirst("/", "");
			}
			String[] annotationParts = classAnnotation.split("/");
			this.numberOfClassParameters = 0;
			for (String annotationPart : annotationParts)
			{
				if (annotationPart.compareTo(this.endPointParts[this.numberOfClassParameters]) != 0)
				{
					found = false;
					break;
				}
				else
					this.numberOfClassParameters++; // it is used in the other
													// method to start looking
													// for
													// the method from the right
													// point of Endpoint
			}
			return found;
		}
		else
			return false;
		
	}
	
}