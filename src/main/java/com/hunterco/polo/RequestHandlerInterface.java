package com.hunterco.polo;

public interface RequestHandlerInterface {
	public void processRequest(RequestMessage message) throws PoloMessagingException;
}
