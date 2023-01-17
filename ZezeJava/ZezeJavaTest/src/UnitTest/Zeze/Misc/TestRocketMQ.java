package UnitTest.Zeze.Misc;

import java.nio.charset.StandardCharsets;
import demo.App;
import org.apache.rocketmq.client.ClientConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class TestRocketMQ {
	@Before
	public void before() throws Throwable {
		App.Instance.Start();
	}

	@After
	public void after() throws Throwable {
		App.Instance.Stop();
	}

	@Test
	public void testProducer() throws Throwable {
		// todo，@项洋呈 自动读取配置还是可以手动在代码里配置一下。
		var clientConfig = new ClientConfig();
		App.Instance.RocketMQProducer.start("testRocketMQ", clientConfig);
		App.Instance.RocketMQConsumer.start("testRocketMQ", clientConfig);

		// 发送普通消息
		{
			var msg = new org.apache.rocketmq.common.message.Message();
			msg.setBody("body".getBytes(StandardCharsets.UTF_8));
			msg.setTransactionId("1");
			msg.setTopic("topic");

			App.Instance.RocketMQProducer.sendMessage(msg);
			// todo, @项洋呈 订阅消息，验证消息收到。
			// App.Instance.RocketMQConsumer.getConsumer().
		}
		// 发送事务消息
		{
			App.Instance.Zeze.newProcedure(() -> {
				var msg = new org.apache.rocketmq.common.message.Message();
				msg.setBody("body2".getBytes(StandardCharsets.UTF_8));
				msg.setTransactionId("2");
				msg.setTopic("topic2");
				App.Instance.RocketMQProducer.sendMessageInTransaction(msg);
				return 0;
			}, "").call();
			// todo, @项洋呈 订阅消息，验证消息收到。
			// App.Instance.RocketMQConsumer.getConsumer().
		}
	}
}
