package com.hunterco.polo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.RuntimeErrorException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SampleApp {
	private static Logger logger = LoggerFactory.getLogger(SampleApp.class);
	
	protected String name;
	protected Map<String, Object> config;
	protected PoloMessaging messagingAPI;
	
	private ArrayList<RequestMessage> requests = new ArrayList<>();
	private ArrayList<RequestMessage> pendingResponses = new ArrayList<>();
	private ArrayList<ResponseMessage> responses = new ArrayList<>();
	private ArrayList<ResponseMessage> wrongResponses = new ArrayList<>();

	private boolean replyEnabled = false;
		
	public SampleApp(String name, Map<String, Object> config) {
	    // Defaults
	    this.name = name;
	    ConfigurationUtils.set(config, Arrays.asList("app"), name);
	    ConfigurationUtils.set(config, Arrays.asList("aws", "sqs", "create"), true);
	    this.config = config;
	
	    this.reset();
	}
	
	public String getName() {
		return name;
	}
	
	public void initializeQueue() throws PoloMessagingException {
	    this.messagingAPI = new PoloMessaging(this.config);
	    this.messagingAPI.initializeSQS();
	    
        logger.info("Queue initialized for " + this.getName());
        this.messagingAPI.onRequest ("greetings", new RequestHandlerInterface() {
			public void processRequest(RequestMessage message) throws PoloMessagingException {
				SampleApp.this.onRequestArrived(message);
			}
		});
        
        this.messagingAPI.onResponse("greetings", new ResponseHandlerInterface() {
	        	public void processResponse(ResponseMessage message) throws PoloMessagingException {
        			SampleApp.this.onResponseArrived(message);
	        	}        	
        });

        this.messagingAPI.onRequest ("asyncGreetings", new RequestHandlerInterface() {
			public void processRequest(RequestMessage message) throws PoloMessagingException {
				SampleApp.this.onAsyncRequestArrived(message);
			}
		});
        
        this.messagingAPI.onResponse("asyncGreetings", new ResponseHandlerInterface() {
	        	public void processResponse(ResponseMessage message) throws PoloMessagingException {
        			SampleApp.this.onResponseArrived(message);
	        	}        	
        });
	}
	
	public void reset() {
	    this.requests.clear();
	    this.responses.clear();
	    this.wrongResponses.clear();
	    this.replyEnabled = true;
	}
	
	public void setReplyEnabled(boolean r) {
	    this.replyEnabled  = r;
	}
	
	public void onRequestArrived(RequestMessage message) throws PoloMessagingException {
	    logger.info(this.name + " - Request received by " + this.getName() + " from " + message.getSentBy().getApplication());
	
	    // Sempre registra as mensagens recebidas, mesmo quando não processa.
	    this.requests.add(message);
	    
	    Map<String, Object> response = new HashMap<>();
	    response.put("answer", "Nice to meet you!");
	    
	    if(this.replyEnabled)
	        message.reply(response);
	    else
	        message.dismiss();
	}
	
	public void onAsyncRequestArrived(RequestMessage message) throws PoloMessagingException {
        logger.info(this.name + " - Message received by", this.name, "from", message.getSentBy().getApplication(), "- Async Response");

        this.requests.add(message);
        this.pendingResponses.add(message);

        // Won't send an answer but will remove the message from queue
        // Notice that the app should send an async response sometime later.
        message.done();
    }

	public String sendAsyncResponse(RequestMessage message) throws PoloMessagingException {
        Map<String, Object> data = new HashMap<>();
        data.put("message", "It took some time, but here it is...");
        return this.messagingAPI.sendAsyncResponse(message, data);
    }

	public String sendAsyncGreetings(String destination, Object payload) throws PoloMessagingException {
        String message = "Hello, " + destination + "... See you later...";
        return this.messagingAPI.sendRequest(destination, "asyncGreetings", message);
    }
	
	public void onResponseArrived(ResponseMessage message) throws PoloMessagingException {
	    logger.info(this.name + " - Response received by " + this.getName() + " from " + message.getSentBy().getApplication());
	
	    this.responses.add(message);
	    message.done();
	}

	public String sendGreetings(String destination) throws PoloMessagingException {
		return this.sendGreetings(destination, null);
	}
	public String sendGreetings(String destination, Object payload) throws PoloMessagingException {
	    String message = "Hello, " + destination + "... I'm " + this.getName();
	    return this.messagingAPI.sendRequest(destination, "greetings", message);
	}
	
	public int receiveMessages() throws PoloMessagingException {
	    return this.messagingAPI.readMessages();
	}
	
	public List<RequestMessage> getRequestsReceived() {
	    return this.requests;
	}
	
	public List<ResponseMessage> getResponsesReceived() {
	    return this.responses;
	}
	
	public List<ResponseMessage> getWrongResponsesReceived() {
	    return this.wrongResponses;
	}
	
	public void sendWrong(String destination, Object payload) throws PoloMessagingException {
	    String message = "Hello, " + destination + "... I'm " + this.name;
	    this.messagingAPI.sendRequest(destination, "wrong_greetings", message);;
	}
	
	public void registerWrongHandler() {
	    this.messagingAPI.onResponse("wrong_greetings", this::onWrongResponseArrived);
	}
	
	public void onWrongResponseArrived(ResponseMessage message) throws PoloMessagingException {
	    logger.debug(this.name + " - WRONG Response received by", this.getName(), "from", message.getSentBy().getApplication());
	
	    this.wrongResponses.add(message);
	    message.done();
	}
}
