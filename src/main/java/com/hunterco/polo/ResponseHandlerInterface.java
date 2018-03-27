package com.hunterco.polo;

public interface ResponseHandlerInterface {
	public void processResponse(ResponseMessage message) throws PoloMessagingException;
}
