package UnitTest.Zeze.Misc;

import java.nio.charset.StandardCharsets;
import Zeze.Services.RocketMQ.Consumer;
import demo.App;
import org.apache.rocketmq.client.ClientConfig;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class TestRocketMQ {
	@Before
	public void before() throws Exception {
		App.Instance.Start();
	}

	@After
	public void after() throws Exception {
		//App.Instance.Stop();
	}

	@Test
	public void testProducer() throws Exception {
		var clientConfig = new ClientConfig();
		App.Instance.RocketMQProducer.start("testRocketMQ", clientConfig);
		var consumer = new Consumer(App.Instance.Zeze, "testRocketMQ", clientConfig);

		consumer.setMessageListener((MessageListenerConcurrently)(msgs, context) -> {
			for (MessageExt msg : msgs) {
				System.out.println("Receive: ");
				System.out.println("\tBody: " + new String(msg.getBody()));
				System.out.println("\tTags: " + msg.getTags());
				System.out.println("\tKeys: " + msg.getKeys());
				System.out.println("\tTopic: " + msg.getTopic());
				System.out.println("\tMsgId: " + msg.getMsgId());
			}
			return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
		});
		consumer.subscribe("topic2", "*");
		consumer.start();

		try {
			// 发送普通消息
			{
				var msg = new org.apache.rocketmq.common.message.Message();
				msg.setBody("body".getBytes(StandardCharsets.UTF_8));
				msg.setTransactionId("1");
				msg.setTopic("topic2");

				App.Instance.RocketMQProducer.sendMessage(msg);
			}
			// 发送事务消息
			{
				var msg = new org.apache.rocketmq.common.message.Message();
				msg.setBody("body2".getBytes(StandardCharsets.UTF_8));
				msg.setTransactionId("2");
				msg.setTopic("topic2");

				App.Instance.RocketMQProducer.sendMessageWithTransaction(msg, () -> {
					/* local transaction bind to this message */
					return 0;
				});
			}
		} finally {
			consumer.stop();
		}
	}
}
