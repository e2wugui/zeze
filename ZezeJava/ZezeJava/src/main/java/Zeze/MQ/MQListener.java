package Zeze.MQ;

import Zeze.Builtin.MQ.BPushMessage;

/**
 * MQ 消息监听回调：消费者收到推送消息时调用。
 */
@FunctionalInterface
public interface MQListener {
	void onMessage(BPushMessage.Data pushMessage);
}
