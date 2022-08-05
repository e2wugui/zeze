package Zeze.Services.RocketMQ;

import javax.jms.CompletionListener;
import javax.jms.Destination;
import javax.jms.IllegalStateException;
import javax.jms.JMSException;
import javax.jms.Message;

public class MessageProducer implements javax.jms.MessageProducer {

	org.apache.rocketmq.client.producer.DefaultMQProducer producer;
	protected Session session;
	protected Destination destination; // default destination
	protected int deliveryMode;
	protected boolean disableMessageID;
	protected boolean disableMessageTimestamp;
	protected int priority;
	protected long timeToLive;
	protected long deliveryDelay;
	protected boolean closed;
	protected int producerID;

	public MessageProducer() {
	}

	public MessageProducer(Session session, int producerId, Destination destination, int sendTimeout) {
		this.producer = new org.apache.rocketmq.client.producer.DefaultMQProducer("producer" + producerId);
		this.producerID = producerId;
		this.session = session;
		this.destination = destination;

//		this.producer.setSendMsgTimeout(sendTimeout); // Fixme: fix sendTimeout
		org.apache.rocketmq.client.ClientConfig clientConfig = session.getConnection().getClientConfig();
		this.producer.setNamesrvAddr(clientConfig.getNamesrvAddr());
	}

	@Override
	public Destination getDestination() throws JMSException {
		return destination;
	}

	@Override
	public void close() throws JMSException {
		// TODO: do not to directly close the producer, instead, put it into a pool.
	}

	@Override
	public void send(Message message) throws JMSException {
		this.send(this.destination, message, this.deliveryMode, this.priority, this.timeToLive);
	}

	@Override
	public void send(Message message, int deliveryMode, int priority, long timeToLive) throws JMSException {
		this.send(this.destination, message, deliveryMode, priority, timeToLive);
	}

	@Override
	public void send(Destination destination, Message message) throws JMSException {
		this.send(destination, message, this.deliveryMode, this.priority, this.timeToLive);
	}

