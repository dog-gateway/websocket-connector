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
// TODO define equals and hashCode
public class ConnectedClientInfo
{
	private String clientId;
	private HashMap<String, ArrayList<String>> subscriptions;
	private Connection connection;
	
	/**
	 * @param clientId
	 */
	public ConnectedClientInfo(String clientId, Connection connection)
	{
		this.clientId = clientId;
		this.connection = connection;
		this.subscriptions = new HashMap<String, ArrayList<String>>();
	}
	
	/**
	 * @param peerId
	 * @param subscriptions
	 */
	public ConnectedClientInfo(String peerId, Connection connection, HashMap<String, ArrayList<String>> subscriptions)
	{
		this.clientId = peerId;
		this.connection = connection;
		this.subscriptions = subscriptions;
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
	public void setClientId(String clientId)
	{
		this.clientId = clientId;
	}
	
	/**
	 * @return the subscriptions
	 */
	public HashMap<String, ArrayList<String>> getSubscriptions()
	{
		return subscriptions;
	}
	
	/**
	 * @param subscriptions
	 *            the subscriptions to set
	 */
	public void setSubscriptions(HashMap<String, ArrayList<String>> subscriptions)
	{
		this.subscriptions = subscriptions;
	}
	
	/**
	 * @return the connection
	 */
	public Connection getConnection()
	{
		return connection;
	}
	
	/**
	 * @param connection
	 *            the connection to set
	 */
	public void setConnection(Connection connection)
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
