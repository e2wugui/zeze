package Zeze.Services.RocketMQ.examples;

import java.io.UnsupportedEncodingException;
import javax.jms.JMSException;
import Zeze.Services.RocketMQ.ZezeConnection;
import Zeze.Services.RocketMQ.ZezeConnectionFactory;
import Zeze.Services.RocketMQ.ZezeSession;
import Zeze.Services.RocketMQ.ZezeTopic;
import Zeze.Services.RocketMQ.msg.ZezeTextMessage;
import Zeze.Services.RocketMQ.producer.ZezeMessageProducer;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.common.RemotingHelper;
import org.apache.rocketmq.remoting.exception.RemotingException;

public class TestProvider {
	public static void main(String[] args) throws JMSException, MQClientException, MQBrokerException, RemotingException, InterruptedException, UnsupportedEncodingException {
		ZezeConnectionFactory factory = new ZezeConnectionFactory("127.0.0.1:9876");
		ZezeConnection connection = (ZezeConnection)factory.createConnection();
		ZezeSession session = (ZezeSession)connection.createSession(false, ZezeSession.AUTO_ACKNOWLEDGE);

		ZezeMessageProducer producer = (ZezeMessageProducer)session.createProducer(new ZezeTopic("TopicTest"));
		producer.start();
		producer.send(new ZezeTextMessage("Hello, World!"));
		producer.shutdown();
	}
}
