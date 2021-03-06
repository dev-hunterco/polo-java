package com.hunterco.polo;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class RequestMessage extends PoloMessage {
	@JsonIgnore
	private transient RequestActionInterface actionHandler;
	
	private ApplicationInfo forwardedBy;
	
	public RequestMessage() {
		super();
		this.setType(MessageTypeEnum.request);
	}
	
	protected void setForwardedBy(ApplicationInfo forwardedBy) {
		this.forwardedBy = forwardedBy;
	}
	
	public ApplicationInfo getForwardedBy() {
		return forwardedBy;
	}
	
	@Override
	public void setConversation(String conversationId) {
		super.setConversation(conversationId);
	}
	
	protected void setActionHandler(RequestActionInterface handler) {
		this.actionHandler = handler;
	}
	
	public void reply(Object replyData) throws PoloMessagingException {
		this.actionHandler.reply(this, replyData);
	}

	public void replyError(Object errorData) throws PoloMessagingException {
		this.actionHandler.replyError(this, errorData);
	}
	
	public void forward(String destination) throws PoloMessagingException {
		this.actionHandler.forward(this, destination);
	}

	public void done() throws PoloMessagingException {
		this.actionHandler.done(this);
	}

	public void dismiss() {
		this.actionHandler.dismiss(this);
	}
	
	@Override
	public void setPayload(Object payload) {
		super.setPayload(payload);
	}
}
