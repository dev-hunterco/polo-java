package com.hunterco.polo;

public interface RequestActionInterface {
	public String reply(RequestMessage msg, Object data) throws PoloMessagingException;
	public String replyError(RequestMessage msg, Object error) throws PoloMessagingException;
	public void dismiss(RequestMessage msg);
	public String forward(RequestMessage msg, String destination) throws PoloMessagingException;
	public void done(RequestMessage requestMessage) throws PoloMessagingException;
}
