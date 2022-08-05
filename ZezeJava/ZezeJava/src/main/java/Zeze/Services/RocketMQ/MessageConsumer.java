package Zeze.Services.RocketMQ;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageListener;

public class MessageConsumer implements javax.jms.MessageConsumer {

	org.apache.rocketmq.client.consumer.DefaultMQPushConsumer consumer;
	private Session session;
	private final Destination destination; // default destination
	private final String selector;
	private final AtomicReference<javax.jms.MessageListener> messageListener = new AtomicReference<>();
	private int consumerId;

	public MessageConsumer(Session session, Destination destination, int consumerId, String selector) throws JMSException {
		this.consumer = new org.apache.rocketmq.client.consumer.DefaultMQPushConsumer("consumer" + consumerId);
		this.session = session;
		this.destination = destination;
		this.selector = selector;
		this.consumerId = consumerId;

		org.apache.rocketmq.client.ClientConfig clientConfig = session.getConnection().getClientConfig();
		this.consumer.setNamesrvAddr(clientConfig.getNamesrvAddr());
		try {
			this.consumer.subscribe(((Topic)destination).getTopicName(), "*");
		} catch (org.apache.rocketmq.client.exception.MQClientException e) {
			// TODO: exception handling
			System.out.println(e.getErrorMessage());
		}
	}

	@Override
	public String getMessageSelector() throws JMSException {
		return selector;
	}

	@Override
	public MessageListener getMessageListener() throws JMSException {
		return this.messageListener.get();
	}

	@Override
	public void setMessageListener(MessageListener listener) throws JMSException {
		this.messageListener.set(listener);
	}

	@Override
	public javax.jms.Message receive() throws JMSException {
		return null;
	}

	@Override
	public javax.jms.Message receive(long timeout) throws JMSException {
		return null;
	}

	@Override
	public javax.jms.Message receiveNoWait() throws JMSException {
		return null;
	}

	@Override
	public void close() throws JMSException {

	}

	public void start() {
		if (messageListener.get() instanceof MessageListenerConcurrently) {
			this.consumer.registerMessageListener(new org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently() {
				@Override
				public org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus consumeMessage(List<org.apache.rocketmq.common.message.MessageExt> msgs, org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext context) {
					try {
						for (org.apache.rocketmq.common.message.MessageExt msg : msgs) {
							messageListener.get().onMessage(new Message(msg));
						}
					} catch (Exception e) {
						e.printStackTrace();
						return org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus.RECONSUME_LATER;
					}
					return org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
				}
			});
		}
		if (messageListener.get() instanceof MessageListenerOrderly) {
			this.consumer.registerMessageListener(new org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly() {
				@Override
				public org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus consumeMessage(List<org.apache.rocketmq.common.message.MessageExt> msgs, org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyContext context) {
					try {
						int i = 0;
						for (org.apache.rocketmq.common.message.MessageExt msg : msgs) {
							System.out.println("consumeMessage " + i++);
							messageListener.get().onMessage(new Message(msg));
						}
					} catch (Exception e) {
						e.printStackTrace();
						return org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
					}
					return org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus.SUCCESS;
				}
			});
		}
		try {
			this.consumer.start();
		} catch (org.apache.rocketmq.client.exception.MQClientException e) {
			// TODO: exception handling
			System.out.println(e.getErrorMessage());
		}
	}

	@Deprecated // maybe we don't need this method
	public void subscribe(Topic topic) throws JMSException {
		try {
			this.consumer.subscribe(topic.getTopicName(), this.selector);
		} catch (org.apache.rocketmq.client.exception.MQClientException e) {
			// TODO: exception handling
			System.out.println(e.getErrorMessage());
		}
	}

	@Deprecated // maybe we don't need this method
	public void unsubscribe(Topic topic) throws JMSException {
		this.consumer.unsubscribe(topic.getTopicName());
	}

	// default: CLUSTERING
	public void useClusteringMessageModel() {
		this.consumer.setMessageModel(org.apache.rocketmq.common.protocol.heartbeat.MessageModel.CLUSTERING);
	}

	// default: CLUSTERING
	public void useBroadcastMessageModel() {
		this.consumer.setMessageModel(org.apache.rocketmq.common.protocol.heartbeat.MessageModel.BROADCASTING);
	}

	// default: CONSUME_FROM_LAST_OFFSET
	public void setConsumeFromLastOffset() {
		this.consumer.setConsumeFromWhere(org.apache.rocketmq.common.consumer.ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
	}

	// default: CONSUME_FROM_LAST_OFFSET
	public void setConsumeFromFirstOffset() {
		this.consumer.setConsumeFromWhere(org.apache.rocketmq.common.consumer.ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
	}

	// default: CONSUME_FROM_LAST_OFFSET
	public void setConsumeFromTimestamp() {
		this.consumer.setConsumeFromWhere(org.apache.rocketmq.common.consumer.ConsumeFromWhere.CONSUME_FROM_TIMESTAMP);
	}

	// default: 20
	public void setConsumeThreadMin(int consumeThreadMin) {
		this.consumer.setConsumeThreadMin(consumeThreadMin);
	}

	// default: 20
	public void setConsumeThreadMax(int consumeThreadMax) {
		this.consumer.setConsumeThreadMax(consumeThreadMax);
	}

	// default: 0
	public void setPullInterval(int pullInterval) {
		this.consumer.setPullInterval(pullInterval);
	}

	// default: 32
	public void setPullBatchSize(int pullBatchSize) {
		this.consumer.setPullBatchSize(pullBatchSize);
	}

	// default: -1 (representing 16)
	public void setMaxReconsumeTimes(int maxReconsumeTimes) {
		this.consumer.setMaxReconsumeTimes(maxReconsumeTimes);
	}

	// default: 15 min
	public void setConsumeTimeout(int consumeTimeout) {
		this.consumer.setConsumeTimeout(consumeTimeout);
	}
}
