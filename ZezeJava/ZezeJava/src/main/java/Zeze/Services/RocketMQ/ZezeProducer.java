package Zeze.Services.RocketMQ;

import java.io.UnsupportedEncodingException;
import javax.jms.CompletionListener;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.common.RemotingHelper;
import org.apache.rocketmq.remoting.exception.RemotingException;
import static java.lang.String.format;
import static javax.jms.Message.DEFAULT_DELIVERY_MODE;
import static javax.jms.Message.DEFAULT_PRIORITY;
import static javax.jms.Message.DEFAULT_TIME_TO_LIVE;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

public class ZezeProducer implements MessageProducer {

	private org.apache.rocketmq.client.producer.DefaultMQProducer mqProducer;
	private javax.jms.Destination destination;
	private boolean isStart = false;

	public ZezeProducer(String name, String nameserverAddress) {
		this.mqProducer = new DefaultMQProducer(name);
		this.mqProducer.setNamesrvAddr(nameserverAddress);
	}

	private org.apache.rocketmq.common.message.Message createRmqMessage(javax.jms.Message message, String topicName) throws JMSException {
		ZezeMessage jmsMsg = (ZezeMessage)message;
		initJMSHeaders(jmsMsg, destination);

		org.apache.rocketmq.common.message.Message rmqMsg = null;

		try {
			rmqMsg = Util.Converter.jmsMessage2RmqMessage(jmsMsg);
		} catch (Exception e) {
			throw new JMSException(format("Fail to convert to RocketMQ message. Error: %s", getStackTrace(e)));
		}

		return rmqMsg;
	}

	public void start() {
		try {
			this.mqProducer.start();
			isStart = true;
		} catch (MQClientException e) {
			throw new RuntimeException(e);
		}
	}

