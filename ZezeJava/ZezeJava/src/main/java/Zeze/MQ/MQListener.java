package Zeze.MQ;

import Zeze.Builtin.MQ.BPushMessage;

@FunctionalInterface
public interface MQListener {
	void onMessage(BPushMessage.Data pushMessage);
}
