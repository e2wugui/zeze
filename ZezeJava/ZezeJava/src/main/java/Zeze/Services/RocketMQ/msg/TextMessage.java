package Zeze.Services.RocketMQ.msg;

import javax.jms.JMSException;

public class TextMessage extends Message implements javax.jms.TextMessage {

	protected String text;

	public TextMessage() {
	}

	public TextMessage(String text) {
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
