package it.polito.elite.dog.communication.websocket;

import java.io.IOException;
import java.util.Map;

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
	
	public boolean isWebSocketAvailable();
	
	public Map<String, ConnectedClientInfo> getConnectedClients();
	
	public void sendMessage(String message, String recipient) throws IOException;
}
