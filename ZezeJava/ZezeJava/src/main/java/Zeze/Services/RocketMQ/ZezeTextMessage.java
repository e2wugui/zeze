package Zeze.Services.RocketMQ;

import javax.jms.JMSException;

public class ZezeTextMessage extends ZezeMessage implements javax.jms.TextMessage {

	private String text;

	public ZezeTextMessage(String text) {
		this.text = text;
	}

	@Override
	public void clearBody() throws JMSException {
		this.text = null;
		super.clearBody();
	}

	@Override
	public void setText(String string) throws JMSException {
		this.body = text;
		this.text = text;
	}

	@Override
	public String getText() throws JMSException {
		return this.text;
	}
}
