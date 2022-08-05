package Zeze.Services.RocketMQ;

import javax.jms.JMSException;

public class Topic implements javax.jms.Topic {

	private String name;

	public Topic(String name) {
		this.name = name;
	}

	@Override
	public String getTopicName() throws JMSException {
		return name;
	}
}
