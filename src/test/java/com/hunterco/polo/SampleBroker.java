package com.hunterco.polo;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SampleBroker extends SampleApp {
	private static Logger loger = LoggerFactory.getLogger(SampleBroker.class);
	public SampleBroker(String name, Map<String, Object> config) {
        super(name, config);
    }

    public void initializeQueue() throws PoloMessagingException {
        this.messagingAPI = new PoloMessaging(this.config);
        this.messagingAPI.initializeSQS();
	    this.messagingAPI.onRequest ("greetings", new RequestHandlerInterface() {
			@Override
			public void processRequest(RequestMessage message) throws PoloMessagingException {
				SampleBroker.this.onRequestArrived(message);
			}
	    });
	    this.messagingAPI.onResponse("greetings", new ResponseHandlerInterface() {
			@Override
			public void processResponse(ResponseMessage message) throws PoloMessagingException {
				SampleBroker.this.onResponseArrived(message);
			}
		});
    }

    public void onRequestArrived(RequestMessage message) throws PoloMessagingException {
        this.getRequestsReceived().add(message);
        // Always forward to app2
        message.forward("App2");
    }

    public void onResponseArrived(ResponseMessage message) {
        throw new Error("Broker should not receive an answer!");
    }

}
