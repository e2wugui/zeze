package Zeze.Services.RocketMQ.msg;

import javax.jms.JMSException;
import Zeze.Services.RocketMQ.msg.ZezeMessage;

public class ZezeTextMessage extends ZezeMessage implements javax.jms.TextMessage {

	protected String text;

	public ZezeTextMessage() {
	}

	public ZezeTextMessage(String text) {
		this.text = text;
		super.setBody(text.getBytes());
	}

	@Override
	public void setText(String string) throws JMSException {
		text = string;
		super.setBody(text.getBytes());
	}

	@Override
	public String getText() throws JMSException {
		return text;
	}
}
