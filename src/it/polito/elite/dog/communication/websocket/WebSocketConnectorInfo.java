/**
 * 
 */
package it.polito.elite.dog.communication.websocket;

/**
 * @author luigi
 *
 */
public class WebSocketConnectorInfo
{
	private Class<?> registeredBundle;
	private Object restEndpoint;
	private Object webSocketEndpoint;
	private String[] endpointPackages;
	
	public WebSocketConnectorInfo(Class<?> registeredBundle, Object restEndpoint, Object webSocketEndpoint, String... endpointPackages)
	{
		this.registeredBundle = registeredBundle;
		this.restEndpoint = restEndpoint;
		this.webSocketEndpoint = webSocketEndpoint;
		this.endpointPackages = endpointPackages;
	}

	/**
	 * @return the registeredBundle
	 */
	public Class<?> getRegisteredBundle()
	{
		return registeredBundle;
	}

	/**
	 * @return the restEndpoint
	 */
	public Object getRestEndpoint()
	{
		return restEndpoint;
	}

	/**
	 * @return the webSocketEndpoint
	 */
	public Object getWebSocketEndpoint()
	{
		return webSocketEndpoint;
	}

	/**
	 * @return the endpointPackages
	 */
	public String[] getEndpointPackages()
	{
		return endpointPackages;
	}
}
