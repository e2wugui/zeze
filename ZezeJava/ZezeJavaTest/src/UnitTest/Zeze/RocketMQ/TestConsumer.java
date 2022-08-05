package UnitTest.Zeze.RocketMQ;

import javax.jms.JMSException;
import Zeze.Services.RocketMQ.Connection;
import Zeze.Services.RocketMQ.ConnectionFactory;
import Zeze.Services.RocketMQ.Session;
import Zeze.Services.RocketMQ.Topic;
import Zeze.Services.RocketMQ.MessageConsumer;
import Zeze.Services.RocketMQ.MessageListenerConcurrently;
import Zeze.Services.RocketMQ.Message;
import org.apache.rocketmq.client.exception.MQClientException;

class BankB {
	private int balance = 0;

	public void add(int amount) {
		balance += amount;
	}

	public void sub(int amount) {
		balance -= amount;
	}

	@Override
	public String toString() {
		return "BankB{" + "balance=" + balance + '}';
	}
}

public class TestConsumer {
	public static void main(String[] args) throws JMSException, MQClientException {

		// Local Transaction Environment
		BankB bankB = new BankB();
		System.out.println(bankB);


		ConnectionFactory factory = new ConnectionFactory("127.0.0.1:9876");
		Connection connection = (Connection)factory.createConnection();
		Session session = (Session)connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

		Topic topic = new Topic("TopicTest");
		MessageConsumer consumer = (MessageConsumer)session.createConsumer(topic);
		consumer.setMessageListener(new MessageListenerConcurrently() {
			@Override
			public void onMessage(Message message) {
				System.out.println("Received: " + new String(message.getBody()));
			}
		});
		consumer.start();
	}
}
