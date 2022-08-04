package Zeze.Services.RocketMQ;

import javax.jms.JMSException;

public class ZezeTopic implements javax.jms.Topic {

	private String name;

	public ZezeTopic(String name) {
		this.name = name;
	}

	@Override
	public String getTopicName() throws JMSException {
		return name;
	}
}
