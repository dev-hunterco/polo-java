package com.hunterco.polo;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

public class InitializationTest {
	private static final File DEFAULT_CONFIG = new File("./src/test/resources/sample_conf.json");

	@Test
    public void testInitializeQueue() throws Exception {
        PoloMessaging messagingAPI = new PoloMessaging(DEFAULT_CONFIG);
        messagingAPI.initializeSQS();
        
        Assert.assertNotNull(messagingAPI.getQueueName());
        Assert.assertNotNull(messagingAPI.getQueueUrl());
    }
    
    
}
