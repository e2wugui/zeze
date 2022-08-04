package Zeze.Services.RocketMQ.consumer;

import javax.jms.Message;
import javax.jms.MessageListener;
import Zeze.Services.RocketMQ.msg.ZezeMessage;

public abstract class ZezeMessageListenerConcurrently implements MessageListener {

	@Override
	public void onMessage(Message message) {
		onMessage((ZezeMessage)message);
	}

	public abstract void onMessage(ZezeMessage message);
}
