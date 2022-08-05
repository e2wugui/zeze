package UnitTest.Zeze.RocketMQ;

import javax.jms.JMSException;
import Zeze.Services.RocketMQ.Connection;
import Zeze.Services.RocketMQ.ConnectionFactory;
import Zeze.Services.RocketMQ.Session;
import Zeze.Services.RocketMQ.Topic;
import Zeze.Services.RocketMQ.msg.Message;
import Zeze.Services.RocketMQ.producer.TransactionListener;
import Zeze.Services.RocketMQ.producer.TransactionProducer;

public class TestTransactionProvider {
	public static void main(String[] args) throws JMSException {
		ConnectionFactory factory = new ConnectionFactory("127.0.0.1:9876");
		Connection connection = (Connection)factory.createConnection();
		Session session = (Session)connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

		TransactionProducer producer = (TransactionProducer)session.createTransactionProducer(new Topic("TopicTest"));
		producer.setTransactionListener(new TransactionListener() {
			@Override
			public void sendHalfMessage(Message message, int deliveryMode, int priority, long timeToLive) {

			}

			@Override
			public void sendCompleteMessage(Message message, int deliveryMode, int priority, long timeToLive) {

			}
		});
		producer.start();
	}
}
