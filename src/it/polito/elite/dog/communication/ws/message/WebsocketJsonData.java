package it.polito.elite.dog.communication.ws.message;

import java.util.HashMap;
import java.util.Set;

public class WebsocketJsonData{
    private String clientId;
    private String sequenceNumber;
    private String messageType;
    private String type;
    private String action;
    private String endPoint;
    private String parameters;
    private Object response;
    private String notifications;
	private HashMap<String, String> notification;
	

    public String getClientId() {return clientId;}
    public void setClientId(String clientId) {this.clientId = clientId;}
    
    public String getSequenceNumber() {return sequenceNumber;}
    public void setSequenceNumber(String sequenceNumber) {this.sequenceNumber = sequenceNumber;}
    
    public String getMessageType() {return messageType;}
    public void setMessageType(String messageType) {this.messageType = messageType;}
    
    public String getType() {return type;}
    public void setType(String type) {this.type = type;}
    
    public String getAction() {return action;}
    public void setAction(String action) {this.action = action;}
    
    public String getEndPoint() {return endPoint;}
    public void setEndPoint(String endPoint) {this.endPoint = endPoint;}
    
    public String getParameters() {return parameters;}
    public void setParameters(String parameters) {this.parameters = parameters;}
    
    public Object getResponse() {return response;}
    public void setResponse(Object response) {this.response = response;}
    
    public String getNotifications() {return notifications;}
    public void setNotifications(String notifications) {this.notifications = notifications;}
    
	public HashMap<String, String> getNotification() {return notification;}
    public void setNotification(HashMap<String, String> notification) {this.notification = notification;}
  

    @Override
    public String toString() {
    	String result = "WebsoketJsonData [clientId =" + this.clientId + ", sequenceNumber=" + this.sequenceNumber + ", messageType=" + this.messageType + ", type="
                            + this.type + ", action=" + this.action + ", endPoint=" + this.endPoint + ", parameters=" + this.parameters + ", response=" + this.response + ", notifications=" + this.notifications;
    	
    	if (this.notification != null)
    	{
	    	Set<String> keySet = this.notification.keySet();
	    	result = ", notification= [";
	    	for(Object key:keySet)
	    	{
				result+=key+"= "+this.notification.get(key)+",";
	    	}
	    	result+="] ]";
    	}
    	else
    	{
	    	result+="]";
    	}
	            return result;
    }
  
   
}