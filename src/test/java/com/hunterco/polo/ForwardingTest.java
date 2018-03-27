package com.hunterco.polo;

import java.io.File;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ForwardingTest {
	private static Logger logger = LoggerFactory.getLogger(ForwardingTest.class);
	private static final File DEFAULT_CONFIG = new File("./src/test/resources/sample_conf.json");
	private SampleApp app1;
	private SampleApp app2;
	private SampleBroker broker;
	
	public ForwardingTest() throws Exception {
		this.app1 = new SampleApp("App1", ConfigurationUtils.loadJsonFile(DEFAULT_CONFIG));
		this.app2 = new SampleApp("App2", ConfigurationUtils.loadJsonFile(DEFAULT_CONFIG));
        this.broker = new SampleBroker("Broker", ConfigurationUtils.loadJsonFile(DEFAULT_CONFIG));

		this.initQueues();
	}
	
	public void initQueues() throws PoloMessagingException {
		app1.initializeQueue();
		app2.initializeQueue();
		broker.initializeQueue();
	}
	
	@Before
	public void reset() {
		app1.reset();
		app2.reset();
		broker.reset();
		SQSUtils.purgeQueues(app1.messagingAPI);
	}
	
	
	@Test
	public void testSendToBrokerAndReceiveMessage() throws Exception {
        logger.info("_______________________ App1 to Broker ________________________");
        app1.sendGreetings("Broker");
        Assert.assertEquals(0, app1.getRequestsReceived().size());
        Assert.assertEquals(0, app1.getResponsesReceived().size());
        Assert.assertEquals(0, app2.getRequestsReceived().size());
        Assert.assertEquals(0, app2.getResponsesReceived().size());
        Assert.assertEquals(0, broker.getRequestsReceived().size());
        Assert.assertEquals(0, broker.getResponsesReceived().size());

        logger.info("_______________________ Broker receives and forwards _____________________");
        int numOfMessages = broker.receiveMessages();
        Assert.assertEquals(1, numOfMessages);
        Assert.assertEquals(0, app1.getRequestsReceived().size());
        Assert.assertEquals(0, app1.getResponsesReceived().size());
        Assert.assertEquals(0, app2.getRequestsReceived().size());
        Assert.assertEquals(0, app2.getResponsesReceived().size());
        Assert.assertEquals(1, broker.getRequestsReceived().size());
        
        logger.debug("_______________________ App2 receives and responds (to app1) _____________________");
        numOfMessages = app2.receiveMessages();
        Assert.assertEquals(1, numOfMessages);
        Assert.assertEquals(0, app1.getRequestsReceived().size());
        Assert.assertEquals(0, app1.getResponsesReceived().size());
        Assert.assertEquals(1, app2.getRequestsReceived().size());
        Assert.assertEquals(0, app2.getResponsesReceived().size());
        // It should only be "Hello, Broker" because sampleApp adds broker's name in the string
        Assert.assertEquals("Hello, Broker... I'm App1", app2.getRequestsReceived().get(0).getBody());
        Assert.assertEquals(1, broker.getRequestsReceived().size());
        
	    logger.debug("_______________________ App1 gets a response _____________________");
	    numOfMessages = app1.receiveMessages();
	    Assert.assertEquals(0, app1.getRequestsReceived().size());
	    Assert.assertEquals(1, app1.getResponsesReceived().size());
	    Assert.assertEquals(1, app2.getRequestsReceived().size());
	    Assert.assertEquals(0, app2.getResponsesReceived().size());

        Map<String, Object> responseData = (Map) app1.getResponsesReceived().get(0).getBody();
        Assert.assertEquals("Nice to meet you!", responseData.get("answer"));

	    Assert.assertEquals("Broker", app1.getResponsesReceived().get(0).getOriginalMessage().getForwardedBy().getApplication());
        Assert.assertEquals(1, numOfMessages);
    }

}
