package it.polito.elite.dog.communication.ws.message;

import java.util.HashMap;

public class WebSocketJsonNotification extends WebSocketJsonData
{
	// content of the notification
	private HashMap<String, String> notification;
	
	// get notification
	public HashMap<String, String> getNotification()
	{
		return notification;
	}
	
	// setter for notification
	public void setNotification(HashMap<String, String> notification)
	{
		this.notification = notification;
	}
	
}