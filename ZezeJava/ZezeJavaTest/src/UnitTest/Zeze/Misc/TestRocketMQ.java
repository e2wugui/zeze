package UnitTest.Zeze.Misc;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Services.RocketMQ.Consumer;
import demo.App;
import org.apache.rocketmq.client.ClientConfig;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.common.RemotingHelper;
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
		// App.Instance.Stop();
	}

	@Test
	public void testProducer() throws Exception {
		App.Instance.RocketMQProducer.start();
		var consumer = new Consumer(App.Instance.Zeze, "testRocketMQ", new ClientConfig());

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
				var msg = new Message();
				msg.setBody("body".getBytes(StandardCharsets.UTF_8));
				msg.setTransactionId("1");
				msg.setTopic("topic2");
				App.Instance.RocketMQProducer.sendMessage(msg);
			}
			// 发送事务消息
			{
				var msg = new Message();
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

	static class TransactionListenerImpl implements TransactionListener {
		private final AtomicInteger transactionIndex = new AtomicInteger();
		private final ConcurrentHashMap<String, Integer> localTrans = new ConcurrentHashMap<>();

		@Override
		public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
			System.out.println("executeLocalTransaction: " + msg);
			localTrans.put(msg.getTransactionId(), transactionIndex.getAndIncrement() % 3); // 0,1,2,0,1,2,...
			return LocalTransactionState.UNKNOW;
		}

		@Override
		public LocalTransactionState checkLocalTransaction(MessageExt msg) {
			System.out.println("checkLocalTransaction: " + msg);
			Integer status = localTrans.get(msg.getTransactionId());
			if (status != null) {
				switch (status) {
				case 0:
					return LocalTransactionState.UNKNOW;
				case 1:
					return LocalTransactionState.COMMIT_MESSAGE;
				case 2:
					return LocalTransactionState.ROLLBACK_MESSAGE;
				}
			}
			return LocalTransactionState.COMMIT_MESSAGE;
		}
	}

	public static void main(String[] args) throws Exception {
		var producer = new TransactionMQProducer("please_rename_unique_group_name");
		producer.setExecutorService(new ThreadPoolExecutor(2, 5, 100, TimeUnit.SECONDS, new ArrayBlockingQueue<>(2000),
				r -> new Thread(r, "client-transaction-msg-check-thread")));
		producer.setTransactionListener(new TransactionListenerImpl());
		producer.setNamesrvAddr("127.0.0.1:9876");
		producer.start();

		var tags = new String[]{"TagA", "TagB", "TagC", "TagD", "TagE"};
		for (int i = 0; i < 10; i++) {
			SendResult sendResult = producer.sendMessageInTransaction(new Message(
					"TopicTest", tags[i % tags.length], "KEY" + i,
					("Hello RocketMQ " + i).getBytes(RemotingHelper.DEFAULT_CHARSET)), null);
			System.out.println("send half msg: " + sendResult);
			Thread.sleep(10);
		}

		Thread.sleep(Integer.MAX_VALUE);
		producer.shutdown();
	}
}
