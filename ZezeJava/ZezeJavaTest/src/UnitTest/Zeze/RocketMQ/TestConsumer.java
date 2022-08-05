package UnitTest.Zeze.RocketMQ;

import javax.jms.JMSException;
import Zeze.Services.RocketMQ.Connection;
import Zeze.Services.RocketMQ.ConnectionFactory;
import Zeze.Services.RocketMQ.Session;
import Zeze.Services.RocketMQ.Topic;
import Zeze.Services.RocketMQ.consumer.MessageConsumer;
import Zeze.Services.RocketMQ.consumer.MessageListenerConcurrently;
import Zeze.Services.RocketMQ.msg.Message;
import org.apache.rocketmq.client.exception.MQClientException;

public class TestConsumer {
	public static void main(String[] args) throws JMSException, MQClientException {
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
