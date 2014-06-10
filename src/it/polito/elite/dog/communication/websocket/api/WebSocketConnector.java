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
package it.polito.elite.dog.communication.websocket.api;

import java.io.IOException;
import java.util.Set;

/**
 * Interface to interact with the WebSocket Connector.
 * 
 * @author <a href="mailto:luigi.derussis@polito.it">Luigi De Russis</a>
 * @see <a href="http://elite.polito.it">http://elite.polito.it</a>
 * 
 */
public interface WebSocketConnector
{
	/**
	 * Register a new endpoint for communicating data on WebSocket. Other
	 * dependencies are self-calculated. Please, use the other interface method
	 * to specify other parameters.
	 * 
	 * @param webSocketEndpoint
	 *            the instance of the bundle endpoint to register
	 * @param restEndpoint
	 *            the instance of the corresponding REST endpoint
	 */
	public void registerEndpoint(Object webSocketEndpoint, Object restEndpoint);
	
	/**
	 * Register a new endpoint for communicating data on WebSocket.
	 * 
	 * @param webSocketEndpoint
	 *            the instance of the bundle endpoint to register
	 * @param restEndpoint
	 *            the instance of the corresponding REST endpoint
	 * @param packages
	 *            zero or more Java packages associated to the endpoint to
	 *            register
	 */
	public void registerEndpoint(Object webSocketEndpoint, Object restEndpoint, String... packages);
	
	/**
	 * Check if the WebSocket connection is available
	 * 
	 * @return true if the connection has been created, false otherwise
	 */
	public boolean isWebSocketAvailable();
	
	/**
	 * Get a list of connected applications (clients)
	 * 
	 * @return a {@link Set} of {@link String} representing the clients IDs
	 */
	public Set<String> getConnectedClients();
	
	/**
	 * Send a message on the WebSocket connection
	 * 
	 * @param message
	 *            the message to send
	 * @param recipient
	 *            the destination client ID
	 * @throws IOException
	 *             if the sending cannot be performed
	 */
	public void sendMessage(String message, String recipient) throws IOException;
}
