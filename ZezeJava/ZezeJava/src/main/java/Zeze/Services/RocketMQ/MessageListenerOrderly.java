package Zeze.Services.RocketMQ;

import javax.jms.MessageListener;

public abstract class MessageListenerOrderly implements MessageListener {

	@Override
	public void onMessage(javax.jms.Message message) {
		onMessage((Message)message);
	}

	public abstract void onMessage(Message message);
}
