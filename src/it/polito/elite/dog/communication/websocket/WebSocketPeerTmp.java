package it.polito.elite.dog.communication.websocket;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Represent a client (peer) connected to Dog WebSocket
 * 
 * @author derussis
 *
 */
public class WebSocketPeerTmp
{
	private String peerId;
	private HashMap<String, ArrayList<String>> subscriptions;
	
	/**
	 * @param peerId
	 */
	public WebSocketPeerTmp(String peerId)
	{
		this.peerId = peerId;
		this.subscriptions = new HashMap<String, ArrayList<String>>();
	}
	
	
	/**
	 * @param peerId
	 * @param subscriptions
	 */
	public WebSocketPeerTmp(String peerId, HashMap<String, ArrayList<String>> subscriptions)
	{
		this.peerId = peerId;
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
	
}
