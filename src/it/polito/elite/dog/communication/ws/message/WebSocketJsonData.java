package it.polito.elite.dog.communication.ws.message;

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