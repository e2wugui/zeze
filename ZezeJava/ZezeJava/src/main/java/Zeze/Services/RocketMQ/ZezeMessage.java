package Zeze.Services.RocketMQ;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Map;
import javax.jms.Destination;
import javax.jms.JMSException;
import org.tikv.shade.com.google.common.collect.Maps;
import org.tikv.shade.com.google.common.io.BaseEncoding;

public class ZezeMessage implements javax.jms.Message {

	protected Map<String, Object> properties = Maps.newHashMap();
	protected Map<String, Object> headers = Maps.newHashMap();
	protected Serializable body;

	protected boolean writeOnly;

//	private org.apache.rocketmq.common.message.Message message;
//
//	public Message(String topic, byte[] body) {
//		this.message = new org.apache.rocketmq.common.message.Message(topic, body);
//	}
//
//	public Message(String topic, String tags, byte[] body) {
//		this.message = new org.apache.rocketmq.common.message.Message(topic, tags, body);
//	}
//
//	public Message(String topic, String tags, byte[] body, String key) {
//		this.message = new org.apache.rocketmq.common.message.Message(topic, tags, body);
//		this.message.setKeys(key);
//	}

	public Map<String, Object> getHeaders() {
		return this.headers;
	}

	public void setHeader(String name, Object value) {
		this.headers.put(name, value);
	}

	public Map<String, Object> getProperties() {
		return this.properties;
	}

	@Override
	public String getJMSMessageID() throws JMSException {
		return Zeze.Services.RocketMQ.TypeConverter.convert2String(headers.get(Constant.JMS_MESSAGE_ID));
	}

	@Override
	public void setJMSMessageID(String id) throws JMSException {
		throw new UnsupportedOperationException("Operation unsupported!");
	}

	@Override
	public long getJMSTimestamp() throws JMSException {
		if (headers.containsKey(Constant.JMS_TIMESTAMP))
			return Zeze.Services.RocketMQ.TypeConverter.convert2Long(headers.get(Constant.JMS_TIMESTAMP));
		return 0;
	}

	@Override
	public void setJMSTimestamp(long timestamp) throws JMSException {
		throw new UnsupportedOperationException("Operation unsupported!");
	}

	@Override
	public byte[] getJMSCorrelationIDAsBytes() throws JMSException {
		String jmsCorrelationID = getJMSCorrelationID();
		if (jmsCorrelationID != null) {
			try {
				return BaseEncoding.base64().decode(jmsCorrelationID);
			} catch (Exception e) {
				return jmsCorrelationID.getBytes();
			}
		}
		return null;
	}

	@Override
	public void setJMSCorrelationIDAsBytes(byte[] correlationID) throws JMSException {
		String encodedText = BaseEncoding.base64().encode(correlationID);
		setJMSCorrelationID(encodedText);
	}

	@Override
	public void setJMSCorrelationID(String correlationID) throws JMSException {
		throw new UnsupportedOperationException("Operation unsupported!");
	}

	@Override
	public String getJMSCorrelationID() throws JMSException {
		if (headers.containsKey(Constant.JMS_CORRELATION_ID)) {
			return TypeConverter.convert2String(headers.get(Constant.JMS_CORRELATION_ID));
		}
		return null;
	}

	@Override
	public Destination getJMSReplyTo() throws JMSException {
		if (headers.containsKey(Constant.JMS_REPLY_TO)) {
			return TypeConverter.convert2Object(headers.get(Constant.JMS_REPLY_TO), Destination.class);
		}
		return null;
	}

	@Override
	public void setJMSReplyTo(Destination replyTo) throws JMSException {
		throw new UnsupportedOperationException("Operation unsupported!");
	}

	@Override
	public Destination getJMSDestination() throws JMSException {
		if (headers.containsKey(Constant.JMS_DESTINATION)) {
			return TypeConverter.convert2Object(headers.get(Constant.JMS_DESTINATION), Destination.class);
		}
		return null;
	}

	@Override
	public void setJMSDestination(Destination destination) throws JMSException {
		throw new UnsupportedOperationException("Operation unsupported!");
	}

	@Override
	public int getJMSDeliveryMode() throws JMSException {
		if (headers.containsKey(Constant.JMS_DELIVERY_MODE)) {
			return TypeConverter.convert2Integer(headers.get(Constant.JMS_DELIVERY_MODE));
		}
		return 0;
	}

	@Override
	public void setJMSDeliveryMode(int deliveryMode) throws JMSException {
		throw new UnsupportedOperationException("Operation unsupported!");
	}

	@Override
	public boolean getJMSRedelivered() throws JMSException {
		return headers.containsKey(Constant.JMS_REDELIVERED) && TypeConverter.convert2Boolean(headers.get(Constant.JMS_REDELIVERED));
	}

	@Override
	public void setJMSRedelivered(boolean redelivered) throws JMSException {
		throw new UnsupportedOperationException("Operation unsupported!");
	}

	@Override
	public String getJMSType() throws JMSException {
		return TypeConverter.convert2String(headers.get(Constant.JMS_TYPE));
	}

	@Override
	public void setJMSType(String type) throws JMSException {
		throw new UnsupportedOperationException("Operation unsupported!");
	}

