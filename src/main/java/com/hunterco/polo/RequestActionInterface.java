package com.hunterco.polo;

public interface RequestActionInterface {
	public void reply(RequestMessage msg, Object data) throws PoloMessagingException;
	public void replyError(RequestMessage msg, Object error) throws PoloMessagingException;
	public void dismiss(RequestMessage msg);
	public String forward(RequestMessage msg, String destination) throws PoloMessagingException;
}
