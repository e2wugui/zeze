package Zeze.Services.RocketMQ;

import java.io.Serializable;
import javax.jms.JMSException;

public class ZezeObjectMessage extends ZezeMessage implements javax.jms.ObjectMessage {

	public ZezeObjectMessage() {}

	public ZezeObjectMessage(Serializable object) {
		this.body = object;
	}

	@Override
	public void setObject(Serializable object) throws JMSException {
		this.body = object;
	}

	@Override
	public Serializable getObject() throws JMSException {
		return this.body;
	}
}
