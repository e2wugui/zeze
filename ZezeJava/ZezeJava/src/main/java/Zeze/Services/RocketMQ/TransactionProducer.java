package Zeze.Services.RocketMQ;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.jms.Destination;
import javax.jms.JMSException;
import org.apache.rocketmq.client.ClientConfig;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.jetbrains.annotations.NotNull;

public class TransactionProducer extends MessageProducer {

	TransactionListener transactionListener;
	ExecutorService executorService;

	public TransactionProducer(Session session, int producerId, Destination destination, int sendTimeout) {
		super();
		super.producer = new org.apache.rocketmq.client.producer.TransactionMQProducer("producer" + producerId);
		super.producerID = producerId;
		super.session = session;
		super.destination = destination;

		ClientConfig clientConfig = session.getConnection().getClientConfig();
		this.producer.setNamesrvAddr(clientConfig.getNamesrvAddr());

		// TODO: customize thread pool
		executorService = new ThreadPoolExecutor(2, 5, 100, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(2000), new ThreadFactory() {
			@Override
			public Thread newThread(@NotNull Runnable r) {
				Thread thread = new Thread(r);
				thread.setName("client-transaction-msg-check-thread");
				return thread;
			}
		});
	}

	@Override
	public void start() {
		if (transactionListener == null) {
			throw new IllegalStateException("transactionListener is null");
		}
		((org.apache.rocketmq.client.producer.TransactionMQProducer)this.producer).setTransactionListener(transactionListener);
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
