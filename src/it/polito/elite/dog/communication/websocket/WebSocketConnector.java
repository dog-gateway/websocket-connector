package it.polito.elite.dog.communication.websocket;

public interface WebSocketConnector
{
	/**
	 * Register a new endpoint bundle for communicating data on WebSocket
	 * 
	 * @param webSocketEndpoint
	 *            the class corresponding to the endpoint to register
	 * @param restEndpoint
	 *            the instance of the corresponding REST endpoint
	 * @param packages
	 *            zero or more Java packages associated to the endpoint to
	 *            register
	 */
	public void registerEndpoint(Class<?> webSocketEndpoint, Object restEndpoint, String... packages);
}
