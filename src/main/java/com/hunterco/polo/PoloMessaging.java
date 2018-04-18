package com.hunterco.polo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.core.auth.AwsCredentials;
import software.amazon.awssdk.core.auth.AwsCredentialsProvider;
import software.amazon.awssdk.core.auth.AwsCredentialsProviderChain;
import software.amazon.awssdk.core.auth.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.auth.StaticCredentialsProvider;
import software.amazon.awssdk.core.auth.SystemPropertyCredentialsProvider;
import software.amazon.awssdk.core.regions.Region;
import software.amazon.awssdk.services.sqs.SQSAsyncClient;
import software.amazon.awssdk.services.sqs.SQSAsyncClientBuilder;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.ListQueuesRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest.Builder;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

public class PoloMessaging {
	private static final Logger logger = LoggerFactory.getLogger(PoloMessaging.class);
	private static final ObjectMapper mapper = new ObjectMapper();

	private Map<String, Object> config;
	private ApplicationInfo appInfo;
	private String queueName = null;
	private SQSAsyncClient sqsClient;
	private Map<String, RequestHandlerInterface> requestHandlers = new HashMap<>();
	private Map<String, ResponseHandlerInterface> responseHandlers = new HashMap<>();
	private Map<String, String> urlCache = new HashMap<>();

	public PoloMessaging(Map<String, Object> conf) throws PoloMessagingException {
		this.checkConfiguration(conf);
        this.config = conf;
        this.appInfo = new ApplicationInfo();
        appInfo.setApplication((String) ConfigurationUtils.get(this.config, "app"));
        appInfo.setInstance((String) conf.get("worker"));
	}

	public PoloMessaging(File configFile) throws PoloMessagingException, JsonParseException, JsonMappingException, FileNotFoundException, IOException {
		this(ConfigurationUtils.loadJsonFile(configFile));
	}

	public PoloMessaging(String configFile) throws PoloMessagingException, JsonParseException, JsonMappingException, FileNotFoundException, IOException {
		this(ConfigurationUtils.loadJsonFile(new File(configFile)));
	}
	
	public String getQueueUrl() {
		return this.appInfo.getCallback();
	}
	
	public String getQueueName() {
		return this.queueName;
	}
	
	private void checkConfiguration(Map<String, Object> conf) throws PoloMessagingException {
		if(conf == null || conf.isEmpty())
			throw new PoloMessagingException("Invalid Configuration");
		if(conf.get("app") == null || conf.get("app").toString().isEmpty())
			throw new PoloMessagingException("No App Identifier was set.");
		
		if(conf.get("stage") == null || conf.get("stage").toString().isEmpty()) {
			if(System.getenv("current_stage") == null)
				throw new PoloMessagingException("Application Stage not set.");
			else {
                conf.put("stage", System.getenv("current_stage"));
                logger.info("Stage set to " + conf.get("stage"));
			}
		}
		
		if(conf.get("worker") == null || conf.get("worker").toString().isEmpty()) {
			String processName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
            logger.warn("Worker Id not set. Assuming " + processName);
            conf.put("worker", processName);
        }

        if(conf.get("aws") == null) {
            logger.warn("AWS Credentials not set. Environment set?");
        } else {
        		Map<String, Object> awsConf = (Map) conf.get("aws");
            if(awsConf.get("sqs") == null) {
                logger.warn("SQS configuration not set.");
            }
            if(awsConf.get("sns") == null) {
                logger.warn("SNS configuration not set.");
            }
        }
	}

	private AwsCredentials getCredentials() {
		ArrayList<AwsCredentialsProvider> providers = new ArrayList<>();;
		
		// Configuration received
		Map<String, Object> map = (Map) ConfigurationUtils.get(this.config, "aws", "api");
		
		if(map != null && 
		   map.get("accessKeyId") != null && 
		   map.get("secretAccessKey") != null) {
			providers.add(StaticCredentialsProvider.create(AwsCredentials.create((String) map.get("accessKeyId"), 
																				(String) map.get("secretAccessKey"))));
		}
		
		// System Properties
		providers.add(SystemPropertyCredentialsProvider.create());
		
		// Environment
		providers.add(EnvironmentVariableCredentialsProvider.create());
		
		// ECS containers have an environment variable pointing to a json - check it later
		AwsCredentialsProviderChain credentialProviderChain = AwsCredentialsProviderChain.builder().credentialsProviders(providers).build();
		return credentialProviderChain.getCredentials();
	}
	
	public Map<String, Object> getConfig() {
		return this.config;
	}
	
