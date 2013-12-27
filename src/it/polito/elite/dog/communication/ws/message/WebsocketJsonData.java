package it.polito.elite.dog.communication.ws.message;

public class WebsocketJsonData{
    private String clientId;
    private String messageType;
    private String type;
	
    public String getClientId() {return clientId;}
    public void setClientId(String clientId) {this.clientId = clientId;}

    public String getMessageType() {return messageType;}
    public void setMessageType(String messageType) {this.messageType = messageType;}
    
    public String getType() {return type;}
    public void setType(String type) {this.type = type;}
    
}