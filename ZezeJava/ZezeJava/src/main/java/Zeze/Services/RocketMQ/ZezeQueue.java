package Zeze.Services.RocketMQ;

import javax.jms.JMSException;
import javax.jms.Queue;

public class ZezeQueue implements Queue {

	private String name;

	public ZezeQueue(String name) {
		this.name = name;
	}

	@Override
	public String getQueueName() throws JMSException {
		return this.name;
	}
}