	private String findQueue(String queueName, boolean autocreate) throws PoloMessagingException {
        ListQueuesRequest lqr = ListQueuesRequest.builder()
				.queueNamePrefix(queueName)
				.build();
        List<String> queueUrls;
        try {
        		queueUrls = this.sqsClient.listQueues(lqr).get().queueUrls();
        		if(queueUrls == null)
        			queueUrls = new ArrayList<>();
        } catch (InterruptedException | ExecutionException e) {
        		logger.warn("Error loading queues: " + e.getMessage());
        		queueUrls = new ArrayList<>();
        }

		if(queueUrls.size() > 0) {
			return queueUrls.get(0);
		}
		else if(autocreate) {
			logger.info("Creating queue...");
			try {
				String queueUrl = this.sqsClient.createQueue(CreateQueueRequest.builder().queueName(queueName)
							  	.build())
								.get().queueUrl();
				logger.info("Queue created: " + queueUrl);
				return queueUrl;
			} catch (InterruptedException | ExecutionException e) {
				throw new PoloMessagingException("Error creating queue", e);
			}
		}
		else {
			logger.error("Queue not found...");
			throw new PoloMessagingException("Queue " + queueName + " not found.");
		}
	}
	
	public void initializeSQS() throws PoloMessagingException {
		Region region = null;
		if(ConfigurationUtils.get(this.config, "aws", "api", "region") != null)
			region = Region.of((String) ConfigurationUtils.get(this.config, "aws", "api", "region"));
		else if(System.getenv("AWS_REGION") != null)
			region = Region.of(System.getenv("AWS_REGION"));
		
        SQSAsyncClientBuilder builder = SQSAsyncClient.builder()
        									.credentialsProvider(StaticCredentialsProvider.create(this.getCredentials()))
        									.region(region);
        
        String endpoint = (String) ConfigurationUtils.get(this.config, "aws", "sqs", "endpoint");
        if(endpoint != null) {
        		builder = builder.endpointOverride(URI.create(endpoint));
        }
        
        this.sqsClient = builder.build();

        // Define queue name
        this.queueName = ConfigurationUtils.get(this.config, "app") + "_" + ConfigurationUtils.get(this.config, "stage");

        String queueUrl = this.findQueue(this.queueName, (boolean) ConfigurationUtils.get(this.config, "aws", "sqs", "create"));
        if(queueUrl == null) {
			throw new PoloMessagingException("Error creating queue.");
        }
		this.appInfo.setCallback(queueUrl);
		logger.info("Found queue for " + ConfigurationUtils.get(this.config, "app") + ": " + this.appInfo.getCallback());
	}

	public void onRequest(String service, RequestHandlerInterface requestConsumer) {
		this.requestHandlers.put(service, requestConsumer);
	}

	public void onResponse(String service, ResponseHandlerInterface responseConsumer) {
		this.responseHandlers.put(service, responseConsumer);
	}

	public String sendRequest(String destination, String service, Object message) throws PoloMessagingException {
		return this.sendRequest(destination, service, message, null);
	}
	public String sendRequest(String destination, String service, Object message, Object payload) throws PoloMessagingException {
		return this.sendRequest(destination, service, message, null, null);
	}
	public String sendRequest(String destination, String service, Object message, Object payload, String conversationId) throws PoloMessagingException {
        // Verifica se o aplicativo é capaz de receber respostas do serviço.
        if(this.responseHandlers.get(service) == null) {
            throw new PoloMessagingException("Can't send message to service " + service + " without a response handler registered.");
        }

        if(conversationId == null)
            conversationId = UUID.randomUUID().toString();

        // Get the queue URL
        String destQueue = this.getQueueUrl(destination);
        RequestMessage request = new RequestMessage();
        request.setSentBy(this.appInfo);
        request.setConversation(conversationId);
        request.setService(service);
        request.setType(MessageTypeEnum.request);
        request.setBody(message);
        request.setPayload(payload);
        
        return this.sendToQueue(destQueue, request);
	}
		
    public int readMessages() throws PoloMessagingException {
    		return this.readMessages(null);
    }
    public int readMessages(Map<String, Object> params) throws PoloMessagingException {
    		Map<String, Object> finalParams = null;
    		Map<String, Object> defaultParams = (Map) ConfigurationUtils.get(this.config, "aws", "sqs", "consume");
    		
    		if(params == null && defaultParams != null) // assume default
    			finalParams = defaultParams;
    		else if(params != null && defaultParams == null) // assume argument
    			finalParams = params;
    		else if(params != null && defaultParams != null) { // override defauls
    			final Map<String, Object> fp = new HashMap<>(defaultParams);
    			params.entrySet().forEach(entry -> fp.put(entry.getKey(), entry.getValue()));
    			finalParams = fp;
    		}
    		
        int numOfMessages = 0;
        int messagesRead = 0;
        
        do {
        		List<Message> messages = this.getMessages(this.appInfo.getCallback(), finalParams);
        		if(messages != null) {
	        		messagesRead = messages.size();
	        		
	        		messages.forEach(this::processMessage);
	        		numOfMessages += messagesRead;
        		}
        		else
        			messagesRead = 0;
        } while(messagesRead > 0);
        
        return numOfMessages;
    }
    
