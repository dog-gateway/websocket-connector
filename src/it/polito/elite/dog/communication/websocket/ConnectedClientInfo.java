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
	private String peerId;
	private HashMap<String, ArrayList<String>> subscriptions;
	private Connection connection;

	/**
	 * @param peerId
	 */
	public ConnectedClientInfo(String peerId, Connection connection)
	{
		this.peerId = peerId;
		this.connection = connection;
		this.subscriptions = new HashMap<String, ArrayList<String>>();
	}
	
	
	/**
	 * @param peerId
	 * @param subscriptions
	 */
	public ConnectedClientInfo(String peerId, Connection connection, HashMap<String, ArrayList<String>> subscriptions)
	{
		this.peerId = peerId;
		this.connection = connection;
		this.subscriptions = subscriptions;
	}


	/**
	 * @return the peerId
	 */
	public String getPeerId()
	{
		return peerId;
	}


	/**
	 * @param peerId the peerId to set
	 */
	public void setPeerId(String peerId)
	{
		this.peerId = peerId;
	}


	/**
	 * @return the subscriptions
	 */
	public HashMap<String, ArrayList<String>> getSubscriptions()
	{
		return subscriptions;
	}


	/**
	 * @param subscriptions the subscriptions to set
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
	 * @param connection the connection to set
	 */
	public void setConnection(Connection connection)
	{
		this.connection = connection;
	}
	
}
