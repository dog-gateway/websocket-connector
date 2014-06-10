/*
 * Dog - WebSocket Connector
 * 
 * Copyright (c) 2013-2014 Teodoro Montanaro and Luigi De Russis
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
package it.polito.elite.dog.communication.websocket.message;

/**
 * 
 * @author <a href="mailto:teo.montanaro@gmail.com">Teodoro Montanaro</a>
 * @author <a href="mailto:luigi.derussis@polito.it">Luigi De Russis</a>
 * @see <a href="http://elite.polito.it">http://elite.polito.it</a>
 * 
 */
public class InvocationResult
{
	// result of action performed by the server
	private String result;
	
	/**
	 * @return the result
	 */
	public String getResult()
	{
		return result;
	}
	
	/**
	 * @param result
	 *            the result to set
	 */
	public void setResult(String result)
	{
		this.result = result;
	}
	
}