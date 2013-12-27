package it.polito.elite.dog.communication.ws.message;

public class WebsocketJsonRequest extends WebsocketJsonData{
    private String sequenceNumber;
    private String action;
    private String endPoint;
    private String parameters;
    private Object response;

    public String getSequenceNumber() {return sequenceNumber;}
    public void setSequenceNumber(String sequenceNumber) {this.sequenceNumber = sequenceNumber;}
    
    public String getAction() {return action;}
    public void setAction(String action) {this.action = action;}
    
    public String getEndPoint() {return endPoint;}
    public void setEndPoint(String endPoint) {this.endPoint = endPoint;}
    
    public String getParameters() {return parameters;}
    public void setParameters(String parameters) {this.parameters = parameters;}
    
    public Object getResponse() {return response;}
    public void setResponse(Object response) {this.response = response;}
    
}