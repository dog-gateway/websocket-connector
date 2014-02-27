/*
 * Dog - WebSocket Endpoint
 * 
 * Copyright (c) 2013-2014 Teodoro Montanaro
 * contact: teo.montanaro@gmail.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package it.polito.elite.dog.communication.ws.message;

public class WebSocketJsonInvocationResult
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