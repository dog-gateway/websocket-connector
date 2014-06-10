package it.polito.elite.dog.communication.websocket;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;;

@Documented
@Target(value={ElementType.TYPE,ElementType.METHOD})
@Retention(value=RetentionPolicy.RUNTIME)
public @interface WebSocketPath
{
	public String value();
	
}
