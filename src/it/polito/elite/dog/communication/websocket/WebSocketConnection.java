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

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import it.polito.elite.dog.communication.websocket.annotation.WebSocketPath;
import it.polito.elite.dog.communication.websocket.info.WebSocketConnectorInfo;
import it.polito.elite.dog.communication.websocket.message.Presentation;
import it.polito.elite.dog.communication.websocket.message.InvocationResult;
import it.polito.elite.dog.communication.websocket.message.Request;
import it.polito.elite.dog.communication.websocket.message.Response;
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

import org.eclipse.jetty.websocket.WebSocket;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

/**
 * Class that handles the effective communication via WebSocket.
 * 
 * @author <a href="mailto:teo.montanaro@gmail.com">Teodoro Montanaro</a>
 * @author <a href="mailto:luigi.derussis@polito.it">Luigi De Russis</a>
 * @see <a href="http://elite.polito.it">http://elite.polito.it</a>
 * 
 */
public class WebSocketConnection implements WebSocket.OnTextMessage
{
	// number of initial parameters (e.g., URI or path) in the endpoint that
	// indicates the class in which the requested action will be performed
	// (devices, environment, rules, ...)
	private int numberOfClassParameters;
	
	// initial parameters (e.g., URI or path) in the endPoint that indicate the
	// class in which the requested action will be performed (devices,
	// environment, rules, ...)
	private String[] endPointParts;
	
	// reference for the WebSocketEndPoint
	private WebSocketEndPoint webSocketEndPoint;
	
	// connection used to send and receive messages
	private Connection connection;
	
	// the service logger
	private LogHelper logger;
	
	// the bundle context reference
	private BundleContext context;
	
	// the instance-level mapper
	private ObjectMapper mapper;
	
	/**
	 * Constructor, called by the method {@link doWebSocketConnect} of
	 * {@link WebSocketEndPoint} (it comes from {@link WebSocketServlet}).
	 * 
	 * @param context
	 *            the OSGi context
	 * @param webSocketEndPoint
	 *            the caller
	 */
	public WebSocketConnection(BundleContext context, WebSocketEndPoint webSocketEndPoint)
	{
		this.webSocketEndPoint = webSocketEndPoint;
		
		// init the number of initial parameters to 0
		this.numberOfClassParameters = 0;
		
		// store the bundle context
		this.context = context;
		
		// initialize the instance-wide object mapper for JSON
		this.mapper = new ObjectMapper();
		// set the mapper pretty printing
		this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
		// avoid empty arrays and null values
		this.mapper.configure(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, false);
		this.mapper.setSerializationInclusion(Include.NON_NULL);
		
		// init the logger
		this.logger = new LogHelper(this.context);
		
		// log the activation - debug
		this.logger.log(LogService.LOG_DEBUG, "Activated....");
	}
	
	/**
	 * Method called when a client opens the connection
	 * 
	 * @param connection
	 *            the {@link Connection} instance
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jetty.websocket.WebSocket#onOpen(org.eclipse.jetty.websocket
	 * .WebSocket.Connection)
	 */
	@Override
	public void onOpen(Connection connection)
	{
		// set the maximum time of the connection to the maximum available
		connection.setMaxIdleTime(Integer.MAX_VALUE);
		// init the connection
		this.connection = connection;
		
		// add the client to the list of users connected to the system
		this.webSocketEndPoint.addUser(this.toString(), connection);
		
		// debug
		this.logger.log(LogService.LOG_DEBUG, "Connection Protocol: " + connection.getProtocol());
		
		if (this.connection.isOpen())
		{
			try
			{
				// prepare the presentation message
				Presentation presentation = new Presentation();
				// extract the clientId from the instance name by taking the
				// last part of the connection instance (after the @), via the
				// toString() method
				presentation.setClientId(this.toString());
				String presentationMessage = this.mapper.writeValueAsString(presentation);
				// send the starting message (presentation) with the clientId
				// generated
				this.connection.sendMessage(presentationMessage);
			}
			catch (IOException e)
			{
				// it was not possible to open the connection
				this.logger.log(LogService.LOG_ERROR,
						"Impossible to open the connection with client " + this.toString(), e);
				
				// if something goes wrong, remove the client from the connected
				// clients list
				this.webSocketEndPoint.removeUser(this.toString());
			}
		}
	}
	
	/**
	 * Method called when a client closes the connection.
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jetty.websocket.WebSocket#onClose(int, java.lang.String)
	 */
	@Override
	public void onClose(int statusCode, String reason)
	{
		// remove the client from the list of connected clients
		this.webSocketEndPoint.removeUser(this.toString());
		
		// TODO remove the notification registration of the client
	}
	
