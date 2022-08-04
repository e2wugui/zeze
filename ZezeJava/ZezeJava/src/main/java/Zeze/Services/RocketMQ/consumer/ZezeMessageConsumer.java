package Zeze.Services.RocketMQ.consumer;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import Zeze.Services.RocketMQ.ZezeSession;
import Zeze.Services.RocketMQ.ZezeTopic;
import Zeze.Services.RocketMQ.msg.ZezeMessage;
import org.apache.rocketmq.client.ClientConfig;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;

public class ZezeMessageConsumer implements javax.jms.MessageConsumer {

	org.apache.rocketmq.client.consumer.DefaultMQPushConsumer consumer;
	private ZezeSession session;
	private final Destination destination; // default destination
	private final String selector;
	private final AtomicReference<javax.jms.MessageListener> messageListener = new AtomicReference<>();
	private int consumerId;

	public ZezeMessageConsumer(ZezeSession session, Destination destination, int consumerId, String selector) throws JMSException {
		this.consumer = new org.apache.rocketmq.client.consumer.DefaultMQPushConsumer("consumer" + consumerId);
		this.session = session;
		this.destination = destination;
		this.selector = selector;
		this.consumerId = consumerId;

		ClientConfig clientConfig = session.getConnection().getClientConfig();
		this.consumer.setNamesrvAddr(clientConfig.getNamesrvAddr());
		try {
			this.consumer.subscribe(((ZezeTopic)destination).getTopicName(), "*");
		} catch (MQClientException e) {
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
	public Message receive() throws JMSException {
		return null;
	}

	@Override
	public Message receive(long timeout) throws JMSException {
		return null;
	}

	@Override
	public Message receiveNoWait() throws JMSException {
		return null;
	}

	@Override
	public void close() throws JMSException {

	}

	public void start() {
		if (messageListener.get() instanceof ZezeMessageListenerConcurrently) {
			this.consumer.registerMessageListener(new MessageListenerConcurrently() {
				@Override
				public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
					try {
						for (MessageExt msg : msgs) {
							messageListener.get().onMessage(new ZezeMessage(msg));
						}
					} catch (Exception e) {
						e.printStackTrace();
						return ConsumeConcurrentlyStatus.RECONSUME_LATER;
					}
					return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
				}
			});
		}
		if (messageListener.get() instanceof ZezeMessageListenerOrderly) {
			this.consumer.registerMessageListener(new MessageListenerOrderly() {
				@Override
				public ConsumeOrderlyStatus consumeMessage(List<MessageExt> msgs, ConsumeOrderlyContext context) {
					try {
						int i = 0;
						for (MessageExt msg : msgs) {
							System.out.println("consumeMessage " + i++);
							messageListener.get().onMessage(new ZezeMessage(msg));
						}
					} catch (Exception e) {
						e.printStackTrace();
						return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
					}
					return ConsumeOrderlyStatus.SUCCESS;
				}
			});
		}
		try {
			this.consumer.start();
		} catch (MQClientException e) {
			// TODO: exception handling
			System.out.println(e.getErrorMessage());
		}
	}

	@Deprecated // maybe we don't need this method
	public void subscribe(ZezeTopic topic) throws JMSException {
		try {
			this.consumer.subscribe(topic.getTopicName(), this.selector);
		} catch (MQClientException e) {
			// TODO: exception handling
			System.out.println(e.getErrorMessage());
		}
	}

	@Deprecated // maybe we don't need this method
	public void unsubscribe(ZezeTopic topic) throws JMSException, MQClientException {
		this.consumer.unsubscribe(topic.getTopicName());
	}

	// default: CLUSTERING
	public void useClusteringMessageModel() {
		this.consumer.setMessageModel(MessageModel.CLUSTERING);
	}

	// default: CLUSTERING
	public void useBroadcastMessageModel() {
		this.consumer.setMessageModel(MessageModel.BROADCASTING);
	}

	// default: CONSUME_FROM_LAST_OFFSET
	public void setConsumeFromLastOffset() {
		this.consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
	}

	// default: CONSUME_FROM_LAST_OFFSET
	public void setConsumeFromFirstOffset() {
		this.consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
	}

	// default: CONSUME_FROM_LAST_OFFSET
	public void setConsumeFromTimestamp() {
		this.consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_TIMESTAMP);
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
