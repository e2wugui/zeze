package Zeze.Services.RocketMQ;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Services.RocketMQ.Message;
import org.apache.rocketmq.client.producer.LocalTransactionState;

public abstract class TransactionListener implements org.apache.rocketmq.client.producer.TransactionListener {

	private AtomicInteger transactionIndex = new AtomicInteger(0);
	private ConcurrentHashMap<String, Integer> localTrans = new ConcurrentHashMap<String, Integer>(); // <TransactionID, Status>

	@Override
	public org.apache.rocketmq.client.producer.LocalTransactionState executeLocalTransaction(org.apache.rocketmq.common.message.Message msg, Object arg) {
		// TODO: thinking half message
		Message message = new Message(msg);

		int value = transactionIndex.getAndIncrement();

		//
		// TODO: thinking how to organize the transaction
		//

		return LocalTransactionState.UNKNOW;
	}

	@Override
	public org.apache.rocketmq.client.producer.LocalTransactionState checkLocalTransaction(org.apache.rocketmq.common.message.MessageExt msg) {

		Integer status = localTrans.get(msg.getTransactionId());
		if (null != status) {
			switch (status) {
			case 0:
				return LocalTransactionState.UNKNOW;
			case 1:
				return LocalTransactionState.COMMIT_MESSAGE;
			case 2:
				return LocalTransactionState.ROLLBACK_MESSAGE;
			default:
				break;
			}
		}
		return LocalTransactionState.COMMIT_MESSAGE;
	}

	public abstract void sendHalfMessage(Message message, Object arg);

	public abstract void checkTransaction(Message message);
}
