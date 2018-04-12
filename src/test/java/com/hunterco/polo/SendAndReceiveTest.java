package com.hunterco.polo;

import java.io.File;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SendAndReceiveTest {
	private static Logger logger = LoggerFactory.getLogger(SendAndReceiveTest.class);
	private static final File DEFAULT_CONFIG = new File("./src/test/resources/sample_conf.json");
	private SampleApp app1;
	private SampleApp app2;
	
	public SendAndReceiveTest() throws Exception {
		this.app1 = new SampleApp("App1", ConfigurationUtils.loadJsonFile(DEFAULT_CONFIG));
		this.app2 = new SampleApp("App2", ConfigurationUtils.loadJsonFile(DEFAULT_CONFIG));
		
		this.initQueues();
	}
	
	public void initQueues() throws PoloMessagingException {
		app1.initializeQueue();
		app2.initializeQueue();
	}
	
	@Before
	public void reset() {
		app1.reset();
		app2.reset();
		SQSUtils.purgeQueues(app1.messagingAPI);
	}
	
	@Test
	public void testSendAndReceive() throws PoloMessagingException {
        logger.info("_______________________ App1 to App2 ________________________");
        String msgId = app1.sendGreetings("App2");
        Assert.assertNotNull(msgId);
        Assert.assertEquals(0, app1.getRequestsReceived().size());
        Assert.assertEquals(0, app1.getResponsesReceived().size());
        Assert.assertEquals(0, app2.getRequestsReceived().size());
        Assert.assertEquals(0, app2.getResponsesReceived().size());
        
        logger.info("_______________________ App2 receives and responds _____________________");
        int numOfMessages = app2.receiveMessages();
        Assert.assertEquals(1, numOfMessages);
        Assert.assertEquals(0, app1.getRequestsReceived().size());
        Assert.assertEquals(0, app1.getResponsesReceived().size());
        Assert.assertEquals(1, app2.getRequestsReceived().size());
        Assert.assertEquals(0, app2.getResponsesReceived().size());
        Assert.assertEquals("Hello, App2... I'm App1", app2.getRequestsReceived().get(0).getBody());
        
        logger.info("_______________________ App1 gets a response _____________________");
        numOfMessages = app1.receiveMessages();
        Assert.assertEquals(1, numOfMessages);
        Assert.assertEquals(0, app1.getRequestsReceived().size());
        Assert.assertEquals(1, app1.getResponsesReceived().size());
        Assert.assertEquals(1, app2.getRequestsReceived().size());
        Assert.assertEquals(0, app2.getResponsesReceived().size());
        Map<String, Object> responseData = (Map) app1.getResponsesReceived().get(0).getBody();
        Assert.assertEquals("Nice to meet you!", responseData.get("answer"));
	}
	
	@Test
	public void testSendAndDismiss() throws PoloMessagingException {
	    logger.info("_______________________ App1 to App2 ________________________");
	    app1.sendGreetings("App2");
	    Assert.assertEquals(0, app1.getRequestsReceived().size());
	    Assert.assertEquals(0, app1.getResponsesReceived().size());
	    Assert.assertEquals(0, app2.getRequestsReceived().size());
	    Assert.assertEquals(0, app2.getResponsesReceived().size());
	    
        logger.info("_______________________ App2 receives but won't answer _____________________");
        app2.setReplyEnabled(false);
        app2.receiveMessages();
        int numOfMessages = app1.receiveMessages();
        
        // since app2 didn't answered, app1 won't receive any message
        Assert.assertEquals(0, numOfMessages);
    }
	
	@Test
	public void testSendToInexistent() throws PoloMessagingException {
        logger.info("_______________________ App1 to BLARGH ________________________");
        try {
        		app1.sendGreetings("BLARGH");
        		Assert.fail("Should never get so far...");
        }
        catch(PoloMessagingException e) {
        		// Ok, I expect an exception.. :)
        		Assert.assertEquals("Error loading queue for app: BLARGH", e.getMessage());
        }
    }
	
	@Test
	public void testRequestInvalidService_NobodyKnows() throws PoloMessagingException {
        logger.info("_______________________ App1 to App2 ________________________");
        try {
        		app1.sendWrong("App2", null);
        		Assert.fail("Should never get here since app1 doesn't have a response handler configured for wrong_greetings");
        }
        catch(PoloMessagingException e) {
        		Assert.assertEquals("Can't send message to service wrong_greetings without a response handler registered.", e.getMessage());
        }
    }
	
	@Test
	public void testRequestInvalidService_App1Knows() throws PoloMessagingException {
        // primeiro faz o registro de wrong_greetings pra poder lançar
        app1.registerWrongHandler();

        logger.info("_______________________ App1 to App2 ________________________");
        app1.sendWrong("App2", null);
	    Assert.assertEquals(0, app1.getRequestsReceived().size());
	    Assert.assertEquals(0, app1.getResponsesReceived().size());
	    Assert.assertEquals(0, app2.getRequestsReceived().size());
	    Assert.assertEquals(0, app2.getResponsesReceived().size());
                
        app2.receiveMessages();
        int numOfMessages = app1.receiveMessages();
        
        Assert.assertEquals(1, numOfMessages);
        Assert.assertEquals(0, app1.getResponsesReceived().size());
        Assert.assertEquals(1, app1.getWrongResponsesReceived().size());

        Assert.assertEquals(false, app1.getWrongResponsesReceived().get(0).isSuccess());
        Assert.assertEquals("{error=Service 'wrong_greetings' not supported.}", app1.getWrongResponsesReceived().get(0).getBody().toString());
    }
	
	@Test
	public void testAsyncResponse() throws PoloMessagingException {
        logger.info("_______________________ App1 to App2 ________________________");
//        app1.sendAsyncGreetings("App2")
//            .then(reciboEnvio => {
//                app1.getRequestsReceived().length.should.be.eql(0);
//                app1.getResponsesReceived().length.should.be.eql(0);
//                app2.getRequestsReceived().length.should.be.eql(0);
//                app2.getResponsesReceived().length.should.be.eql(0);
//                return reciboEnvio;
//            })
//            .then(reciboEvento => {
//                logger.debug("_______________________ App2 receives and responds _____________________")
//                return app2.receiveMessages()
//            })
//            .then(numOfMessages => {
//                numOfMessages.should.be.eql(1);
//                app1.getRequestsReceived().length.should.be.eql(0);
//                app1.getResponsesReceived().length.should.be.eql(0);
//                app2.getRequestsReceived().length.should.be.eql(1);
//                app2.getResponsesReceived().length.should.be.eql(0);
//                app2.getRequestsReceived()[0].body.should.be.eql("Hello, App2... See you later...");
//                app2.getPendingResponses().length.should.be.eql(1);
//                return numOfMessages;
//            })
//            .then(_ => {
//                logger.debug("_______________________ App1 * WON'T *gets a response _____________________")
//                return app1.receiveMessages();
//            })
//            .then(numOfMessages => {
//                app1.getRequestsReceived().length.should.be.eql(0);
//                app1.getResponsesReceived().length.should.be.eql(0);
//                app2.getRequestsReceived().length.should.be.eql(1);
//                app2.getResponsesReceived().length.should.be.eql(0);
//                return numOfMessages;
//            })
//            .then(_ => {
//                logger.debug("_______________________ Now app2 sends an async answer _____________________")
//                var msg = app2.getPendingResponses()[0];
//                app2.sendAsyncResponse(msg);
//                return app1.receiveMessages();
//            })
//            .then(numOfMessages => {
//                app1.getRequestsReceived().length.should.be.eql(0);
//                app1.getResponsesReceived().length.should.be.eql(1);
//                app2.getRequestsReceived().length.should.be.eql(1);
//                app2.getResponsesReceived().length.should.be.eql(0);
//                return numOfMessages;
//            })
//            .then(numOfMessages => {
//                numOfMessages.should.be.eql(1);
//                done()
//            })
//            .catch(error => {
//                done(error);
//            });
    }

	
}
