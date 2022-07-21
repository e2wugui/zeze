package Zeze.Services.RocketMQ;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.JMSRuntimeException;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.TextMessage;
import javax.jms.Topic;
import org.apache.commons.codec.Charsets;
import org.apache.rocketmq.common.message.MessageConst;
import static org.tikv.shade.com.google.common.base.Preconditions.checkState;

public class Util {

	public static final String JMS_MSGMODEL = "jmsMsgModel";
	public static final String MSGMODEL_TEXT = "textMessage";
	public static final String MSGMODEL_BYTES = "bytesMessage";
	public static final String MSGMODEL_OBJ = "objectMessage";
	public static final String MSG_TOPIC = "msgTopic";
	public static final String MSG_TYPE = "msgType";

	public static String getTopicName(Destination destination) {
		try {
			String topicName;
			if (destination instanceof Topic) {
				topicName = ((Topic)destination).getTopicName();
			} else if (destination instanceof Queue) {
				topicName = ((Queue)destination).getQueueName();
			} else {
				throw new JMSException(String.format("Unsupported Destination type:" + destination.getClass()));
			}
			return topicName;
		} catch (JMSException e) {
			throw new JMSRuntimeException(e.getMessage());
		}
	}

	public static class Converter {
		private static final AtomicLong counter = new AtomicLong(1L);

		public static org.apache.rocketmq.common.message.Message jmsMessage2RmqMessage(ZezeMessage message) throws JMSException, IOException {
			org.apache.rocketmq.common.message.Message rmqMessage = new org.apache.rocketmq.common.message.MessageExt();
			rmqMessage.setKeys(System.currentTimeMillis() + "" + counter.incrementAndGet());

			// 1. Transform message body
			rmqMessage.setBody(getBytesFromJmsMessage(message));

			// 2. Transform topic and messageType
			String topic, tag;
			javax.jms.Destination destination = (Destination)message.getHeaders().get(Constant.JMS_DESTINATION);

			if (destination instanceof javax.jms.Topic) {
				topic = ((ZezeTopic)destination).getTopicName();
				tag = ((ZezeTopic)destination).getTypeName();
			} else {
				topic = ((ZezeQueue)destination).getQueueName();
				tag = Constant.NO_MESSAGE_SELECTOR;
			}
			checkState(!tag.contains("||"), "'||' can not be in the destination when sending a message");
			rmqMessage.setTopic(topic);
			rmqMessage.setTags(tag);

			// 3. Transform message properties
			Properties properties = getAllProperties(message, topic, tag);
			for (String name : properties.stringPropertyNames()) {
				String value = properties.getProperty(name);
				if (MessageConst.PROPERTY_KEYS.equals(name)) {
					rmqMessage.setKeys(value);
				} else if (MessageConst.PROPERTY_TAGS.equals(name)) {
					rmqMessage.setTags(value);
				} else if (MessageConst.PROPERTY_DELAY_TIME_LEVEL.equals(name)) {
					rmqMessage.setDelayTimeLevel(Integer.parseInt(value));
				} else if (MessageConst.PROPERTY_WAIT_STORE_MSG_OK.equals(name)) {
					rmqMessage.setWaitStoreMsgOK(Boolean.parseBoolean(value));
				} else if (MessageConst.PROPERTY_BUYER_ID.equals(name)) {
					rmqMessage.setBuyerId(value);
				} else {
					rmqMessage.putUserProperty(name, value);
				}
			}

			return rmqMessage;
		}

		public static byte[] getBytesFromJmsMessage(javax.jms.Message message) throws JMSException, IOException {
			if (message == null)
				return null;

			byte[] bytes = null;

			if (message instanceof javax.jms.TextMessage) {
				if (org.apache.commons.lang3.StringUtils.isEmpty(((javax.jms.TextMessage)message).getText())) {
					throw new IllegalArgumentException("Message body length is zero");
				}
				bytes = string2Bytes(((TextMessage)message).getText(), Charsets.UTF_8.toString());
			} else if (message instanceof javax.jms.ObjectMessage) {
				if (((ObjectMessage)message).getObject() == null) {
					throw new IllegalArgumentException("Message body length is zero");
				}
				bytes = objectSerialize(((ObjectMessage)message).getObject());
			} else if (message instanceof javax.jms.BytesMessage) {
				bytes = ((ZezeBytesMessage)message).getData();
			} else {
				throw new IllegalArgumentException("Unknown message type " + message.getJMSType());
			}
			return bytes;
		}

		public static byte[] string2Bytes(String str, String charset) throws UnsupportedEncodingException {
			if (str == null)
				return new byte[0];
			byte[] bs = null;
			bs = str.getBytes(charset);
			return bs;
		}

		public static byte[] objectSerialize(Object obj) throws IOException {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(obj);
			oos.close();
			baos.close();
			return baos.toByteArray();
		}

		private static Properties getAllProperties(ZezeMessage message, String topic, String tag) throws JMSException {
			Properties userProperties = new Properties();

			Map<String, Object> userProps = message.getProperties();
			for (Map.Entry<String, Object> entry : userProps.entrySet()) {
				userProperties.setProperty(entry.getKey(), entry.getValue().toString());
			}

			Map<String, Object> sysProps = message.getHeaders();
			for (Map.Entry<String, Object> entry : sysProps.entrySet()) {
				userProperties.setProperty(entry.getKey(), entry.getValue().toString());
			}

			if (message instanceof ZezeBytesMessage) {
				userProperties.setProperty(JMS_MSGMODEL, MSGMODEL_BYTES);
			} else if (message instanceof ZezeObjectMessage) {
				userProperties.setProperty(JMS_MSGMODEL, MSGMODEL_OBJ);
			} else if (message instanceof ZezeTextMessage) {
				userProperties.setProperty(JMS_MSGMODEL, MSGMODEL_TEXT);
			}

			userProperties.setProperty(MSG_TOPIC, topic);
			userProperties.setProperty(MSG_TYPE, tag);

			return userProperties;
		}
	}
}
