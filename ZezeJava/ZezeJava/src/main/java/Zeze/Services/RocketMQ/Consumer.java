package Zeze.Services.RocketMQ;

import org.apache.rocketmq.client.ClientConfig;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.exception.MQClientException;

public class Consumer {
	public Zeze.Application Zeze;
	private DefaultMQPushConsumer consumer;

	public Consumer(Zeze.Application zeze) {
		Zeze = zeze; // 保持一致的构造，先记住，以后可能有用。
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
