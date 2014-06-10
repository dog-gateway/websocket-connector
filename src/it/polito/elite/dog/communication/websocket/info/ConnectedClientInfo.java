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

import it.polito.elite.dog.communication.websocket.WebSocketConnection;

import org.eclipse.jetty.websocket.WebSocket.Connection;

/**
 * Represent a client connected to the Dog WebSocket server.
 * 
 * @author <a href="mailto:luigi.derussis@polito.it">Luigi De Russis</a>
 * @see <a href="http://elite.polito.it">http://elite.polito.it</a>
 * 
 */
public class ConnectedClientInfo
{
	// the client unique identifier
	private String clientId;
	// the WebSocket connection
	private Connection connection;
	
	/**
	 * Constructor
	 * 
	 * @param clientId
	 * @param connection
	 */
	public ConnectedClientInfo(String clientId, Connection connection)
	{
		this.clientId = clientId;
		this.connection = connection;
	}
	
	/**
	 * @return the clientId
	 */
	public String getClientId()
	{
		return clientId;
	}
	
	/**
	 * @param clientId
	 *            the clientId to set
	 */
	protected void setClientId(String clientId)
	{
		this.clientId = clientId;
	}
	
	/**
	 * @return the connection
	 */
	public Connection getConnection()
	{
		return connection;
	}
	
	/**
	 * @param connection
	 *            the connection to set
	 */
	public void setConnection(Connection connection)
	{
		this.connection = connection;
	}
	
	/**
	 * Give the {@link WebSocketConnection} client ID by taking the instance
	 * string and removing the class name
	 */
	@Override
	public String toString()
	{
		StringBuilder result = new StringBuilder();
		
		result.append(this.clientId.substring(this.clientId.indexOf('@') + 1));
		
		return result.toString();
	}
	
}