	public void shutdown() {
		try {
			this.mqProducer.shutdown();
			isStart = false;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void initJMSHeaders(ZezeMessage jmsMsg, Destination destination) throws JMSException {
		//JMS_DESTINATION default:"topic:message"
		jmsMsg.setHeader(Constant.JMS_DESTINATION, destination);
		//JMS_DELIVERY_MODE default : PERSISTENT
		jmsMsg.setHeader(Constant.JMS_DELIVERY_MODE, javax.jms.Message.DEFAULT_DELIVERY_MODE);
		//JMS_TIMESTAMP default : current time
		jmsMsg.setHeader(Constant.JMS_TIMESTAMP, System.currentTimeMillis());
		//JMS_EXPIRATION default :  3 days
		//JMS_EXPIRATION = currentTime + time_to_live
		jmsMsg.setHeader(Constant.JMS_EXPIRATION, System.currentTimeMillis() + DEFAULT_TIME_TO_LIVE);
		//JMS_PRIORITY default : 4
		jmsMsg.setHeader(Constant.JMS_PRIORITY, javax.jms.Message.DEFAULT_PRIORITY);
		//JMS_TYPE default : ons(open notification service)
		jmsMsg.setHeader(Constant.JMS_TYPE, Constant.DEFAULT_JMS_TYPE);
		//JMS_REPLY_TO,JMS_CORRELATION_ID default : null
		//JMS_MESSAGE_ID is set by sendResult.
		//JMS_REDELIVERED is set by broker.
	}

	private void sendSync(org.apache.rocketmq.common.message.Message rmqMsg) throws JMSException {
		org.apache.rocketmq.client.producer.SendResult sendResult;
		try {
			sendResult = mqProducer.send(rmqMsg);
		} catch (Exception e) {
			throw new JMSException(format("Fail to send message. Error: %s", getStackTrace(e)));
		}

		if (sendResult != null && sendResult.getSendStatus() == org.apache.rocketmq.client.producer.SendStatus.SEND_OK) {
			System.out.println("Success to send message[key={ " + rmqMsg.getKeys() + " }]");
		} else {
			assert sendResult != null;
			throw new JMSException(format("Sending message error with result status:%s", sendResult.getSendStatus().name()));
		}
	}

	private void sendSync(org.apache.rocketmq.common.message.Message rmqMsg, CompletionListener completionListener) throws JMSException {
		try {
			mqProducer.send(rmqMsg, new ZezeSendCompletionListener(completionListener));
		} catch (Exception e) {
			throw new JMSException(format("Fail to send message. Error: %s", getStackTrace(e)));
		}
	}

	@Override
	public void setDisableMessageID(boolean value) throws JMSException {

	}

	@Override
	public boolean getDisableMessageID() throws JMSException {
		return false;
	}

	@Override
	public void setDisableMessageTimestamp(boolean value) throws JMSException {

	}

	@Override
	public boolean getDisableMessageTimestamp() throws JMSException {
		return false;
	}

	@Override
	public void setDeliveryMode(int deliveryMode) throws JMSException {

	}

	@Override
	public int getDeliveryMode() throws JMSException {
		return 0;
	}

	@Override
	public void setPriority(int defaultPriority) throws JMSException {

	}

	@Override
	public int getPriority() throws JMSException {
		return 0;
	}

	@Override
	public void setTimeToLive(long timeToLive) throws JMSException {

	}

	@Override
	public long getTimeToLive() throws JMSException {
		return 0;
	}

	@Override
	public void setDeliveryDelay(long deliveryDelay) throws JMSException {

	}

	@Override
	public long getDeliveryDelay() throws JMSException {
		return 0;
	}

	@Override
	public Destination getDestination() throws JMSException {
		return null;
	}

	@Override
	public void close() throws JMSException {

	}

	@Override
	public void send(javax.jms.Message message) throws JMSException {
		this.send(this.destination, message);
	}

	@Override
	public void send(javax.jms.Message message, int deliveryMode, int priority, long timeToLive) throws JMSException {
		message.setJMSDestination(destination);
//		org.apache.rocketmq.common.message.Message msg = (MessageBase)message;
	}

	@Override
	public void send(Destination destination, javax.jms.Message message) throws JMSException {
		this.send(destination, message, javax.jms.Message.DEFAULT_DELIVERY_MODE, javax.jms.Message.DEFAULT_PRIORITY, javax.jms.Message.DEFAULT_TIME_TO_LIVE);
	}

	@Override
	public void send(Destination destination, javax.jms.Message message, int deliveryMode, int priority, long timeToLive) throws JMSException {
		String topicName = Util.getTopicName(destination);
		org.apache.rocketmq.common.message.Message rmqMessage = createRmqMessage(message, topicName);

		sendSync(rmqMessage);
	}

	@Override
	public void send(javax.jms.Message message, CompletionListener completionListener) throws JMSException {
		this.send(this.destination, message, DEFAULT_DELIVERY_MODE, DEFAULT_PRIORITY, DEFAULT_TIME_TO_LIVE, completionListener);
	}

	@Override
	public void send(javax.jms.Message message, int deliveryMode, int priority, long timeToLive, CompletionListener completionListener) throws JMSException {
		this.send(this.destination, message, DEFAULT_DELIVERY_MODE, DEFAULT_PRIORITY, DEFAULT_TIME_TO_LIVE, completionListener);
	}

	@Override
	public void send(Destination destination, javax.jms.Message message, CompletionListener completionListener) throws JMSException {
		this.send(destination, message, DEFAULT_DELIVERY_MODE, DEFAULT_PRIORITY, DEFAULT_TIME_TO_LIVE, completionListener);
	}

	@Override
	public void send(Destination destination, javax.jms.Message message, int deliveryMode, int priority, long timeToLive, CompletionListener completionListener) throws JMSException {
		String topicName = Util.getTopicName(destination);
		org.apache.rocketmq.common.message.Message rmqMessage = createRmqMessage(message, topicName);

		sendSync(rmqMessage, completionListener);
	}

	public static void main(String[] args) throws MQClientException, MQBrokerException, RemotingException, InterruptedException, UnsupportedEncodingException {
		DefaultMQProducer producer = new DefaultMQProducer("PRODUCER");
		producer.setNamesrvAddr("localhost:9876");

		producer.start();

		//Create a message instance, specifying topic, tag and message body.
		Message msg = new Message("TopicTest" /* Topic */,
				"TagA" /* Tag */,
				("Hello RocketMQ " +
						1).getBytes(RemotingHelper.DEFAULT_CHARSET) /* Message body */
		);
		//Call send message to deliver message to one of brokers.
		SendResult sendResult = producer.send(msg);
		System.out.printf("%s%n", sendResult);
		//Shut down once the producer instance is not longer in use.
		producer.shutdown();
	}
}
