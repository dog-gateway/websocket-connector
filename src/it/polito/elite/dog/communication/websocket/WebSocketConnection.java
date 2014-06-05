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
import it.polito.elite.dog.communication.websocket.message.WebSocketJsonInvocationResult;
import it.polito.elite.dog.communication.websocket.message.WebSocketJsonNotification;
import it.polito.elite.dog.communication.websocket.message.WebSocketJsonRequest;
import it.polito.elite.dog.communication.websocket.message.WebSocketJsonResponse;
import it.polito.elite.dog.core.library.model.notification.Notification;
import it.polito.elite.dog.core.library.util.LogHelper;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonGenerationException;
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

/**
 * 
 * @author <a href="mailto:teo.montanaro@gmail.com">Teodoro Montanaro</a>
 * @author <a href="mailto:luigi.derussis@polito.it">Luigi De Russis</a>
 * @see <a href="http://elite.polito.it">http://elite.polito.it</a>
 * 
 */
public class WebSocketConnection implements WebSocket.OnTextMessage
{
	// instance of the connection used to obtain the user id
	private WebSocketConnection connectionInstance;
	// number of initial parameters in the endPoint (ex uri or path) that
	// indicates the class in which the requested action will be performed
	// (devices, environment, rules, ...)
	private int numberOfClassParameters;
	// initial parameters in the endPoint (ex uri or path) that indicate the
	// class in which the requested action will be performed (devices,
	// environment, rules, ...)
	private String[] endPointParts;
	
	// reference for the WebSocketEndPoint
	private WebSocketEndPoint webSocketEndPoint;
	
	// connection used to send and receive messages
	private Connection connection;
	
	// reference for the DeviceRESTApi
	// private AtomicReference<DeviceRESTApi> deviceRESTApi;
	// reference for the EnvironmentRESTApi
	// private AtomicReference<EnvironmentRESTApi> environmentRESTApi;
	// reference for the RuleEngineRESTApi
	// private AtomicReference<RuleEngineRESTApi> ruleEngineRESTApi;
	
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
	
