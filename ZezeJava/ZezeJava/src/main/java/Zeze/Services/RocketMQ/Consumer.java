package Zeze.Services.RocketMQ;

import Zeze.Application;
import org.apache.rocketmq.client.ClientConfig;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.MessageListener;
import org.apache.rocketmq.client.exception.MQClientException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Consumer {
	public final @NotNull Application zeze;
	private final @NotNull DefaultMQPushConsumer consumer;

	public Consumer(@NotNull Application zeze, @NotNull String consumerGroup, @NotNull ClientConfig clientConfig) {
		this.zeze = zeze; // 保持一致的构造，先记住，以后可能有用。
		consumer = new DefaultMQPushConsumer(consumerGroup);
		consumer.setNamesrvAddr(clientConfig.getNamesrvAddr());
	}

	/**
	 * 注册消息处理器。
	 * 需要在start之前注册监听器。
	 */
	public void setMessageListener(@NotNull MessageListener messageListener) {
		consumer.setMessageListener(messageListener);
	}

	/**
	 * 订阅消息。
	 * 需要在start之前调用。
	 */
	public void subscribe(@NotNull String topic, @Nullable String subExpression) throws MQClientException {
		consumer.subscribe(topic, subExpression);
	}

	public void start() throws MQClientException {
		consumer.start();
	}

	public void stop() {
		consumer.shutdown();
	}

	public @NotNull DefaultMQPushConsumer getConsumer() {
		return consumer;
	}
}
