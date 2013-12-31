package it.polito.elite.dog.communication.ws.message;

import java.util.ArrayList;

public class WebsocketJsonInvocationResult{
    private String result;
    private ArrayList<String> succededUnregistrations;
    private ArrayList<String> failedUnregistrations;
    private Object unregistrationResults;
	
    public String getResult() {return result;}
    public void setResult(String result) {this.result = result;}
    
    public ArrayList<String> getSuccededUnregistrations() {return succededUnregistrations;}
    public void setSuccededUnregistrations(ArrayList<String> succededUnregistrations) {this.succededUnregistrations = succededUnregistrations;}
    
    public ArrayList<String> getFailedUnregistrations() {return failedUnregistrations;}
    public void setFailedUnregistrations(ArrayList<String> failedUnregistrations) {this.failedUnregistrations = failedUnregistrations;}
    

    public Object getUnregistrationResults() {return unregistrationResults;}
    public void setUnregistrationResults(Object unregistrationResults) {this.unregistrationResults = unregistrationResults;}
    
}