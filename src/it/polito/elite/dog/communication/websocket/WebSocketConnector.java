package it.polito.elite.dog.communication.websocket;

public interface WebSocketConnector
{
	public void registerEndpoint(Class<?> webSocketEndpoint, Object restEndpoint, String... packages);
}