	@Override
	public long getJMSExpiration() throws JMSException {
		if (headers.containsKey(Constant.JMS_EXPIRATION)) {
			return TypeConverter.convert2Long(headers.get(Constant.JMS_EXPIRATION));
		}
		return 0;
	}

	@Override
	public void setJMSExpiration(long expiration) throws JMSException {
		throw new UnsupportedOperationException("Operation unsupported!");
	}

	@Override
	public long getJMSDeliveryTime() throws JMSException {
		// TODO:
		return 0;
	}

	@Override
	public void setJMSDeliveryTime(long deliveryTime) throws JMSException {
		// TODO:
	}

	@Override
	public int getJMSPriority() throws JMSException {
		if (headers.containsKey(Constant.JMS_PRIORITY)) {
			return TypeConverter.convert2Integer(headers.get(Constant.JMS_PRIORITY));
		}
		return 5;
	}

	@Override
	public void setJMSPriority(int priority) throws JMSException {
		throw new UnsupportedOperationException("Operation unsupported!");
	}

	@Override
	public void clearProperties() throws JMSException {
		this.properties.clear();
	}

	@Override
	public boolean propertyExists(String name) throws JMSException {
		return properties.containsKey(name);
	}

	@Override
	public boolean getBooleanProperty(String name) throws JMSException {
		if (propertyExists(name)) {
			Object value = getObjectProperty(name);
			return Boolean.parseBoolean(value.toString());
		}
		return false;
	}

	@Override
	public byte getByteProperty(String name) throws JMSException {
		if (propertyExists(name)) {
			Object value = getObjectProperty(name);
			return Byte.parseByte(value.toString());
		}
		return 0;
	}

	@Override
	public short getShortProperty(String name) throws JMSException {
		if (propertyExists(name)) {
			Object value = getObjectProperty(name);
			return Short.parseShort(value.toString());
		}
		return 0;
	}

	@Override
	public int getIntProperty(String name) throws JMSException {
		if (propertyExists(name)) {
			Object value = getObjectProperty(name);
			return Integer.parseInt(value.toString());
		}
		return 0;
	}

	@Override
	public long getLongProperty(String name) throws JMSException {
		if (propertyExists(name)) {
			Object value = getObjectProperty(name);
			return Long.parseLong(value.toString());
		}
		return 0L;
	}

	@Override
	public float getFloatProperty(String name) throws JMSException {
		if (propertyExists(name)) {
			Object value = getObjectProperty(name);
			return Float.parseFloat(value.toString());
		}
		return 0f;
	}

	@Override
	public double getDoubleProperty(String name) throws JMSException {
		if (propertyExists(name)) {
			Object value = getObjectProperty(name);
			return Double.parseDouble(value.toString());
		}
		return 0d;
	}

	@Override
	public String getStringProperty(String name) throws JMSException {
		if (propertyExists(name)) {
			return getObjectProperty(name).toString();
		}
		return null;
	}

	@Override
	public Object getObjectProperty(String name) throws JMSException {
		return this.properties.get(name);
	}

	@Override
	public Enumeration<?> getPropertyNames() throws JMSException {
		final Object[] keys = this.properties.keySet().toArray();
		return new Enumeration<Object>() {
			int i;

			@Override
			public boolean hasMoreElements() {
				return i < keys.length;
			}

			@Override
			public Object nextElement() {
				return keys[i++];
			}
		};
	}

	@Override
	public void setBooleanProperty(String name, boolean value) throws JMSException {
		setObjectProperty(name, value);
	}

	@Override
	public void setByteProperty(String name, byte value) throws JMSException {
		setObjectProperty(name, value);
	}

	@Override
	public void setShortProperty(String name, short value) throws JMSException {
		setObjectProperty(name, value);
	}

	@Override
	public void setIntProperty(String name, int value) throws JMSException {
		setObjectProperty(name, value);
	}

	@Override
	public void setLongProperty(String name, long value) throws JMSException {
		setObjectProperty(name, value);
	}

	@Override
	public void setFloatProperty(String name, float value) throws JMSException {
		setObjectProperty(name, value);
	}

	@Override
	public void setDoubleProperty(String name, double value) throws JMSException {
		setObjectProperty(name, value);
	}

	@Override
	public void setStringProperty(String name, String value) throws JMSException {
		setObjectProperty(name, value);
	}

	@Override
	public void setObjectProperty(String name, Object value) throws JMSException {
		if (value instanceof Number || value instanceof String || value instanceof Boolean) {
			this.properties.put(name, value);
		} else {
			throw new IllegalArgumentException(
					"Value should be boolean, byte, short, int, long, float, double, and String.");
		}
	}

	@Override
	public void acknowledge() throws JMSException {
		throw new UnsupportedOperationException("Operation unsupported!");
	}

	@Override
	public void clearBody() throws JMSException {
		this.body = null;
		this.writeOnly = true;
	}

	@Override
	public <T> T getBody(Class<T> c) throws JMSException {
		if (c.isInstance(body))
			return TypeConverter.convert2Object(body, c);
		throw new IllegalArgumentException("The class " + c + " is unknown to this implementation");
	}

	@Override
	public boolean isBodyAssignableTo(Class c) throws JMSException {
		return c.isInstance(body);
	}
}