	public WebSocketConnection(BundleContext context, WebSocketEndPoint webSocketEndPoint/*
																						 * ,
																						 * AtomicReference
																						 * <
																						 * DeviceRESTApi
																						 * >
																						 * deviceRESTApi
																						 * ,
																						 * AtomicReference
																						 * <
																						 * EnvironmentRESTApi
																						 * >
																						 * environmentRESTApi
																						 * ,
																						 * AtomicReference
																						 * <
																						 * RuleEngineRESTApi
																						 * >
																						 * ruleEngineRESTApi
																						 */)
	{
		// init the WebSocketEndPoint reference
		this.webSocketEndPoint = webSocketEndPoint;
		// init the Device Rest Api atomic reference
		// this.deviceRESTApi = deviceRESTApi;
		// init the Environment Rest Api atomic reference
		// this.environmentRESTApi = environmentRESTApi;
		// init the RuleEngine REST Api atomic reference
		// this.ruleEngineRESTApi = ruleEngineRESTApi;
		
		// init the variable used to store the type of received message for the
		// notification registration
		this.typeForRegistration = "";
		
		// init the variable used to store the clientId for the notification
		// registration
		this.clientIdForRegistration = "";
		
		// init the number of initial parameters in the endPoint (ex uri or
		// path) that indicates the class in which the requested action will be
		// performed (devices, environment, rules, ...)
		this.numberOfClassParameters = 0;
		
		// store the bundle context
		this.context = context;
		// init the connection intance used to obtain the user id
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
	 * Method called when the user close the connection
	 * 
	 */
	@Override
	public void onClose(int statusCode, String reason)
	{
		// if the connection is closed we remove the user from the list of users
		// connected to the server and his notifications from the list of
		// notifications
		this.webSocketEndPoint.removeUser(this.toString());
	}
	
	/**
	 * Method called when the user open the connection
	 * 
	 * @param connection
	 * 
	 */
	@Override
	public void onOpen(Connection connection)
	{
		// set the maximum time of the connection to the maximum available
		connection.setMaxIdleTime(Integer.MAX_VALUE);
		// init the connection
		this.connection = connection;
		
		// add the user to the list of users connected to the system
		this.webSocketEndPoint.addUser(this.toString(), connection);
		// this.webSocketEndPoint.addUser(new WebSocketPeer(this.toString()));
		this.logger.log(LogService.LOG_DEBUG, "Connection Protocol: " + connection.getProtocol());
		if (this.connection.isOpen())
		{
			try
			{
				// extract the userId from the instance name (by taking the last
				// part of the connection instance (after the @))
				String userId = this.connectionInstance.toString();
				// send the starting message (presentation) with the clientId
				// generated
				this.connection.sendMessage("{ \"clientId\": \"" + userId
						+ "\",\"messageType\":\"info\",\"type\":\"presentation\" }");
			}
			catch (IOException e)
			{
				// it was not possible to open the connection
				this.logger.log(LogService.LOG_INFO, e.toString());
				// if something goes wrong we remove the user from the list of
				// users connected to the server
				this.webSocketEndPoint.removeUser(this.toString());
			}
		}
	}
	
	/**
	 * Method called when the user send a message to the server
	 * 
	 * @param data
	 *            Received message
	 * 
	 */
	@Override
	public void onMessage(String data)
	{
		// if we actually receive data we process them
		if (!data.isEmpty())
		{
			// variable used to parse all the Json data
			WebSocketJsonRequest webSocketReceivedData;
			try
			{
				// parse Json code received
				webSocketReceivedData = this.mapper.readValue(data, WebSocketJsonRequest.class);
				this.logger.log(LogService.LOG_INFO, "Received data: " + webSocketReceivedData.toString());
				String clientId = webSocketReceivedData.getClientId();
				String sequenceNumber = webSocketReceivedData.getSequenceNumber();
				String messageType = webSocketReceivedData.getMessageType();
				String type = webSocketReceivedData.getType();
				String action = webSocketReceivedData.getAction();
				String endPoint = webSocketReceivedData.getEndPoint();
				String parameters = webSocketReceivedData.getParameters();
				// check if the user is the right one and if the message is a
				// request
				if ((clientId != null)
						&& clientId.equals(this.connectionInstance.toString())
						&& (messageType.equals("request")))
				{
					// if it is a notification registration we have to call the
					// method to register a notification (or a list of
					// notifications)
					// the parameters could be a list of string
					// ["TemperatureMeasurementNotification",
					// "AlertNotification", "BatteryLevelNotification"]
					// or a simple string : "TemperatureMeasurementNotification"
					// but here we set only the parameters needed in the invoked
					// method
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
					else
					{
						// reset the type for the registration (if it is not a
						// notification request but the previous request was a
						// notification one, it is necessary to reset the type
						// value, otherwise the method will recognize it as a
						// notification one)
						this.typeForRegistration = type;
						// set to 0 the variable by which we usually count the
						// initial parts of path that indicates the class
						this.numberOfClassParameters = 0;
					}
					String result;
					try
					{
						// obtain the requested information from the method
						// that invoke the right method indicated by Path
						// annotation
						result = this.invokeMethodByAnnotation(endPoint, action, parameters);
						
						// debug
						this.logger.log(LogService.LOG_DEBUG, "Sending data: " + result);
						
						// transform the result in a Json message
						WebSocketJsonResponse jsonResponse = new WebSocketJsonResponse();
						if (!clientId.isEmpty())
							jsonResponse.setClientId(clientId);
						if (!sequenceNumber.isEmpty())
							jsonResponse.setSequenceNumber(sequenceNumber);
						jsonResponse.setMessageType("response");
						if (!action.isEmpty())
							jsonResponse.setAction(action);
						if (!endPoint.isEmpty())
							jsonResponse.setEndPoint(endPoint);
						// if we receive an empty result from the method it
						// means that something went wrong
						if (result.isEmpty())
						{
							WebSocketJsonInvocationResult jsonResult = new WebSocketJsonInvocationResult();
							jsonResult.setResult("Something went wrong");
							result = this.mapper.writeValueAsString(jsonResult);
						}
						Object resultObject;
						// if the result is not a Json object the readTree
						// method generates an exception that we intercept to
						// insert the result as string in the answer
						try
						{
							resultObject = this.mapper.readTree(result);
						}
						catch (Exception e)
						{
							WebSocketJsonInvocationResult jsonResult = new WebSocketJsonInvocationResult();
							jsonResult.setResult(result);
							result = this.mapper.writeValueAsString(jsonResult);
							resultObject = this.mapper.readTree(result);
						}
						jsonResponse.setResponse(resultObject);
						
						String response = this.mapper.writeValueAsString(jsonResponse);
						// send the message just created
						this.connectionInstance.connection.sendMessage(response);
					}
					catch (Exception e)
					{
						this.logger.log(LogService.LOG_WARNING, "Exception in invoking some REST API methods", e);
					}
				}
				else
				{
					// the request has not the right parameters (clientId and
					// messageType)
					// the error message is sent in json format
					WebSocketJsonResponse jsonResponse = new WebSocketJsonResponse();
					if (!clientId.isEmpty())
						jsonResponse.setClientId(clientId);
					if (!sequenceNumber.isEmpty())
						jsonResponse.setSequenceNumber(sequenceNumber);
					jsonResponse.setMessageType("info");
					jsonResponse.setResponse("You forgot to send the clientId or the message type is not a request");
					String response = this.mapper.writeValueAsString(jsonResponse);
					this.connectionInstance.connection.sendMessage(response);
				}
			}
			catch (IOException ex)
			{
				this.logger.log(LogService.LOG_ERROR, "Error in handling messages", ex);
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
		// we obtain the parts of the uri used by the notification as name
		String[] topicParts = notificationEvent.getTopic().split("/");
		String eventName = topicParts[topicParts.length - 1];
		// create the variable that will contain all the fields contained in the
		// notification (deviceUri, notificationTopic ...)
		HashMap<String, String> notificationContent = new HashMap<String, String>();
		// we obtain necessary information about the notifications
		Object eventContent = notificationEvent.getProperty("event");
		if (eventContent != null)
		{
			// we can received a single notification or more than one
			// notification so we create a list in every case (containing one
			// ore more notifications)
			List<Notification> notificationList = new ArrayList<Notification>();
			if (eventContent instanceof HashSet)
				notificationList.addAll((Collection<? extends Notification>) eventContent);
			else
				notificationList.add((Notification) eventContent);
			// we scroll through all items of the list of notifications received
			// to store all the fields contained in it
			for (Notification singleNotification : notificationList)
			{
				WebSocketJsonNotification notificationResponse = new WebSocketJsonNotification();
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
						// notificationTopic contains more information than the
						// ones we need (it/polito/elite/...) so we take only
						// the last part
						if (notificationFieldName.equals("notificationTopic"))
						{
							String[] fieldValueFinalParts = notificationFieldValueFinal.split("/");
							notificationFieldValueFinal = fieldValueFinalParts[fieldValueFinalParts.length - 1];
						}
						// insert the information acquired in the list
						notificationContent.put(notificationFieldName, notificationFieldValueFinal);
					}
					catch (Exception e)
					{
						// if something went wrong we want to continue for the
						// other notificationField
						this.logger.log(LogService.LOG_WARNING,
								"Ops! Something goes wrong in parsing a notification... skip!", e);
					}
				}
				// the Event Handler is executed only once (on the last
				// instance), so it is important to do the following things
				// (check and send right notifications) for all the users
				for (WebSocketPeer user : this.webSocketEndPoint.getUsers().values())
				{
					if (!notificationContent.isEmpty())
					{
						try
						{
							// here we send the notification only to the users
							// that have submitted to receive the kind of
							// notification just received
							Map<String, ArrayList<String>> listOfControllable = new HashMap<String, ArrayList<String>>();
							try
							{
								listOfControllable.putAll(this.webSocketEndPoint.getNotificationsPerUser(user
										.getPeerId().substring(user.getPeerId().indexOf("@") + 1)));
							}
							catch (Exception e)
							{
								// if the list is null it has to continue
								// without copying the list
								this.logger.log(LogService.LOG_WARNING, "The list of notifications is empty");
							}
							// we send only the right notifications checking the
							// list
							if (listOfControllable != null && (!notificationContent.get("deviceUri").isEmpty()))
							{
								// first of all we check for the specific
								// deviceUri, then, if there is not this
								// specific deviceUri in the list we check for
								// "all" entry
								List<String> listOfNotification = listOfControllable.get((String) notificationContent
										.get("deviceUri"));
								if (listOfNotification == null)
								{
									listOfNotification = listOfControllable.get("all");
								}
								if (listOfNotification != null
										&& (listOfNotification.contains(eventName.toString()) || listOfNotification
												.contains("all")))
								{
									// transform the notification in Json
									// format, with clientId, messageType, type
									notificationResponse.setNotification(notificationContent);
									notificationResponse.setClientId(user.getPeerId().substring(
											user.getPeerId().indexOf("@") + 1));
									notificationResponse.setMessageType("info");
									notificationResponse.setType("notification");
									String notificationToSend = this.mapper.writeValueAsString(notificationResponse);
									user.getConnection().sendMessage(notificationToSend);
								}
							}
						}
						catch (IOException e)
						{
							// if something went wrong we want to continue for
							// the other users
							this.logger.log(LogService.LOG_WARNING, "Error handling notification content, skip!", e);
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
	 * @param controllable
	 *            Controllable id (deviceUri) of the device for which we want to
	 *            unsubscribe notifications
	 * 
	 * @param notificationsToRemove
	 *            List of notifications from which the user want to be
	 *            unsubscribed
	 * 
	 * @return a {String} result containing the result of the unregistration
	 */
	public String notificationUnregistration(String clientId, String controllable, Object notificationsToRemove)
			throws JsonParseException, JsonMappingException
	{
		// store a backup copy of the list of notifications, so, in case of
		// errors, we can restore it
		HashMap<String, HashMap<String, ArrayList<String>>> listOfNotificationsPerUserBackup = new HashMap<String, HashMap<String, ArrayList<String>>>();
		listOfNotificationsPerUserBackup = this.webSocketEndPoint.copyHashMapByValue(this.webSocketEndPoint
				.getNotifications());
		
		Object notifications;
		// chech if the notifications received are in json format or not
		try
		{
			notifications = this.mapper.readTree((String) notificationsToRemove);
		}
		catch (Exception e)
		{
			notifications = notificationsToRemove;
		}
		// set default result
		String result = "Unregistration failed";
		// The notifications could be a simple string (only one notification) or
		// a list of element
		if (notifications instanceof String)
		{
			// if we receive only one single notification we can call directly
			// the method that does the unregistration that will return true if
			// the unsubscribtion was successfully completed
			if (this.webSocketEndPoint.removeNotification(clientId, controllable, (String) notifications))
				result = "Unregistration completed successfully";
		}
		else if (notifications instanceof ArrayNode)
		{
			// scroll through all the items received and for each of them we
			// call the method that does the unregistration
			ArrayNode notificationsArrayNode = (ArrayNode) notifications;
			Iterator<JsonNode> iterator = notificationsArrayNode.getElements();
			result = "Unregistration completed successfully";
			while (iterator.hasNext())
			{
				JsonNode current = iterator.next();
				if (!this.webSocketEndPoint.removeNotification(clientId, controllable, (String) current.getTextValue()))
				{
					// if something goes wrong we reset the value copying the
					// list backed up at the beginning of the unregistration
					// process returning an error as result
					this.webSocketEndPoint.setNotifications(this.webSocketEndPoint
							.copyHashMapByValue(listOfNotificationsPerUserBackup));
					result = "Unregistration failed";
					break;
				}
			}
		}
		else
		{
			// if the notification list is empty the user wants to unsubscribe
			// all the notifications
			if (this.webSocketEndPoint.removeNotification(clientId, controllable, "all"))
				result = "Unregistration completed successfully";
		}
		
		try
		{
			// we try to set the result as json but if it generates any kind of
			// exception we will set it directly as a string
			WebSocketJsonInvocationResult jsonResult = new WebSocketJsonInvocationResult();
			jsonResult.setResult(result);
			return this.mapper.writeValueAsString(jsonResult);
		}
		catch (Exception e)
		{
			// if it is not possible to parse the result we send it as string
			// (creating the Json code by hand)
			this.logger.log(LogService.LOG_WARNING, "Impossible to parse the result we send as a string", e);
			return "{\"result\":\"" + result + "\"}";
		}
	}
	
	/**
	 * Register or unregister a notification for all the controllables
	 * 
	 * the clientId and the type of registration are taken from the instance
	 * because we want to invoke this method directly with the path indicated by
	 * the user
	 * 
	 * @param notifications
	 *            List of notifications from which the user want to be
	 *            unsubscribed
	 * 
	 * @return a {String} result containing the result of the registration
	 * 
	 */
	@PUT
	@Path("/api/v1/devices/notifications")
	public String notificationRegistrationWithoutControllable(Object notifications) throws JsonParseException,
			JsonMappingException
	{
		// depending on the type of registration indicated in the request we
		// call one different method
		if (this.typeForRegistration != null
				&& this.typeForRegistration.toLowerCase().contains("notificationregistration"))
		{
			return this.notificationRegistration(this.clientIdForRegistration, "all", notifications);
		}
		if (this.typeForRegistration != null
				&& this.typeForRegistration.toLowerCase().contains("notificationunregistration"))
		{
			return this.notificationUnregistration(this.clientIdForRegistration, "all", notifications);
		}
		
		// if something went wrong (so if we can't access to one of the method
		// above, it means that one or more parameters was missed
		try
		{
			WebSocketJsonInvocationResult jsonResult = new WebSocketJsonInvocationResult();
			jsonResult
					.setResult("The command sent was not a valid command: you forgot (or wrote a wrong one) the type of message (notificationRegistration or notificationUnregistration)");
			return this.mapper.writeValueAsString(jsonResult);
		}
		catch (Exception e)
		{
			this.logger
					.log(LogService.LOG_ERROR,
							"The command sent was not a valid command: you forgot (or wrote a wrong one) the type of message (notificationRegistration or notificationUnregistration)");
			return "{\"result\":\"The command sent was not a valid command: you forgot (or wrote a wrong one) the type of message (notificationRegistration or notificationUnregistration)\"}";
		}
	}
	
	/**
	 * Register or unregister a notification for specific controllables
	 * 
	 * the clientId and the type of registration are taken from the instance
	 * because we want to invoke this method directly with the path indicated by
	 * the user
	 * 
	 * @param controllable
	 *            Controllable to which we want to unsubscribe the notifications
	 *            indicated
	 * 
	 * @param notifications
	 *            List of notifications from which the user want to be
	 *            unsubscribed
	 * 
	 * @return a {String} result containing the result of the registration
	 * 
	 */
	@PUT
	@Path("/api/v1/devices/{controllable}/notifications")
	public String notificationRegistrationWithControllable(@PathParam("controllable") String controllable,
			Object notifications) throws JsonParseException, JsonMappingException
	{
		// depending on the type of registration indicated in the request we
		// call one different method
		if (this.typeForRegistration != null
				&& this.typeForRegistration.toLowerCase().contains("notificationregistration"))
		{
			return this.notificationRegistration(this.clientIdForRegistration, controllable, notifications);
		}
		if (this.typeForRegistration != null
				&& this.typeForRegistration.toLowerCase().contains("notificationunregistration"))
		{
			return this.notificationUnregistration(this.clientIdForRegistration, controllable, notifications);
		}
		
		// if something went wrong (so if we can't access to one of the method
		// above, it means that one or more parameters was missed
		try
		{
			WebSocketJsonInvocationResult jsonResult = new WebSocketJsonInvocationResult();
			jsonResult
					.setResult("The command sent was not a valid command: you forgot (or wrote a wrong one) the type of message (notificationRegistration or notificationUnregistration)");
			return this.mapper.writeValueAsString(jsonResult);
		}
		catch (Exception e)
		{
			this.logger
					.log(LogService.LOG_ERROR,
							"The command sent was not a valid command: you forgot (or wrote a wrong one) the type of message (notificationRegistration or notificationUnregistration)");
			return "{\"result\":\"The command sent was not a valid command: you forgot (or wrote a wrong one) the type of message (notificationRegistration or notificationUnregistration)\"}";
		}
	}
	
	/**
	 * Register a notification (main method)
	 * 
	 * @param clientId
	 *            Id of the client that is requiring the subscription
	 * @param controllable
	 *            the id of the device for which we want to subscribe the
	 *            notifications
	 * @param notifications
	 *            List of notifications from which the user want to be
	 *            subscribed
	 * 
	 * @return a {String} result containing the result of the registration
	 */
	private String notificationRegistration(String clientId, String controllable, Object notificationsAcquired)
			throws JsonParseException, JsonMappingException
	{
		// set the default result value
		String result = "Registration failed";
		Object notifications;
		// check if the notifications are in json format or not
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
			// same notification name twice (or more) we insert only one
			if (notifications instanceof String)
			{
				// if we receive only a single notification we can add it
				// directly to the list of notifications
				// but we do it only if the user has never subscribed the
				// notification just received
				if (!notificationsList.contains((String) notifications))
					notificationsList.add((String) notifications);
				
			}
			else if (notifications instanceof ArrayNode)
			{
				// scroll through all the items received and for each of them,
				// if the user has never subscribed it, we add the specific
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
			// to be subscribed we can call the method that does the real
			// subscription
			if (this.webSocketEndPoint.putNotifications(clientId, controllable, notificationsList))
				result = "Registration completed successfully";
		}
		catch (Exception e)
		{
			this.logger.log(LogService.LOG_ERROR, "Notification registration failed");
			result = "Registration failed";
		}
		
		// try to convert the result in json format
		try
		{
			WebSocketJsonInvocationResult jsonResult = new WebSocketJsonInvocationResult();
			jsonResult.setResult(result);
			return this.mapper.writeValueAsString(jsonResult);
		}
		catch (Exception e)
		{
			this.logger.log(LogService.LOG_ERROR, "Impossible to convert an object in JSON", e);
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
	 * @return a {String} result containing the result generated by the method
	 *         just invoked
	 * 
	 */
	private String invokeMethodByAnnotation(String endPoint, String action, String parameters)
			throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException,
			InstantiationException, IllegalAccessException, InvocationTargetException, JsonGenerationException,
			JsonMappingException
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
		// if the type of the message contains the word "notification" it means
		// that the right class is this one, otherwise we call the method that
		// identify the right class
		if (this.typeForRegistration != null && this.typeForRegistration.toLowerCase().contains("notification"))
		{
			clazz = this.getClass();
		}
		else
		{
			ClassLoader cls = this.webSocketEndPoint.getClassloader();
			clazz = this.getRightClass(endPoint, cls);
		}
		String result = "";
		
		if (clazz != null)
		{
			// now we scroll trough all available methods looking for the
			// annotations present
			// if there are both "/api/v1/devides/status" and
			// "/api/v1/devices/{device-id}" we have to take care to choose the
			// right one
			Method[] methods = clazz.getDeclaredMethods();
			Method rightMethod = null;
			List<String> rightArguments = new ArrayList<String>();
			for (Method method : methods)
			{
				// we start to check for the annotation from the point to which
				// the getRightClass arrived, so we copy the number indicating
				// the part of path already analized
				// (for example after api/v1/devices/)
				int numberOfAnalizedPartsOfEndPoint = this.numberOfClassParameters;
				// we check if the analyzed method has the right action
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
							// method we are looking for have the same number of
							// annotation parts
							if (methodAnnotationParts.length == (this.endPointParts.length - numberOfAnalizedPartsOfEndPoint))
							{
								// for all the parts that makes up the path
								for (int k = 0; k < (this.endPointParts.length - this.numberOfClassParameters); k++)
								{
									// if there are both
									// "/api/v1/devides/status"
									// and "/api/v1/devices/{device-id}" we have
									// to
									// take care to choose the right one
									if (methodAnnotationParts[k]
											.compareTo(this.endPointParts[numberOfAnalizedPartsOfEndPoint]) != 0)
									{
										// if I have already chosen a method it
										// means that a method without a
										// parameter exists (because in the
										// other case I say that it is the right
										// method only at the end (when I've
										// analyzed all the parameters))
										if (rightMethod == null)
										{
											// if "{" or "}" is present it means
											// that it is a parameter
											// So we insert it in the list of
											// arguments (we need the parameters
											// for the invoke method)
											if ((methodAnnotationParts[k].indexOf("{") != -1)
													&& (methodAnnotationParts[k].indexOf("}") != -1))
											{
												arguments.add(new String(
														this.endPointParts[numberOfAnalizedPartsOfEndPoint]));
											}
											else
											{
												// otherwise the method is not
												// the right one (the following
												// part of path is not
												// corresponding)
												break;
											}
										}
										else
											// if a method was already chosen
											// and we are here it would means
											// that it
											// is the wrong method or that we
											// have
											// already chosen the method without
											// parameters
											// (/api/v1/devices/status
											// instead of
											// /api/v1/devices/{devide-id})
											break;
									}
									numberOfAnalizedPartsOfEndPoint++;
								}
								if (numberOfAnalizedPartsOfEndPoint == (this.endPointParts.length))
								{
									// if we arrived at the end of Path and the
									// dimension of endPoint is equal to the
									// dimension of annotation it may be the
									// right method
									// but we have to check if the output of the
									// method is the right one (Json) (if there
									// is not a "Produces" annotation we assumes
									// that it produces Json)
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
							// path used to select the class (/api/v1/devices or
							// /api/v1/environment) it means that the method we
							// are
							// looking for has to answer to path /api/v1/devices
							// or
							// /api/v1/environment
							if ((this.endPointParts.length - numberOfAnalizedPartsOfEndPoint) == 0)
							{
								// we have to check if the output of the method
								// is the right one (Json) (if there is not a
								// "Produces" annotation we assumes that it
								// produces Json)
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
				// more parameter (there are methods that ask for a parameter
				// that could be empty) we will insert an empty one
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
					// depending on the class selected we have to use the right
					// reference (deviceRESTApi, environmentRESTApi,
					// ruleEngineRESTApi, or this (WebSocketImplementation))
					try
					{
						// even if the method should return a String, if the
						// device or the environment doesn't exist, it return a
						// 404 NOT Found message as exception, so we have to
						// intercept the exception
						// TODO
						if (clazz.toString().toLowerCase().indexOf("device") != -1)
						{
							result = (String) rightMethod.invoke(webSocketEndPoint.restEndpoint/*
																								 * this
																								 * .
																								 * deviceRESTApi
																								 * .
																								 * get
																								 * (
																								 * )
																								 */,
									rightArguments.toArray());
						}
						/*
						 * if
						 * (clazz.toString().toLowerCase().indexOf("environment"
						 * ) != -1) { result = (String)
						 * rightMethod.invoke(this.environmentRESTApi.get(),
						 * rightArguments.toArray()); }
						 */
						/*
						 * if (clazz.toString().toLowerCase().indexOf("rule") !=
						 * -1) { result = (String) rightMethod
						 * .invoke(this.ruleEngineRESTApi.get(),
						 * rightArguments.toArray()); }
						 */
					}
					catch (Exception e)
					{
						// here we intercept the Exception generated to say
						// that the requested resource was not found
						String resultMessage = "The requested resource was not found";
						
						// we send the result as Json
						WebSocketJsonInvocationResult jsonResult = new WebSocketJsonInvocationResult();
						jsonResult.setResult(resultMessage);
						try
						{
							return this.mapper.writeValueAsString(jsonResult);
						}
						catch (IOException e1)
						{
							this.logger.log(LogService.LOG_INFO, e.toString());
							return "{\"result\":\"" + jsonResult + "\"}";
						}
					}
					if (clazz.equals(this.getClass()))
					{
						result = (String) rightMethod.invoke(this, rightArguments.toArray());
					}
				}
				else
				{
					// if the chosen method doesn't return a string it should
					// generate an exception in which we can find the result of
					// the operation
					if (clazz.toString().toLowerCase().contains("device")
							|| clazz.toString().toLowerCase().contains("environment")
							|| clazz.toString().toLowerCase().contains("rule"))
					{
						try
						{
							// TODO
							if (clazz.toString().toLowerCase().contains("device"))
							{
								rightMethod.invoke(webSocketEndPoint.restEndpoint/*
																				 * this
																				 * .
																				 * deviceRESTApi
																				 * .
																				 * get
																				 * (
																				 * )
																				 */, rightArguments.toArray());
							}
							/*
							 * if (clazz.toString().toLowerCase().contains(
							 * "environment")) {
							 * rightMethod.invoke(this.environmentRESTApi.get(),
							 * rightArguments.toArray()); }
							 */
							/*
							 * if
							 * (clazz.toString().toLowerCase().contains("rule"))
							 * {
							 * rightMethod.invoke(this.ruleEngineRESTApi.get(),
							 * rightArguments.toArray()); }
							 */
							
						}
						catch (Exception e)
						{
							// here we intercept the Exception generated to say
							// if the operation was executed correctly
							String resultMessage = "";
							if (e instanceof InvocationTargetException)
							{
								// even if the InvocationTargetException is the
								// actually exception generated, the right one
								// would be a WebApplicationException, so we
								// manage that exception too
								InvocationTargetException exception = (InvocationTargetException) e;
								resultMessage = exception.getTargetException().getMessage();
								if (resultMessage.toLowerCase().contains("ok")
										|| resultMessage.toLowerCase().contains("created"))
								{
									resultMessage = "Command executed successfully";
								}
								else
									resultMessage = "Command execution failed";
							}
							else if (e instanceof WebApplicationException)
							{
								// even if the InvocationTargetException is the
								// actually exception generated, the right one
								// would be a WebApplicationException, so we
								// manage that exception too
								WebApplicationException exception = (WebApplicationException) e;
								resultMessage = exception.getResponse().toString();
								if (resultMessage.toLowerCase().contains("ok")
										|| resultMessage.toLowerCase().contains("created"))
								{
									resultMessage = "Command executed successfully";
								}
								else
									resultMessage = "Command execution failed";
							}
							
							// we send the result as Json
							WebSocketJsonInvocationResult jsonResult = new WebSocketJsonInvocationResult();
							jsonResult.setResult(resultMessage);
							try
							{
								return this.mapper.writeValueAsString(jsonResult);
							}
							catch (IOException e1)
							{
								this.logger.log(LogService.LOG_INFO, e.toString());
								return "{\"result\":\"" + jsonResult + "\"}";
							}
						}
					}
					else if (clazz.equals(this.getClass()))
					{
						// if the right class is this one
						// (WebSocketImplementation) but the right method
						// doesn't generate a String as result we set the result
						// as "success" if the method doesn't generate
						// exceptions
						
						// set the default result value
						String resultMessage = "Command execution failed";
						try
						{
							rightMethod.invoke(this, rightArguments.toArray());
							resultMessage = "Command executed successfully";
							
						}
						catch (Exception e)
						{
							resultMessage = "Command execution failed";
						}
						
						// we send the result as Json
						WebSocketJsonInvocationResult jsonResult = new WebSocketJsonInvocationResult();
						jsonResult.setResult(resultMessage);
						try
						{
							return this.mapper.writeValueAsString(jsonResult);
						}
						catch (IOException e1)
						{
							this.logger.log(LogService.LOG_INFO, e1.toString());
							return "{\"result\":\"" + jsonResult + "\"}";
						}
					}
				}
			}
		}
		// if something goes wrong we return an empty string
		return result;
	}
	
	/**
	 * Get the class containing the annotation we are looking for (at this level
	 * we look only for the first part of endPoint (api/v1/devices for
	 * DeviceRESTApi and api/v1/devices for EnvironmentRESTApi)
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
	private Class<?> getRightClass(String endPoint, ClassLoader cls) throws ClassNotFoundException
	{
		Class<?> clazz = null;
		// first of all we check if the method searched is in the DeviceRESTApi
		// class
		try
		{
			ClassLoader clsI = webSocketEndPoint.registeredEndpoint.getClassLoader();
			Thread.currentThread().setContextClassLoader(clsI);
			clazz = clsI
					.loadClass(webSocketEndPoint.endpointPackages[1]/* "it.polito.elite.dog.communication.rest.device.api.DeviceRESTApi" */);
			if (this.checkClass(clazz, endPoint))
			{
				return clazz;
			}
		}
		catch (Exception e)
		{
			// it (DeviceRESTApi) is not the right class
			this.logger.log(LogService.LOG_INFO, e.toString());
		}
		finally
		{
			Thread.currentThread().setContextClassLoader(cls);
		}
		
		// if the method is not in the DeviceRESTApi class (so if it doesn't
		// return), we look for it
		// in the EnvironmentRESTApi class
		try
		{
			clazz = cls.loadClass("it.polito.elite.dog.communication.rest.environment.api.EnvironmentRESTApi");
			if (this.checkClass(clazz, endPoint))
			{
				return clazz;
			}
		}
		catch (Exception e)
		{
			// it (EnvironmentRESTApi) is not the right class
			this.logger.log(LogService.LOG_INFO, e.toString());
		}
		
		try
		{
			clazz = cls.loadClass("it.polito.elite.dog.communication.rest.ruleengine.api.RuleEngineRESTApi");
			if (this.checkClass(clazz, endPoint))
			{
				return clazz;
			}
		}
		catch (Exception e)
		{
			// it (RuleEngineRESTApi) is not the right class
			this.logger.log(LogService.LOG_INFO, e.toString());
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
	 * @return a {boolean} object: a true value indicates that the clazz
	 *         contains the method
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
				{
					// it is used in the other method to start looking for the
					// method from the right point of Endpoint
					this.numberOfClassParameters++;
				}
			}
			return found;
		}
		else
			return false;
		
	}
	
	/**
	 * Give the {@link WebSocketConnection} id by taking the instance string and
	 * removing the class name
	 */
	@Override
	public String toString()
	{
		StringBuilder result = new StringBuilder();
		
		String connectionId = super.toString();
		result.append(connectionId.substring(connectionId.indexOf('@') + 1));
		
		return result.toString();
	}
	
}