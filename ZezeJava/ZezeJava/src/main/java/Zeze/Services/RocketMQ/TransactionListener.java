package Zeze.Services.RocketMQ;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.rocketmq.client.producer.LocalTransactionState;

public abstract class TransactionListener implements org.apache.rocketmq.client.producer.TransactionListener {
	public enum State {
		COMMIT_MESSAGE,
		ROLLBACK_MESSAGE,
		UNKNOW,
	}

	private AtomicInteger transactionIndex = new AtomicInteger(0);
	private ConcurrentHashMap<String, Integer> localTrans = new ConcurrentHashMap<String, Integer>(); // <TransactionID, Status>

	@Override
	public org.apache.rocketmq.client.producer.LocalTransactionState executeLocalTransaction(org.apache.rocketmq.common.message.Message msg, Object arg) {
		switch (sendHalfMessage(new Message(msg), arg)) {
		case COMMIT_MESSAGE:
			return LocalTransactionState.COMMIT_MESSAGE;
		case ROLLBACK_MESSAGE:
			return LocalTransactionState.ROLLBACK_MESSAGE;
		default:
			return LocalTransactionState.UNKNOW;
		}
	}

	@Override
	public org.apache.rocketmq.client.producer.LocalTransactionState checkLocalTransaction(org.apache.rocketmq.common.message.MessageExt msg) {
		switch (checkTransaction(new Message(msg))) {
		case COMMIT_MESSAGE:
			return LocalTransactionState.COMMIT_MESSAGE;
		case ROLLBACK_MESSAGE:
			return LocalTransactionState.ROLLBACK_MESSAGE;
		default:
			return LocalTransactionState.UNKNOW;
		}
	}

	public abstract TransactionListener.State sendHalfMessage(Message message, Object arg);

	// default: check by every 60s
	public abstract TransactionListener.State checkTransaction(Message message);
}
