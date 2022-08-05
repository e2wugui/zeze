package Zeze.Services.RocketMQ.producer;

import Zeze.Services.RocketMQ.msg.Message;

public abstract class TransactionListener implements org.apache.rocketmq.client.producer.TransactionListener {
	@Override
	public org.apache.rocketmq.client.producer.LocalTransactionState executeLocalTransaction(org.apache.rocketmq.common.message.Message msg, Object arg) {
		// TODO: thinking half message
		return null;
	}

	@Override
	public org.apache.rocketmq.client.producer.LocalTransactionState checkLocalTransaction(org.apache.rocketmq.common.message.MessageExt msg) {
		// TODO: thinking complete message
		return null;
	}

	public abstract void sendHalfMessage(Message message, int deliveryMode, int priority, long timeToLive);

	public abstract void sendCompleteMessage(Message message, int deliveryMode, int priority, long timeToLive);
}
