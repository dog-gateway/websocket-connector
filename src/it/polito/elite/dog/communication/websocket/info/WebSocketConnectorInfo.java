/*
 * Dog - WebSocket Connector
 * 
 * Copyright (c) 2014 Luigi De Russis
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
package it.polito.elite.dog.communication.websocket.info;

/**
 * Represent the information needed to let other WebSocket bundle use this
 * general connector.
 * 
 * @author <a href="mailto:luigi.derussis@polito.it">Luigi De Russis</a>
 * @see <a href="http://elite.polito.it">http://elite.polito.it</a>
 * 
 */
public class WebSocketConnectorInfo
{
	// the class of the registered bundles
	private Class<?> registeredBundle;
	// the REST endpoint to use via WebSocket
	private Object restEndpoint;
	// the registered bundle
	private Object webSocketEndpoint;
	// the packages to access via WebSocket
	private String[] endpointPackages;
	
	/**
	 * Base constructor. It self-calculate all the other needed parameters.
	 * 
	 * @param webSocketEndpoint
	 * @param restEndpoint
	 */
	public WebSocketConnectorInfo(Object webSocketEndpoint, Object restEndpoint)
	{
		this.registeredBundle = webSocketEndpoint.getClass();
		this.restEndpoint = restEndpoint;
		this.webSocketEndpoint = webSocketEndpoint;
		this.endpointPackages = new String[2];
		this.endpointPackages[0] = this.registeredBundle.getName();
		this.endpointPackages[1] = this.restEndpoint.getClass().getInterfaces()[0].getName();
	}
	
	/**
	 * Full constructor.
	 * 
	 * @param registeredBundle
	 *            the class of the registered bundle
	 * @param restEndpoint
	 *            the REST endpoint to use via WebSocket
	 * @param webSocketEndpoint
	 *            the registered bundle
	 * @param endpointPackages
	 *            the packages to access via WebSocket
	 */
	public WebSocketConnectorInfo(Class<?> registeredBundle, Object restEndpoint, Object webSocketEndpoint,
			String... endpointPackages)
	{
		this.registeredBundle = registeredBundle;
		this.restEndpoint = restEndpoint;
		this.webSocketEndpoint = webSocketEndpoint;
		this.endpointPackages = endpointPackages;
	}
	
	/**
	 * @return the registeredBundle
	 */
	public Class<?> getRegisteredBundle()
	{
		return registeredBundle;
	}
	
	/**
	 * @return the restEndpoint
	 */
	public Object getRestEndpoint()
	{
		return restEndpoint;
	}
	
	/**
	 * @return the webSocketEndpoint
	 */
	public Object getWebSocketEndpoint()
	{
		return webSocketEndpoint;
	}
	
	/**
	 * @return the endpointPackages
	 */
	public String[] getEndpointPackages()
	{
		return endpointPackages;
	}
}
