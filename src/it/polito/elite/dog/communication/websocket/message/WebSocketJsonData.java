/*
 * Dog - WebSocket Endpoint
 * 
 * Copyright (c) 2013-2014 Teodoro Montanaro
 * contact: teo.montanaro@gmail.com
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

public class WebSocketJsonData
{
	// id of the client that ask to connect to the webSocket service (generated
	// by the server)
	private String clientId;
	// type of the message sent by the user (possible values: request, response,
	// info)
	private String messageType;
	// type of the request/response/info (possible values: notification,
	// notificationRegistration, notificationUnregistration, presentation)
	private String type;
	
	// get "clientId"
	public String getClientId()
	{
		return clientId;
	}
	
	// setters for "clientId"
	public void setClientId(String clientId)
	{
		this.clientId = clientId;
	}
	
	// get "messageType"
	public String getMessageType()
	{
		return messageType;
	}
	
	// setters for "messageType"
	public void setMessageType(String messageType)
	{
		this.messageType = messageType;
	}
	
	// get "Type"
	public String getType()
	{
		return type;
	}
	
	// setters for "Type"
	public void setType(String type)
	{
		this.type = type;
	}
	
}