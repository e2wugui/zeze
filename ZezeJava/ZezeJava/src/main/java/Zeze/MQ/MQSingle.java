package Zeze.MQ;

import Zeze.Builtin.MQ.BSendMessage;

public class MQSingle {
	private final String topic;
	private final int partitionId;

	public MQSingle(String topic, int partitionId) {
		this.topic = topic;
		this.partitionId = partitionId;
	}

	public double load() {
		return 0.0;
	}
	public void sendMessage(BSendMessage.Data message) {

	}
}
