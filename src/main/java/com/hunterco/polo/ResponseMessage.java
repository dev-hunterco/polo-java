package com.hunterco.polo;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ResponseMessage extends PoloMessage {
	private RequestMessage originalMessage;
	private boolean success;
	
	@JsonIgnore
	private transient ResponseActionInterface actionHandler;
	
	protected ResponseMessage() {
	}
	
	public ResponseMessage(RequestMessage originalMessage) {
		super();
		this.originalMessage = originalMessage;
		this.setConversation(originalMessage.getConversation());
		this.setPayload(originalMessage.getPayload());
		this.setService(originalMessage.getService());
		this.setType(MessageTypeEnum.response);
	}

	public boolean isSuccess() {
		return success;
	}
	
	public void setSuccess(boolean success) {
		this.success = success;
	}
	
	public RequestMessage getOriginalMessage() {
		return this.originalMessage;
	}

	
	protected void setActionHandler(ResponseActionInterface actionHandler) {
		this.actionHandler = actionHandler;
	}
	
	public void done() throws PoloMessagingException {
		this.actionHandler.done(this);
	}
	
	public void dismiss() throws PoloMessagingException {
		this.actionHandler.dismiss(this);
	}
}
