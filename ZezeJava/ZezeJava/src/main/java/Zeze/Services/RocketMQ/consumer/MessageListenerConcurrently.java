package Zeze.Services.RocketMQ.consumer;

import javax.jms.MessageListener;
import Zeze.Services.RocketMQ.msg.Message;

public abstract class MessageListenerConcurrently implements MessageListener {

	@Override
	public void onMessage(javax.jms.Message message) {
		onMessage((Message)message);
	}

	public abstract void onMessage(Message message);
}
