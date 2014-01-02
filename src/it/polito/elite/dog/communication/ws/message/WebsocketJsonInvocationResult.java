package it.polito.elite.dog.communication.ws.message;

public class WebsocketJsonInvocationResult
{
	// result of action performed by the server
	private String result;
	
	// get "result"
	public String getResult()
	{
		return result;
	}
	
	// setter for "result"
	public void setResult(String result)
	{
		this.result = result;
	}
}