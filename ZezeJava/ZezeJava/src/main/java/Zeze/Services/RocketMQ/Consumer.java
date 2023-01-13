package Zeze.Services.RocketMQ;

import org.apache.rocketmq.client.ClientConfig;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.exception.MQClientException;

public class Consumer {
	private DefaultMQPushConsumer consumer;

	public Consumer() {

	}

	public void start(String consumerGroup, ClientConfig clientConfig) throws MQClientException {
		consumer = new DefaultMQPushConsumer(consumerGroup);
		consumer.setNamesrvAddr(clientConfig.getNamesrvAddr());
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
