package com.hunterco.polo;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * Unit test for simple App.
 */
public class ConfigurationsTest {
	private static final ObjectMapper objectMapper = new ObjectMapper();
	private static final File DEFAULT_CONFIG = new File("./src/test/resources/sample_conf.json");

	@Test
	public void testNoConf() {
        try {
            PoloMessaging messagingAPI = new PoloMessaging((Map) null);
            Assert.fail("Should not have initialized without a configuration");
         } catch (Exception e) {
             Assert.assertEquals(PoloMessagingException.class, e.getClass());
         }
	}
	
	@Test
	public void testNoApp() {
        try {
            PoloMessaging messagingAPI = new PoloMessaging(new HashMap<String, Object>());
            Assert.fail("Should not have initialized without a configuration");
         } catch (Exception e) {
             Assert.assertEquals(PoloMessagingException.class, e.getClass());
         }
	}
	
	@Test
	public void testNoStage() {
        try {
        		PoloMessaging messagingAPI = new PoloMessaging(objectMapper.readValue("{\"app\":\"testApp\"}", Map.class));
        		Assert.fail("Should not have initialized without config.stage");
        } catch (Exception e) {
            Assert.assertEquals(PoloMessagingException.class, e.getClass());
        }
    }

	@Test
    public void testAutosetWorkerId() throws Exception {
		PoloMessaging messagingAPI = new PoloMessaging(objectMapper.readValue("{\"app\":\"testApp\", \"stage\":\"test\"}", Map.class));
		Assert.assertNotNull(messagingAPI.getConfig().get("worker"));
    }

	@Test
    public void testNoAWSConfiguration() throws Exception {
		PoloMessaging messagingAPI = new PoloMessaging(objectMapper.readValue("{\"app\":\"testApp\", \"stage\":\"test\", \"worker\":\"me\"}", Map.class));
		Assert.assertEquals("me", messagingAPI.getConfig().get("worker"));
    }

	@Test
    public void testConfWithWarnings() throws Exception {
    		PoloMessaging messagingAPI = new PoloMessaging(objectMapper.readValue("{\"app\":\"testApp\", \"stage\":\"test\", \"worker\":\"me\", \"aws\":{}}", Map.class));
    }

	@Test
    public void testConfWithoutWarnings() throws Exception {
         // Não testa efetivamente os warnings (só visualmente) mas poderia fazer
         // se criar um appender pro logger
    		PoloMessaging messagingAPI = new PoloMessaging(objectMapper.readValue("{\"app\":\"testApp\", \"stage\":\"test\", \"worker\":\"me\", \"aws\":{\"sqs\":{}, \"sns\":{}}}", Map.class)) ;
    }

	@Test
    public void testConfiguringUsingFile() throws Exception {
    		PoloMessaging messagingAPI = new PoloMessaging(DEFAULT_CONFIG);
    }
}
