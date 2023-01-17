package UnitTest.Zeze.RocketMQ;

import Zeze.Services.RocketMQ.Consumer;
import Zeze.Services.RocketMQ.Producer;
import demo.App;
import org.apache.rocketmq.client.ClientConfig;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;

public class TestRocketMQ {
	static Zeze.Application zeze = null;

	static Zeze.Application getZeze() throws Throwable {
		if (zeze == null)
		{
			App.getInstance().Start();
			zeze = App.getInstance().getZeze();
		}
		return zeze;
	}
}

class RunConsumer {
	public static void main(String[] args) throws Throwable {
		var config = new ClientConfig();
		config.setNamesrvAddr("localhost:9876");

		Consumer consumer = new Consumer(TestRocketMQ.getZeze());

		consumer.start("Group1", config);
	}
}

class RunProducer {
	public static void main(String[] args) throws Throwable {
		var config = new ClientConfig();
		config.setNamesrvAddr("localhost:9876");

		Producer producer = new Producer(TestRocketMQ.getZeze());
		producer.start("Group1", config);
		producer.sendMessage(new Message("TopicTest", "Hello, World!".getBytes()));
		producer.stop();
	}
}

