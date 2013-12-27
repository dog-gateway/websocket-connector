package it.polito.elite.dog.communication.ws.message;

import java.util.HashMap;

public class WebsocketJsonNotification extends WebsocketJsonData{

	private HashMap<String, String> notification;
	
	public HashMap<String, String> getNotification() {return notification;}
    public void setNotification(HashMap<String, String> notification) {this.notification = notification;}
  
   
}