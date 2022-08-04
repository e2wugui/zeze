package Zeze.Services.RocketMQ.examples;

import java.util.Arrays;
import java.util.List;
import javax.jms.JMSException;
import Zeze.Services.RocketMQ.ZezeConnection;
import Zeze.Services.RocketMQ.ZezeConnectionFactory;
import Zeze.Services.RocketMQ.ZezeSession;
import Zeze.Services.RocketMQ.ZezeTopic;
import Zeze.Services.RocketMQ.consumer.ZezeMessageConsumer;
import Zeze.Services.RocketMQ.consumer.ZezeMessageListenerConcurrently;
import Zeze.Services.RocketMQ.msg.ZezeMessage;
import org.apache.rocketmq.client.exception.MQClientException;

public class TestConsumer {
	public static void main(String[] args) throws JMSException, MQClientException {
		ZezeConnectionFactory factory = new ZezeConnectionFactory("127.0.0.1:9876");
		ZezeConnection connection = (ZezeConnection)factory.createConnection();
		ZezeSession session = (ZezeSession)connection.createSession(false, ZezeSession.AUTO_ACKNOWLEDGE);

		ZezeTopic topic = new ZezeTopic("TopicTest");
		ZezeMessageConsumer consumer = (ZezeMessageConsumer)session.createConsumer(topic);
		consumer.setMessageListener(new ZezeMessageListenerConcurrently() {
			@Override
			public void onMessage(ZezeMessage message) {
				System.out.println("Received: " + new String(message.getBody()));
			}
		});
		consumer.start();
	}
}
