package Zeze.Services.RocketMQ;

import javax.jms.JMSException;
import javax.jms.Topic;

public class ZezeTopic implements Topic {

	private String name;
	private String type;

	@Override
	public String getTopicName() throws JMSException {
		return this.name;
	}

	public String getTypeName() throws JMSException {
		return this.type;
	}
}
