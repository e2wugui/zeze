package Zeze.Services.RocketMQ;

import java.io.Serializable;
import javax.jms.JMSException;
@Deprecated // Do not use Object Message yet
public class ObjectMessage extends Message implements javax.jms.ObjectMessage {
	@Override
	public void setObject(Serializable object) throws JMSException {

	}

	@Override
	public Serializable getObject() throws JMSException {
		return null;
	}
}
