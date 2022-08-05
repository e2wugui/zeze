package UnitTest.Zeze.RocketMQ;

import javax.jms.JMSException;
import Zeze.Services.RocketMQ.Connection;
import Zeze.Services.RocketMQ.ConnectionFactory;
import Zeze.Services.RocketMQ.Session;
import Zeze.Services.RocketMQ.Topic;
import Zeze.Services.RocketMQ.Message;
import Zeze.Services.RocketMQ.TransactionListener;
import Zeze.Services.RocketMQ.TransactionProducer;

class BankA {
	private int balance = 0;

	public void add(int amount) throws Exception {
		balance += amount;

		throw new Exception();
	}

	public void sub(int amount) {
		balance -= amount;
	}

	@Override
	public String toString() {
		return "BankA{" + "balance=" + balance + '}';
	}
}

public class TestTransactionProvider {
	public static void main(String[] args) throws JMSException {

		// Local Transaction Environment
		BankA bankA = new BankA();
		System.out.println(bankA);

		ConnectionFactory factory = new ConnectionFactory("127.0.0.1:9876");
		Connection connection = (Connection)factory.createConnection();
		Session session = (Session)connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

		TransactionProducer producer = (TransactionProducer)session.createTransactionProducer(new Topic("TopicTest"));
		producer.setTransactionListener(new TransactionListener() {
			@Override
			public void sendHalfMessage(Message message, Object arg) {

			}

			@Override
			public void checkTransaction(Message message) {

			}
		});
		producer.start();
	}
}