    protected List<Message> getMessages(String queue, Map<String, Object> params) throws PoloMessagingException {
	    	if(params == null)
	    		params = new HashMap<>();
	    	
    		try {
	    		Builder builder = ReceiveMessageRequest.builder().queueUrl(queue);
	    		
	    		// dynamically set extra parameters
	    		params.entrySet().forEach(entry -> {
	    			String methodName = "set" + entry.getKey();
					try {
						Method m = builder.getClass().getMethod(methodName, entry.getValue().getClass());
						m.setAccessible(true);
						m.invoke(builder, entry.getValue());
					} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						logger.warn("Error setting ReadMessage parameter " + entry + ": " + e.getMessage());
					}
	    		});
	    		
	    		ReceiveMessageRequest req = builder.build();
	    		return this.sqsClient.receiveMessage(req).get().messages();
    		}
    		catch(InterruptedException | ExecutionException e) {
    			throw new PoloMessagingException("Error sending message: " + e.getMessage(), e);
    		}
    }
    
	public String sendAsyncResponse(RequestMessage message, Object data) throws PoloMessagingException {
		ResponseMessage response = new ResponseMessage(message);
		response.setBody(data);
		response.setSuccess(true);
		response.setSentBy(PoloMessaging.this.appInfo);
		
        return PoloMessaging.this.sendToQueue(message.getSentBy().getCallback(), response);
	}

	public String sendAsyncForward(RequestMessage msg, String destination) throws PoloMessagingException {
        RequestMessage request = new RequestMessage();
        request.setSentBy(msg.getSentBy());
        request.setConversation(msg.getConversation());
        request.setService(msg.getService());
        request.setType(MessageTypeEnum.request);
        request.setBody(msg.getBody());
        request.setPayload(msg.getPayload());
        request.setForwardedBy(PoloMessaging.this.appInfo);
        
        String destQueue = PoloMessaging.this.getQueueUrl(destination);
        return PoloMessaging.this.sendToQueue(destQueue, request);
    }
	
	public String sendAsyncReplyError(RequestMessage msg, Object error) throws PoloMessagingException {
		Map<String, Object> errorInfo = new HashMap<>();
		errorInfo.put("error", error);
		ResponseMessage response = new ResponseMessage(msg);
		response.setBody(errorInfo);
		response.setSuccess(false);
		response.setSentBy(PoloMessaging.this.appInfo);
		
        return PoloMessaging.this.sendToQueue(msg.getSentBy().getCallback(), response);
    }
    
    protected void processMessage(Message sqsMessage) {
		try {
			PoloMessage poloMessage = mapper.readValue(sqsMessage.body(), PoloMessage.class);
			
	        if(poloMessage.getType().equals(MessageTypeEnum.request)) {
	        		RequestMessage requestMessage = mapper.readValue(sqsMessage.body(), RequestMessage.class);
	        		
	        		// Attach Action Handlers
				RequestActionInterface actions = new RequestActionHandler(sqsMessage.receiptHandle());
				requestMessage.setActionHandler(actions);
				
				// Gets Business Handler (consumer)
				RequestHandlerInterface handler = this.requestHandlers.get(requestMessage.getService());
				if(handler == null) {
	                actions.replyError(requestMessage, "Service '" + requestMessage.getService() + "' not supported.");
				}
				else {
					handler.processRequest(requestMessage);
				}
	        }
	        else if(poloMessage.getType().equals(MessageTypeEnum.response)) {
	        		ResponseMessage responseMessage = mapper.readValue(sqsMessage.body(), ResponseMessage.class);
	        		
	        		// Attach Action Handlers
				ResponseActionInterface actions = new ResponseActionHandler(sqsMessage.receiptHandle());
				responseMessage.setActionHandler(actions);
				
				// Gets Business Handler (consumer)
				ResponseHandlerInterface handler = this.responseHandlers.get(responseMessage.getService());
				if(handler == null) {
	                // The service, originally, was able to send such a message but can't recognize the response
					// Just log the error and remove it from the queue
	                logger.error("An invalid request was sent to " + responseMessage.getSentBy().getApplication() + " by this application (" + this.appInfo.getApplication() + ")");
	                logger.error("or there's no response handler for service " + responseMessage.getService() + " (although it was able to send this message at some time).");
	                actions.done(responseMessage);
				}
				else {
					handler.processResponse(responseMessage);
				}
	        }
	        else {
	            // Mensagem recebida tem um tipo incompatível.
	            logger.error("Invalid message received. Type should be request|response");
	            logger.info("Message will be removed.");
	            this.removeFromQueue(this.appInfo.getCallback(), sqsMessage.receiptHandle());
	        }
		} catch (IOException | PoloMessagingException e) {
			e.printStackTrace();
			//throw new PoloMessagingException("Error processing message: " + e.getMessage(), e);
		}
    }
	
	private String getQueueUrl(String destination) throws PoloMessagingException {
		boolean autoCreate = (boolean) ConfigurationUtils.get(this.config, "aws", "sqs", "create");
        String destQueue = this.urlCache.get(destination);
        if(destQueue == null) {
        		String targetQueue = destination + "_" + this.config.get("stage");
        		GetQueueUrlRequest req = GetQueueUrlRequest.builder().queueName(targetQueue).build();
        		
            try {
				destQueue = this.sqsClient.getQueueUrl(req).get().queueUrl();
			} catch (InterruptedException | ExecutionException e) {
				if(autoCreate) {
					logger.info("Creating queue...");
					try {
						String queueUrl = this.sqsClient.createQueue(CreateQueueRequest.builder().queueName(targetQueue)
									  	.build())
										.get().queueUrl();
						logger.info("Queue created: " + targetQueue);
						return queueUrl;
					} catch (InterruptedException | ExecutionException e2) {
						throw new PoloMessagingException("Error creating queue", e2);
					}
				}
				else
					throw new PoloMessagingException("Error loading queue for app: " + destination, e);
			}
            this.urlCache.put(destination, destQueue);
        }
        return destQueue;
	}

	protected String sendToQueue(String queueUrl, Object data) throws PoloMessagingException {
		try {
			String messageJson = mapper.writeValueAsString(data);
			SendMessageRequest req = SendMessageRequest.builder()
				.queueUrl(queueUrl)
				.messageBody(messageJson)
				.build();
			
			SendMessageResponse response = this.sqsClient.sendMessage(req).get();
			return response.messageId();
		} catch (JsonProcessingException | InterruptedException | ExecutionException e) {
			throw new PoloMessagingException("Error sending message to queue: " + e.getMessage(), e);
		}
	}
		
	class RequestActionHandler implements RequestActionInterface {
		private String messageReceipt;
		public RequestActionHandler(String receipt) {
			this.messageReceipt = receipt;
		}
		
		@Override
		public String reply(RequestMessage msg, Object data) throws PoloMessagingException {
			String messageReceipt = PoloMessaging.this.sendAsyncResponse(msg, data);
	        PoloMessaging.this.removeFromQueue(PoloMessaging.this.appInfo.getCallback(), this.messageReceipt);
	        return messageReceipt;
		}

		@Override
		public String replyError(RequestMessage msg, Object error) throws PoloMessagingException {
			String messageReceipt = PoloMessaging.this.sendAsyncReplyError(msg, error);
	        PoloMessaging.this.removeFromQueue(PoloMessaging.this.appInfo.getCallback(), this.messageReceipt);
	        return messageReceipt;
		}

		@Override
		public void dismiss(RequestMessage msg) {
		}

		@Override
		public String forward(RequestMessage msg, String destination) throws PoloMessagingException {
	        String newMessageReceipt = PoloMessaging.this.sendAsyncForward(msg, destination); 
	        PoloMessaging.this.removeFromQueue(PoloMessaging.this.appInfo.getCallback(), this.messageReceipt);
	        return newMessageReceipt; 
		}
		
		@Override
		public void done(RequestMessage msg) throws PoloMessagingException {
	        PoloMessaging.this.removeFromQueue(PoloMessaging.this.appInfo.getCallback(), this.messageReceipt);
		}
	}

	class ResponseActionHandler implements ResponseActionInterface {
		private String messageReceipt;
		public ResponseActionHandler(String receipt) {
			this.messageReceipt = receipt;
		}
		
		@Override
		public void done(ResponseMessage msg) throws PoloMessagingException {
	        PoloMessaging.this.removeFromQueue(PoloMessaging.this.appInfo.getCallback(), this.messageReceipt);
		}

		@Override
		public void dismiss(ResponseMessage msg) {
		}
	}

	public void removeFromQueue(String callback, String messageReceipt) throws PoloMessagingException {
		DeleteMessageRequest req = DeleteMessageRequest.builder().queueUrl(callback).receiptHandle(messageReceipt).build();
		try {
			this.sqsClient.deleteMessage(req).get();
		} catch (InterruptedException | ExecutionException e) {
			throw new PoloMessagingException("Error removing message from queue: " + callback, e);
		}
	}
	
	SQSAsyncClient getSQSClient() {
		return this.sqsClient;
	}
}
