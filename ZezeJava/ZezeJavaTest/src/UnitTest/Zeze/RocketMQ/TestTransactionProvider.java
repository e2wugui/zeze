package UnitTest.Zeze.RocketMQ;

import javax.jms.JMSException;
import Zeze.Services.RocketMQ.Connection;
import Zeze.Services.RocketMQ.ConnectionFactory;
import Zeze.Services.RocketMQ.Session;
import Zeze.Services.RocketMQ.TextMessage;
import Zeze.Services.RocketMQ.Topic;
import Zeze.Services.RocketMQ.Message;
import Zeze.Services.RocketMQ.TransactionListener;
import Zeze.Services.RocketMQ.TransactionProducer;

class BankA {
	private int balance = 0;

	public void add(int amount) throws Exception {
		balance += amount;
	}

	public void sub(int amount) throws Exception {
		balance -= amount;
	}

	@Override
	public String toString() {
		return "BankA{" + "balance=" + balance + '}';
	}
}

class BankAddBalanceTransaction extends TransactionListener {
	BankA bankA;

	public BankAddBalanceTransaction(BankA bankA) {
		this.bankA = bankA;
	}

	@Override
	public State sendHalfMessage(Message message, Object arg) {
		try {
			bankA.add(100);
			return State.COMMIT_MESSAGE;
		} catch (Exception e) {
			e.printStackTrace();
			return State.ROLLBACK_MESSAGE;
		}
	}

	@Override
	public State checkTransaction(Message message) {
		return State.COMMIT_MESSAGE;
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
		producer.setTransactionListener(new BankAddBalanceTransaction(bankA));
		producer.start();
		producer.sendMessageInTransaction(new TextMessage("Hello World"));
	}
}
