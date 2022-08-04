package Zeze.Services.RocketMQ.producer;

import Zeze.Services.RocketMQ.msg.ZezeMessage;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;

public abstract class ZezeTransactionListener implements TransactionListener {
	@Override
	public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
		// TODO: thinking half message
		return null;
	}

	@Override
	public LocalTransactionState checkLocalTransaction(MessageExt msg) {
		// TODO: thinking half message
		return null;
	}

	public abstract void sendHalfMessage(ZezeMessage message, int deliveryMode, int priority, long timeToLive);

	public abstract void sendCompleteMessage(ZezeMessage message, int deliveryMode, int priority, long timeToLive);
}
