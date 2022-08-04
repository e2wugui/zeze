package Zeze.Services.RocketMQ.producer;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.jms.Destination;
import Zeze.Services.RocketMQ.ZezeSession;
import org.apache.rocketmq.client.ClientConfig;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.jetbrains.annotations.NotNull;

public class ZezeTransactionProducer extends ZezeMessageProducer {

	TransactionListener transactionListener;
	ExecutorService executorService;

	public ZezeTransactionProducer(ZezeSession session, int producerId, Destination destination, int sendTimeout) {
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

	public void setTransactionListener(TransactionListener transaction) {
		this.transactionListener = transaction;
	}

	public void setExecutorService(ExecutorService executorService) {
		this.executorService = executorService;
	}
}
