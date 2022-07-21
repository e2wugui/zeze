package Zeze.Services.RocketMQ;

import java.util.List;
import javax.jms.JMSConsumer;
import javax.jms.JMSRuntimeException;
import javax.jms.Message;
import javax.jms.MessageListener;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;

public class ZezeConsumer implements JMSConsumer {

	private org.apache.rocketmq.client.consumer.DefaultMQPushConsumer consumer;

	public ZezeConsumer(String name) {
		this.consumer = new DefaultMQPushConsumer(name);
	}

	@Override
	public String getMessageSelector() {
		return null;
	}

	@Override
	public MessageListener getMessageListener() throws JMSRuntimeException {
		return null;
	}

	@Override
	public void setMessageListener(MessageListener listener) throws JMSRuntimeException {

	}

	@Override
	public Message receive() {
		return null;
	}

	@Override
	public Message receive(long timeout) {
		return null;
	}

	@Override
	public Message receiveNoWait() {
		return null;
	}

	@Override
	public void close() {

	}

	@Override
	public <T> T receiveBody(Class<T> c) {
		return null;
	}

	@Override
	public <T> T receiveBody(Class<T> c, long timeout) {
		return null;
	}

	@Override
	public <T> T receiveBodyNoWait(Class<T> c) {
		return null;
	}

	public static void main(String[] args) throws MQClientException {

		// Instantiate with specified consumer group name.
		DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("CONSUMER");

		// Specify name server addresses.
		consumer.setNamesrvAddr("localhost:9876");

		// Subscribe one more more topics to consume.
		consumer.subscribe("TopicTest", "*");
		// Register callback to execute on arrival of messages fetched from brokers.
		consumer.registerMessageListener(new MessageListenerConcurrently() {

			@Override
			public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs,
															ConsumeConcurrentlyContext context) {
				System.out.printf("%s Receive New Messages: %s %n", Thread.currentThread().getName(), msgs);
				return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
			}
		});

		//Launch the consumer instance.
		consumer.start();

		System.out.printf("Consumer Started.%n");
	}
}
