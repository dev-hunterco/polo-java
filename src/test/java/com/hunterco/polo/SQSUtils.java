package com.hunterco.polo;

import java.util.List;
import java.util.concurrent.ExecutionException;

import software.amazon.awssdk.services.sqs.model.ListQueuesRequest;
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest;

public class SQSUtils {
	public static void purgeQueues(PoloMessaging api) {
		System.out.println("*** Purging queues...");
		try {
			api.getSQSClient()
				.listQueues(ListQueuesRequest.builder().build())
				.get()
				.queueUrls()
				.stream()
				.map(url -> PurgeQueueRequest.builder().queueUrl(url).build())
//				.peek(req -> System.out.println("Purging " + req.queueUrl()))
				.forEach(api.getSQSClient()::purgeQueue);
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}
}
