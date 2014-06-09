package it.polito.elite.dog.communication.websocket;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.jetty.websocket.WebSocket.Connection;

/**
 * Represent a client (peer) connected to Dog WebSocket
 * 
 * @author derussis
 * 
 */
public class ConnectedClientInfo
{
	private String clientId;
	private Connection connection;
	
	/**
	 * @param clientId
	 */
	public ConnectedClientInfo(String clientId, Connection connection)
	{
		this.clientId = clientId;
		this.connection = connection;
	}
	
	/**
	 * @param peerId
	 * @param subscriptions
	 */
	public ConnectedClientInfo(String peerId, Connection connection, HashMap<String, ArrayList<String>> subscriptions)
	{
		this.clientId = peerId;
		this.connection = connection;
	}
	
	/**
	 * @return the clientId
	 */
	public String getClientId()
	{
		return clientId;
	}
	
	/**
	 * @param clientId
	 *            the peerId to set
	 */
	protected void setClientId(String clientId)
	{
		this.clientId = clientId;
	}
	
	/**
	 * @return the connection
	 */
	protected Connection getConnection()
	{
		return connection;
	}
	
	/**
	 * @param connection
	 *            the connection to set
	 */
	protected void setConnection(Connection connection)
	{
		this.connection = connection;
	}
	
	/**
	 * Give the {@link WebSocketConnection} client id by taking the instance string and
	 * removing the class name
	 */
	@Override
	public String toString()
	{
		StringBuilder result = new StringBuilder();
		
		result.append(this.clientId.substring(this.clientId.indexOf('@') + 1));
		
		return result.toString();
	}
	
}
