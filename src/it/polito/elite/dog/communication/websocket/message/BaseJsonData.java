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
package it.polito.elite.dog.communication.websocket.message;

/**
 * Define the base JSON structure for WebSocket communications.
 * 
 * @author <a href="mailto:teo.montanaro@gmail.com">Teodoro Montanaro</a>
 * @author <a href="mailto:luigi.derussis@polito.it">Luigi De Russis</a>
 * @see <a href="http://elite.polito.it">http://elite.polito.it</a>
 * 
 */
public class BaseJsonData
{
	// ID of the client that ask to connect to the webSocket service (generated
	// by the server)
	private String clientId;
	// type of the message sent by the user (possible values: request, response,
	// error, notification, presentation)
	private String messageType;
	
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
	public void setClientId(String clientId)
	{
		this.clientId = clientId;
	}
	
	/**
	 * @return the messageType
	 */
	public String getMessageType()
	{
		return messageType;
	}
	
	/**
	 * @param messageType
	 *            the messageType to set
	 */
	public void setMessageType(String messageType)
	{
		this.messageType = messageType;
	}
	
}