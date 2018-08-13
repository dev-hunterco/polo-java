package com.hunterco.polo;

import java.util.Date;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PoloMessage {
	private String id;
	private String conversation;
	private MessageTypeEnum type;
	private ApplicationInfo sentBy;
	private String service;
	private Object body;
	private Object payload;
	private Date timestamp;
	private String sqsReceipt;
	
	public PoloMessage() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = new Date();
	}
	
	public String getId() {
		return id;
	}

	@JsonFormat(shape=JsonFormat.Shape.STRING)
	public Date getTimestamp() {
		return timestamp;
	}
	
	public String getConversation() {
		return conversation;
	}
	protected void setConversation(String conversationId) {
		this.conversation = conversationId;
	}
	
	public MessageTypeEnum getType() {
		return type;
	}
	protected void setType(MessageTypeEnum type) {
		this.type = type;
	}
	
	public ApplicationInfo getSentBy() {
		return sentBy;
	}
	public void setSentBy(ApplicationInfo sentBy) {
		this.sentBy = sentBy;
	}
	
	public String getService() {
		return service;
	}
	public void setService(String service) {
		this.service = service;
	}
	
	public Object getBody() {
		return body;
	}
	public void setBody(Object body) {
		this.body = body;
	}
	
	public Object getPayload() {
		return payload;
	}
	
	protected void setPayload(Object payload) {
		this.payload = payload;
	}
	
	protected String getSqsReceipt() {
		return sqsReceipt;
	}
	protected void setSqsReceipt(String sqsReceipt) {
		this.sqsReceipt = sqsReceipt;
	}
}
