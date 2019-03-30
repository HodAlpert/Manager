package com.manager.managers;
import java.util.*;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.*;

public class SQSManager extends BaseManager {
    AmazonSQS sqs = AmazonSQSClientBuilder.standard()
            .withRegion("us-east-2")
            .build();

    public String create_queue(String queue_name) {
        try {
            logger.fine("Creating a new SQS queue called " + queue_name);
            CreateQueueRequest createQueueRequest = new CreateQueueRequest(queue_name + UUID.randomUUID());
            String answer = sqs.createQueue(createQueueRequest).getQueueUrl();
            logger.fine("returned " + answer);
            return answer;
        } catch (Exception exc) {
            handle_exception(exc);
            return null;
        }
    }

    public List<String> list_queues() {
        try {
            List<String> answer = new ArrayList<String>(sqs.listQueues().getQueueUrls());
            logger.fine("returned " + answer);
            return answer;
        } catch (Exception exc) {
            handle_exception(exc);
            return null;
        }
    }

    public SendMessageResult send_message(String queue_url, String message, String request_id, String consumer) {
        logger.fine("entry");
        try {
            logger.fine(String.format("Sending a message: %s to queue: %s with request_id: %s and consumer: %s",
                    message, queue_url, request_id, consumer));
            final Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
            messageAttributes.put("request_id", new MessageAttributeValue()
                    .withDataType("String")
                    .withStringValue(request_id));
            messageAttributes.put("consumer", new MessageAttributeValue()
                    .withDataType("String")
                    .withStringValue(consumer));

            SendMessageRequest req = new SendMessageRequest(queue_url, message)
                    .withMessageAttributes(messageAttributes)
                    .withMessageGroupId(String.valueOf(UUID.randomUUID()));
            SendMessageResult answer = sqs.sendMessage(req);
            logger.fine("returned " + answer);
            return answer;
        } catch (Exception exc) {
            handle_exception(exc);
            return null;
        }

    }

    public Message recieve_message(String queue_url, String request_id, String consumer) {
        logger.fine("entry");
        Message answer = null;
        try {
            logger.fine(String.format("Receiving messages from bucket: %s with request_id: %s and consumer: %s"
                    , queue_url, request_id, consumer));
            List<String> attributes = new ArrayList<>();
            attributes.add(consumer);
            if (request_id != null)
                attributes.add(request_id);
            ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queue_url)
                    .withAttributeNames(attributes)
                    .withMaxNumberOfMessages(1)
                    .withWaitTimeSeconds(10);
            List<Message> messages = sqs.receiveMessage(receiveMessageRequest).getMessages();
            if (!messages.isEmpty())
                answer = messages.get(0);
            logger.fine("returned " + answer);
            return answer;
        } catch (Exception exc) {
            handle_exception(exc);
            return null;
        }
    }

    public DeleteMessageResult delete_message(String queue_url, Message message) {
        try {
            logger.fine("entry");
            logger.fine(String.format("Deleting message %s from queue %s",
                    message.getMessageId(), queue_url));
            final String messageReceiptHandle = message.getReceiptHandle();
            DeleteMessageResult answer = sqs.deleteMessage(new DeleteMessageRequest(queue_url, messageReceiptHandle));
            logger.fine("returned " + answer);
            return answer;
        } catch (Exception exc) {
            handle_exception(exc);
            return null;
        }
    }

    public DeleteQueueResult delete_queue(String queue_url) {
        try {
            logger.fine("entry");
            logger.fine(String.format("Deleting queue %s", queue_url));
            DeleteQueueResult answer = sqs.deleteQueue(new DeleteQueueRequest(queue_url));
            logger.fine("returned " + answer);
            return answer;

        } catch (Exception exc) {
            handle_exception(exc);
            return null;
        }
    }
}
