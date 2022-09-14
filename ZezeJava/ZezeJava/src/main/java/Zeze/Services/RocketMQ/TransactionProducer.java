package Zeze.Services.RocketMQ;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.jms.Destination;
import javax.jms.JMSException;
import org.apache.rocketmq.client.ClientConfig;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.client.producer.TransactionMQProducer;

public class TransactionProducer extends MessageProducer {

	TransactionListener transactionListener;
	ExecutorService executorService;

	public TransactionProducer(Session session, int producerId, Destination destination, int sendTimeout) {
		super();
		super.producer = new TransactionMQProducer("producer" + producerId);
		super.producerID = producerId;
		super.session = session;
		super.destination = destination;

		ClientConfig clientConfig = session.getConnection().getClientConfig();
		this.producer.setNamesrvAddr(clientConfig.getNamesrvAddr());

		// TODO: customize thread pool
		executorService = new ThreadPoolExecutor(2, 5, 100, TimeUnit.SECONDS, new ArrayBlockingQueue<>(2000), r -> {
			Thread thread = new Thread(r);
			thread.setName("client-transaction-msg-check-thread");
			return thread;
		});
	}

	@Override
	public void start() {
		if (transactionListener == null) {
			throw new IllegalStateException("transactionListener is null");
		}
		((TransactionMQProducer)this.producer).setTransactionListener(transactionListener);
		super.start();
	}

	public void sendMessageInTransaction(Message message) throws JMSException {
		sendMessageInTransaction(message, null);
	}

	public void sendMessageInTransaction(Message message, Object arg) throws JMSException {
		try {
			this.producer.sendMessageInTransaction(createRmqMessage(message), arg);
		} catch (Exception e) {
			throw new JMSException(e.getMessage());
		}
	}

	public void setTransactionListener(TransactionListener transaction) {
		this.transactionListener = transaction;
	}

	public void setExecutorService(ExecutorService executorService) {
		this.executorService = executorService;
	}
}
