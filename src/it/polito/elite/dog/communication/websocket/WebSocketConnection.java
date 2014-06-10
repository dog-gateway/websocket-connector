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
import it.polito.elite.dog.communication.websocket.message.WebSocketJsonRequest;
import it.polito.elite.dog.communication.websocket.message.WebSocketJsonResponse;
import it.polito.elite.dog.core.library.util.LogHelper;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.eclipse.jetty.websocket.WebSocket;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

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
	// private String clientIdForRegistration;
	
	// variable used to store the type of received message for the notification
	// registration
	// private String typeForRegistration;
	
	public WebSocketConnection(BundleContext context, WebSocketEndPoint webSocketEndPoint)
	{
		// init the WebSocketEndPoint reference
		this.webSocketEndPoint = webSocketEndPoint;
		// init the Device Rest Api atomic reference
		// this.deviceRESTApi = deviceRESTApi;
		// init the Environment Rest Api atomic reference
		// this.environmentRESTApi = environmentRESTApi;
		// init the RuleEngine REST Api atomic reference
		// this.ruleEngineRESTApi = ruleEngineRESTApi;
		
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
				this.connection.sendMessage("{ \"clientId\": \"" + userId + "\",\"messageType\":\"presentation\" }");
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
				// String type = webSocketReceivedData.getType();
				String action = webSocketReceivedData.getAction();
				String endPoint = webSocketReceivedData.getEndPoint();
				String parameters = webSocketReceivedData.getParameters();
				// check if the user is the right one and if the message is a
				// request
				if ((clientId != null) && clientId.equals(this.connectionInstance.toString())
						&& (messageType.equals("request")))
				{
					
					// set to 0 the variable by which we usually count the
					// initial parts of path that indicates the class
					this.numberOfClassParameters = 0;
					
					String result;
					try
					{
						// obtain the requested information from the method
						// that invoke the right method indicated by Path
						// annotation
						// if it is a notification registration we have to call
						// the
						// method to register a notification (or a list of
						// notifications)
						// the parameters could be a list of string
						// ["TemperatureMeasurementNotification",
						// "AlertNotification", "BatteryLevelNotification"]
						// or a simple string :
						// "TemperatureMeasurementNotification"
						// but here we set only the parameters needed in the
						// invoked
						// method
						// TODO
						if (endPoint != null && endPoint.toLowerCase().contains("subscription"))
						{
							result = this.invokeMethodByAnnotation(endPoint, action, parameters, clientId);
						}
						else
							result = this.invokeMethodByAnnotation(endPoint, action, parameters, null);
						
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
	private String invokeMethodByAnnotation(String endPoint, String action, String parameters, String clientId)
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
		
		ClassLoader cls = this.webSocketEndPoint.getClassloader();
		clazz = this.getRightClass(endPoint, cls);
		
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
			// TODO
			if ((clientId != null) && (!clientId.isEmpty()))
			{
				rightArguments.add(new String(clientId));
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
						boolean invoked = false;
						for (WebSocketConnectorInfo wci : this.webSocketEndPoint.getRegisteredEndpoints())
						{
							if (!invoked)
							{
								// TODO
								if (clazz.isAssignableFrom(wci.getRegisteredBundle()))
								{
									result = (String) rightMethod.invoke(wci.getWebSocketEndpoint(),
											rightArguments.toArray());
									invoked = true;
								}
								else if(clazz.isAssignableFrom(wci.getRestEndpoint().getClass()))
								{
									result = (String) rightMethod.invoke(wci.getRestEndpoint(),
											rightArguments.toArray());
									invoked = true;
								}
							}
						}
						
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
							boolean invoked = false;
							for (WebSocketConnectorInfo wci : this.webSocketEndPoint.getRegisteredEndpoints())
							{
								if (!invoked)
								{
									// TODO
									if (clazz.isAssignableFrom(wci.getRegisteredBundle()))
									{
										result = (String) rightMethod.invoke(wci.getWebSocketEndpoint(),
												rightArguments.toArray());
										invoked = true;
										
									}
									else if(clazz.isAssignableFrom(wci.getRestEndpoint().getClass()))
									{
										result = (String) rightMethod.invoke(wci.getRestEndpoint(),
												rightArguments.toArray());
										invoked = true;
									}
										
								}
							}
							
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
		
		try
		{
			for (WebSocketConnectorInfo wci : this.webSocketEndPoint.getRegisteredEndpoints())
			{
				ClassLoader clsI = wci.getRegisteredBundle().getClassLoader();
				Thread.currentThread().setContextClassLoader(clsI);
				for (String pack : wci.getEndpointPackages())
				{
					clazz = clsI.loadClass(pack);
					if (this.checkClass(clazz, endPoint))
					{
						return clazz;
					}
				}
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
		Annotation tempAnnotation = null;
		tempAnnotation = (Path) clazz.getAnnotation(Path.class);
		if (tempAnnotation == null)
			tempAnnotation = (WebSocketPath) clazz.getAnnotation(WebSocketPath.class);
		
		if (tempAnnotation != null)
		{
			String classAnnotation = null;
			if (tempAnnotation instanceof Path)
				classAnnotation = ((Path) tempAnnotation).value().toString();
			else if (tempAnnotation instanceof WebSocketPath)
				classAnnotation = ((WebSocketPath) tempAnnotation).value().toString();
			
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