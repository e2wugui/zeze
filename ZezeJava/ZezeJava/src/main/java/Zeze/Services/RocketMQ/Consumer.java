package Zeze.Services.RocketMQ;

import org.apache.rocketmq.client.ClientConfig;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.MessageListener;
import org.apache.rocketmq.client.exception.MQClientException;

public class Consumer {
	public final Zeze.Application zeze;
	private final DefaultMQPushConsumer consumer;

	public Consumer(Zeze.Application zeze, String consumerGroup, ClientConfig clientConfig) {
		this.zeze = zeze; // 保持一致的构造，先记住，以后可能有用。
		consumer = new DefaultMQPushConsumer(consumerGroup);
		consumer.setNamesrvAddr(clientConfig.getNamesrvAddr());
	}

	/**
	 * 注册消息处理器。
	 * 需要在start之前注册监听器。
	 * @param messageListener listener
	 */
	public void setMessageListener(MessageListener messageListener) {
		consumer.setMessageListener(messageListener);
	}

	/**
	 * 订阅消息。
	 * 需要在start之前调用。
	 * @param topic 主题
	 * @param subExpression 过滤器
	 * @throws MQClientException exception
	 */
	public void subscribe(String topic, String subExpression) throws MQClientException {
		consumer.subscribe(topic, subExpression);
	}

	public void start() throws MQClientException {
		consumer.start();
	}

	public void stop() {
		if (null != consumer)
			consumer.shutdown();
	}

	// 完全开放出去，不进行其他包装了。
	public DefaultMQPushConsumer getConsumer() {
		return consumer;
	}
}
