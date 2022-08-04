package Zeze.Services.RocketMQ.examples;

import javax.jms.JMSException;
import Zeze.Services.RocketMQ.ZezeConnection;
import Zeze.Services.RocketMQ.ZezeConnectionFactory;
import Zeze.Services.RocketMQ.ZezeSession;
import Zeze.Services.RocketMQ.ZezeTopic;
import Zeze.Services.RocketMQ.msg.ZezeMessage;
import Zeze.Services.RocketMQ.producer.ZezeTransactionListener;
import Zeze.Services.RocketMQ.producer.ZezeTransactionProducer;

public class TestTransactionProvider {
	public static void main(String[] args) throws JMSException {
		ZezeConnectionFactory factory = new ZezeConnectionFactory("127.0.0.1:9876");
		ZezeConnection connection = (ZezeConnection)factory.createConnection();
		ZezeSession session = (ZezeSession)connection.createSession(false, ZezeSession.AUTO_ACKNOWLEDGE);

		ZezeTransactionProducer producer = (ZezeTransactionProducer)session.createTransactionProducer(new ZezeTopic("TopicTest"));
		producer.setTransactionListener(new ZezeTransactionListener() {
			@Override
			public void sendHalfMessage(ZezeMessage message, int deliveryMode, int priority, long timeToLive) {

			}

			@Override
			public void sendCompleteMessage(ZezeMessage message, int deliveryMode, int priority, long timeToLive) {

			}
		});
		producer.start();
	}
}