	@Override
	public void send(Destination destination, Message message, int deliveryMode, int priority, long timeToLive) throws JMSException {
		checkClosed();
		if (destination == null) {
			throw new UnsupportedOperationException("Destination is null");
		}

		// Topic topic = (Topic)destination; // TODO: only support Topic destination now
		message.setJMSDestination(destination);
		try {
			org.apache.rocketmq.common.message.Message msg = createRmqMessage(message);

			// TODO: deal with message delay time
//			msg.setDelayTimeLevel(1);
			org.apache.rocketmq.client.producer.SendResult sendResult = producer.send(msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void send(Message message, CompletionListener completionListener) throws JMSException {
		send(this.destination, message, this.deliveryMode, this.priority, this.timeToLive, completionListener);
	}

	@Override
	public void send(Message message, int deliveryMode, int priority, long timeToLive, CompletionListener completionListener) throws JMSException {
		send(this.destination, message, deliveryMode, priority, timeToLive, completionListener);
	}

	@Override
	public void send(Destination destination, Message message, CompletionListener completionListener) throws JMSException {
		send(destination, message, this.deliveryMode, this.priority, this.timeToLive, completionListener);
	}

	@Override
	public void send(Destination destination, Message message, int deliveryMode, int priority, long timeToLive, CompletionListener completionListener) throws JMSException {

	}

	@Override
	public void setDisableMessageID(boolean value) throws JMSException {
		this.disableMessageID = value;
	}

	@Override
	public boolean getDisableMessageID() throws JMSException {
		return this.disableMessageID;
	}

	@Override
	public void setDisableMessageTimestamp(boolean value) throws JMSException {
		this.disableMessageTimestamp = value;
	}

	@Override
	public boolean getDisableMessageTimestamp() throws JMSException {
		return this.disableMessageTimestamp;
	}

	@Override
	public void setDeliveryMode(int deliveryMode) throws JMSException {
		this.deliveryMode = deliveryMode;
	}

	@Override
	public int getDeliveryMode() throws JMSException {
		return this.deliveryMode;
	}

	@Override
	public void setPriority(int defaultPriority) throws JMSException {
		this.priority = defaultPriority;
	}

	@Override
	public int getPriority() throws JMSException {
		return this.priority;
	}

	@Override
	public void setTimeToLive(long timeToLive) throws JMSException {
		this.timeToLive = timeToLive;
	}

	@Override
	public long getTimeToLive() throws JMSException {
		return this.timeToLive;
	}

	@Override
	public void setDeliveryDelay(long deliveryDelay) throws JMSException {
		this.deliveryDelay = deliveryDelay;
	}

	@Override
	public long getDeliveryDelay() throws JMSException {
		return this.deliveryDelay;
	}

	public void setTheSameProducerGroupAs(MessageProducer producer) {
		this.setProducerGroup(producer.producer.getProducerGroup());
	}

	// default: producer + producerID
	public void setProducerGroup(String producerGroup) {
		this.producer.setProducerGroup(producerGroup);
	}

	// default: 8
	public void setDefaultTopicQueueNums(int defaultTopicQueueNums) {
		this.producer.setDefaultTopicQueueNums(defaultTopicQueueNums);
	}

	// default: 3000ms
	public void setSendMsgTimeout(int sendMsgTimeout) {
		this.producer.setSendMsgTimeout(sendMsgTimeout);
	}

	// default: 4K
	public void setCompressMsgBodyOverHowmuch(int compressMsgBodyOverHowmuch) {
		this.producer.setCompressMsgBodyOverHowmuch(compressMsgBodyOverHowmuch);
	}

	// default: 2 by 3 times
	public void setRetryTimesWhenSendFailed(int retryTimesWhenSendFailed) {
		this.producer.setRetryTimesWhenSendFailed(retryTimesWhenSendFailed);
	}

	// default: 2 by 3 times
	public void setRetryTimesWhenSendAsyncFailed(int retryTimesWhenSendAsyncFailed) {
		this.producer.setRetryTimesWhenSendAsyncFailed(retryTimesWhenSendAsyncFailed);
	}

	// default: false
	public void setRetryAnotherBrokerWhenNotStoreOK(boolean retryAnotherBrokerWhenNotStoreOK) {
		this.producer.setRetryAnotherBrokerWhenNotStoreOK(retryAnotherBrokerWhenNotStoreOK);
	}

	// default: 4M
	public void setMaxMessageSize(int maxMessageSize) {
		this.producer.setMaxMessageSize(maxMessageSize);
	}

	public void start() {
		try {
			this.producer.start();
		} catch (org.apache.rocketmq.client.exception.MQClientException e) {
			// TODO: exception handling
			System.out.println(e.getErrorMessage());
		}
	}

	public void shutdown() {
		this.producer.shutdown();
	}

	public int getProducerID() {
		return producerID;
	}

	public void setProducerID(int producerID) {
		this.producerID = producerID;
	}

	protected org.apache.rocketmq.common.message.Message createRmqMessage(Message message) throws JMSException {
		return createRmqMessage((Zeze.Services.RocketMQ.Message)message);
	}

	protected org.apache.rocketmq.common.message.Message createRmqMessage(Zeze.Services.RocketMQ.Message message) throws JMSException {
		org.apache.rocketmq.common.message.Message msg = new org.apache.rocketmq.common.message.Message();

		msg.setBody(message.getBody());
		msg.setTransactionId(message.getTransactionId());
		msg.setTopic(((javax.jms.Topic)message.getJMSDestination()).getTopicName()); // FIXME: maybe error caused here

		return msg;
	}

	protected void checkClosed() throws IllegalStateException {
		if (closed) {
			throw new IllegalStateException("The producer is closed");
		}
	}
}
