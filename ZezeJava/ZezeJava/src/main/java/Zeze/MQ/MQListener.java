package Zeze.MQ;

import Zeze.Builtin.MQ.BPushMessage;

public interface MQListener {
	void onMessage(BPushMessage.Data pushMessage);
}
