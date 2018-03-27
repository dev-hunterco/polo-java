package com.hunterco.polo;

public interface ResponseActionInterface {
	public void done(ResponseMessage msg) throws PoloMessagingException;
	public void dismiss(ResponseMessage msg) throws PoloMessagingException;
}