	/**
	 * Method called when a client sends a message to the server.
	 * 
	 * @param data
	 *            Received message
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jetty.websocket.WebSocket.OnTextMessage#onMessage(java.lang
	 * .String)
	 */
	@Override
	public void onMessage(String data)
	{
		// if we actually receive data we process them
		if (!data.isEmpty())
		{
			// variable used to parse all the JSON data
			Request webSocketReceivedData;
			try
			{
				// parse the received JSON
				webSocketReceivedData = this.mapper.readValue(data, Request.class);
				// debug
				this.logger.log(LogService.LOG_DEBUG,
						"Received data: " + this.mapper.writeValueAsString(webSocketReceivedData));
				
				String clientId = webSocketReceivedData.getClientId();
				String sequenceNumber = webSocketReceivedData.getSequenceNumber();
				String messageType = webSocketReceivedData.getMessageType();
				String action = webSocketReceivedData.getAction();
				String endPoint = webSocketReceivedData.getEndPoint();
				String parameters = webSocketReceivedData.getParameters();
				
				// check if the user is the right one and if the message is a
				// request
				if ((clientId != null) && clientId.equals(this.toString()) && (messageType.equals("request")))
				{
					// set to 0 the variable by which we usually count the
					// initial parts of path that indicates the class
					this.numberOfClassParameters = 0;
					
					String result;
					try
					{
						// invoke the "right" method given the endpoint, action,
						// parameters and client ID
						result = this.invokeMethodByAnnotation(endPoint, action, parameters, clientId);
						
						// debug
						this.logger.log(LogService.LOG_DEBUG, "Sending data: " + result);
						
						// transform the result in a JSON message
						Response jsonResponse = new Response();
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
							InvocationResult jsonResult = new InvocationResult();
							jsonResult.setResult("Something went wrong");
							result = this.mapper.writeValueAsString(jsonResult);
						}
						
						// if the result is not a JSON object, the readTree
						// method will generate an exception that we intercept
						// to insert the result as a string in the response
						Object resultObject;
						try
						{
							resultObject = this.mapper.readTree(result);
						}
						catch (Exception e)
						{
							InvocationResult jsonResult = new InvocationResult();
							jsonResult.setResult(result);
							result = this.mapper.writeValueAsString(jsonResult);
							resultObject = this.mapper.readTree(result);
						}
						// set the response, finally
						jsonResponse.setResponse(resultObject);
						
						String response = this.mapper.writeValueAsString(jsonResponse);
						// send the message just created
						this.connection.sendMessage(response);
					}
					catch (Exception e)
					{
						this.logger.log(LogService.LOG_ERROR, "Exception in invoking some REST/WebSocket API methods",
								e);
					}
				}
				else
				{
					// the request has not the right parameters (clientId and
					// messageType)
					// the error message is sent in JSON format
					Response jsonResponse = new Response();
					if (!clientId.isEmpty())
						jsonResponse.setClientId(clientId);
					if (!sequenceNumber.isEmpty())
						jsonResponse.setSequenceNumber(sequenceNumber);
					jsonResponse.setMessageType("response");
					jsonResponse.setResponse("You forgot to send the clientId or the message type is not a request");
					String response = this.mapper.writeValueAsString(jsonResponse);
					// send the error message
					this.connection.sendMessage(response);
				}
			}
			catch (IOException ex)
			{
				this.logger.log(LogService.LOG_ERROR, "Error in handling messages", ex);
			}
		}
	}
	
	/**
	 * Invoke the right method from the right class (DeviceRESTApi,
	 * EnvironmentRESTApi, etc.)
	 * 
	 * @param endPoint
	 *            {@link Path} specified as annotation on the method that has we
	 *            want to invoke
	 * @param action
	 *            action (GET, POST, PUT, DELETE) that is specified as
	 *            annotation on the method that we want to invoke
	 * @param parameters
	 *            parameters that has to be passed to the method that we want to
	 *            invoke
	 * @param clientId
	 *            the client unique identifier
	 * @return a {@link String} result containing the result generated by the
	 *         method just invoked
	 * 
	 * @throws ClassNotFoundException
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws IllegalArgumentException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws JsonGenerationException
	 * @throws JsonMappingException
	 */
	private String invokeMethodByAnnotation(String endPoint, String action, String parameters, String clientId)
			throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException,
			InstantiationException, IllegalAccessException, InvocationTargetException, JsonGenerationException,
			JsonMappingException
	{
		// first of all we choose the class that contains the method we want to
		// invoke
		// init
		Class<?> clazz = null;
		String result = "";
		
		// then we divide the endPoint in different parts (in the
		// path they are separated by "/")
		if (endPoint.startsWith("/"))
		{
			endPoint = endPoint.replaceFirst("/", "");
		}
		this.endPointParts = endPoint.split("/");
		
		// try to call the method that identify the right class
		ClassLoader cls = this.webSocketEndPoint.getClass().getClassLoader();
		clazz = this.getRightClass(endPoint, cls);
		
		if (clazz != null)
		{
			// now we scroll trough all available methods looking for the
			// annotations present
			// if there are both "/api/v1/devices/status" and
			// "/api/v1/devices/{device-id}" we have to take care to choose the
			// right one
			Method[] methods = clazz.getDeclaredMethods();
			Method rightMethod = null;
			List<String> rightArguments = new ArrayList<String>();
			for (Method method : methods)
			{
				// we start to check for the annotation from the point to which
				// the getRightClass arrived, so we copy the number indicating
				// the part of path already analized (for example after
				// api/v1/devices/)
				int numberOfAnalizedPartsOfEndPoint = this.numberOfClassParameters;
				// we check if the analyzed method defined the right action
				// (GET, PUT, POST or DELETE)
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
					// right method (with the proper annotation)
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
									// "/api/v1/devides/status" and
									// "/api/v1/devices/{device-id}" we have to
									// take care to choose the right one
									if (methodAnnotationParts[k]
											.compareTo(this.endPointParts[numberOfAnalizedPartsOfEndPoint]) != 0)
									{
										// if I have already chosen a method it
										// means that a method without a
										// parameter exists, because otherwise
										// I'd say that it is the right
										// method only at the end (when I've
										// analyzed all the parameters)
										if (rightMethod == null)
										{
											// if "{" or "}" is present it means
											// that it is a parameter
											// so we insert it in the list of
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
											// that it is the wrong method or
											// that we have already chosen the
											// method without parameters
											// (/api/v1/devices/status instead
											// of /api/v1/devices/{device-id})
											break;
									}
									numberOfAnalizedPartsOfEndPoint++;
								}
								
								if (numberOfAnalizedPartsOfEndPoint == (this.endPointParts.length))
								{
									// if we arrived at the end of Path and the
									// dimension of endPoint is equal to the
									// dimension of annotation it should be the
									// right method but we have to check if the
									// output of the method is the right one: if
									// there is not a "Produces" annotation we
									// assume that it produces JSON
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
							// if the endPoint does not have other parts after
							// the path used to select the class
							// (/api/v1/devices or /api/v1/environment) it means
							// that the method we are looking for has to answer
							// to the "root" path /api/v1/devices or
							// /api/v1/environment
							if ((this.endPointParts.length - numberOfAnalizedPartsOfEndPoint) == 0)
							{
								// we have to check if the output of the method
								// is the right one: if there is not a
								// "Produces" annotation we assumes that it
								// produces JSON
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
			// the parameters are specified as a JSON string
			if ((parameters != null) && (!parameters.isEmpty()))
			{
				rightArguments.add(new String(parameters));
			}
			// now we should have selected a method...
			if (rightMethod != null)
			{
				// check how many parameters the method needs and if we need at
				// least one more parameter we insert an empty one (there are
				// methods that ask for a parameter
				// that could be null)
				Annotation[][] parameterAnnotations = rightMethod.getParameterAnnotations();
				int numMethodParameters = parameterAnnotations.length;
				if (numMethodParameters >= rightArguments.size() + 1)
				{
					rightArguments.add(null);
				}
				// if the method will return a String value we save the result,
				// otherwise we have to intercept the WebApplicationException
				// generated to give the result of the action
				try
				{
					boolean invoked = false;
					// depending on the class selected we have to use the
					// right reference (deviceRESTApi, environmentRESTApi,
					// etc.)
					for (WebSocketConnectorInfo wci : this.webSocketEndPoint.getRegisteredEndpoints())
					{
						if (!invoked)
						{
							if (clazz.isAssignableFrom(wci.getRegisteredBundle()))
							{
								// add the client ID for notifications
								// registration
								if ((clientId != null) && (!clientId.isEmpty()))
								{
									if (rightArguments.size() == rightMethod.getParameterAnnotations().length)
									{
										rightArguments.remove(rightArguments.size() - 1);
									}
									rightArguments.add(new String(clientId));
								}
								result = (String) rightMethod.invoke(wci.getWebSocketEndpoint(),
										rightArguments.toArray());
								invoked = true;
							}
							else if (clazz.isAssignableFrom(wci.getRestEndpoint().getClass()))
							{
								result = (String) rightMethod.invoke(wci.getRestEndpoint(), rightArguments.toArray());
								invoked = true;
							}
						}
					}
					
				}
				catch (Exception e)
				{
					String resultMessage = "";
					
					if (rightMethod.getGenericReturnType().equals(String.class))
					{
						// even if the method should return a String, if the
						// device or the environment doesn't exist, it return a
						// 404 Not Found message as an exception, so we have to
						// handle the exception
						resultMessage = "The requested resource was not found";
					}
					else
					{
						// here we intercept the Exception generated by methods
						// that does not return a String, to know if the
						// operation was executed correctly or not
						if (e instanceof InvocationTargetException)
						{
							// even if the InvocationTargetException is the
							// only exception generated, the right one
							// would be a WebApplicationException, so we
							// manage that exception too
							InvocationTargetException exception = (InvocationTargetException) e;
							resultMessage = exception.getTargetException().getMessage();
							
						}
						else if (e instanceof WebApplicationException)
						{
							WebApplicationException exception = (WebApplicationException) e;
							resultMessage = exception.getResponse().toString();
						}
						
						if (resultMessage.toLowerCase().contains("ok")
								|| resultMessage.toLowerCase().contains("created"))
						{
							resultMessage = "Method " + rightMethod + " executed successfully";
						}
						else
							resultMessage = "Method " + rightMethod + " execution failed";
					}
					
					// send the result as a JSON
					InvocationResult jsonResult = new InvocationResult();
					jsonResult.setResult(resultMessage);
					try
					{
						return this.mapper.writeValueAsString(jsonResult);
					}
					catch (IOException ex)
					{
						// log
						this.logger.log(LogService.LOG_WARNING, "Exception in parsing the message: " + resultMessage,
								ex);
						// manually do the same work of Jackson...
						return "{\"result\":\"" + resultMessage + "\"}";
					}
				}
			}
		}
		// if something goes wrong we return an empty string
		return result;
	}
	
	/**
	 * Get the class containing the annotation we are looking for.<br/>
	 * At this level we look for the first part of endpoint (e.g.,
	 * api/v1/devices), only
	 * 
	 * @param endPoint
	 *            path for which we are looking for (that denote the method that
	 *            has to be invoke)
	 * @param cls
	 *            the bundle ClassLoader
	 * @return a {@link Class<?>} object containing the right class response to
	 *         the status API
	 * 
	 */
	private Class<?> getRightClass(String endPoint, ClassLoader cls) throws ClassNotFoundException
	{
		Class<?> clazz = null;
		
		try
		{
			// for each registered WebSocket endpoints...
			for (WebSocketConnectorInfo wci : this.webSocketEndPoint.getRegisteredEndpoints())
			{
				// set the classloader to the one of the selected endpoint...
				ClassLoader clsI = wci.getRegisteredBundle().getClassLoader();
				Thread.currentThread().setContextClassLoader(clsI);
				// for each registered packages (from the other WebSocket
				// bundles) try to load the desired class
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
			this.logger.log(LogService.LOG_ERROR, "Impossible to find the required endpoint in the given classes", e);
		}
		finally
		{
			// restore the bundle classloader
			Thread.currentThread().setContextClassLoader(cls);
		}
		
		return null;
	}
	
	/**
	 * Check if the class indicated contains the method annotated by the desired
	 * endpoint
	 * 
	 * @param clazz
	 *            Class in which we have to look for the desired method
	 * @param endPoint
	 *            Path for which we are looking for (that denote the method that
	 *            has to be invoke)
	 * @return true if the given class contains the method, false otherwise
	 * 
	 */
	private boolean checkClass(Class<?> clazz, String endPoint) throws ClassNotFoundException
	{
		
		boolean found = true;
		// we divide the class annotation in different parts (that are separated
		// by "/") and we count the number of parameters obtained (so we know
		// when we have to stop the comparison)
		Annotation tempAnnotation = null;
		// check for Path annotation (REST)
		tempAnnotation = (Path) clazz.getAnnotation(Path.class);
		
		// otherwise, check for WebSocketPath annotation (defined here)
		if (tempAnnotation == null)
			tempAnnotation = (WebSocketPath) clazz.getAnnotation(WebSocketPath.class);
		
		// found?
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
					// method from the right point of endpoint
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
	 * removing the class name.
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