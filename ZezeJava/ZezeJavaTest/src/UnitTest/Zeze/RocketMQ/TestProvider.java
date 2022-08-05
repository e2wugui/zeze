package UnitTest.Zeze.RocketMQ;

import java.io.UnsupportedEncodingException;
import javax.jms.JMSException;
import Zeze.Services.RocketMQ.Connection;
import Zeze.Services.RocketMQ.ConnectionFactory;
import Zeze.Services.RocketMQ.Session;
import Zeze.Services.RocketMQ.Topic;
import Zeze.Services.RocketMQ.msg.TextMessage;
import Zeze.Services.RocketMQ.producer.MessageProducer;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.remoting.exception.RemotingException;

public class TestProvider {
	public static void main(String[] args) throws JMSException, MQClientException, MQBrokerException, RemotingException, InterruptedException, UnsupportedEncodingException {
		ConnectionFactory factory = new ConnectionFactory("127.0.0.1:9876");
		Connection connection = (Connection)factory.createConnection();
		Session session = (Session)connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

		MessageProducer producer = (MessageProducer)session.createProducer(new Topic("TopicTest"));
		producer.start();
		producer.send(new TextMessage("Hello, World!"));
		producer.shutdown();
	}
}
