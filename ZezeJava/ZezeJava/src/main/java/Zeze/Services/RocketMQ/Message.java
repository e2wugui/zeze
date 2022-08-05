package Zeze.Services.RocketMQ;

import java.util.Enumeration;
import java.util.Map;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;

public class Message implements javax.jms.Message {

	private String messageID; // TODO: use unique message ID
	private long timestamp;
	private Destination destination;
	private Destination replyTo;
	private boolean persistent;
	private int redeliveryCounter;
	private String type;
	private long expiration;
	private byte priority;
	private Map<String, Object> properties = new java.util.HashMap<String, Object>();
	//	protected transient Callback acknowledgeCallback;// may would be used?
	private byte[] body;

	org.apache.rocketmq.common.message.MessageExt message;

	public Message() {
	}

	public Message(org.apache.rocketmq.common.message.MessageExt message) {
		this.message = message;
		body = message.getBody();
	}

	@Override
	public String getJMSMessageID() throws JMSException {
		return messageID;
	}

	@Override
	public void setJMSMessageID(String id) throws JMSException {
		messageID = id;
	}

	@Override
	public long getJMSTimestamp() throws JMSException {
		return timestamp;
	}

	@Override
	public void setJMSTimestamp(long timestamp) throws JMSException {
		this.timestamp = timestamp;
	}

	@Deprecated
	@Override
	public byte[] getJMSCorrelationIDAsBytes() throws JMSException {
		return new byte[0];
	}

	@Deprecated
	@Override
	public void setJMSCorrelationIDAsBytes(byte[] correlationID) throws JMSException {
	}

	@Deprecated
	@Override
	public void setJMSCorrelationID(String correlationID) throws JMSException {
	}

	@Deprecated
	@Override
	public String getJMSCorrelationID() throws JMSException {
		return null;
	}

	@Override
	public Destination getJMSReplyTo() throws JMSException {
		return replyTo;
	}

	@Override
	public void setJMSReplyTo(Destination replyTo) throws JMSException {
		this.replyTo = replyTo;
	}

	@Override
	public Destination getJMSDestination() throws JMSException {
		return destination;
	}

	@Override
	public void setJMSDestination(Destination destination) throws JMSException {
		this.destination = destination;
	}

	@Override
	public int getJMSDeliveryMode() throws JMSException {
		return persistent ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT;
	}

	@Override
	public void setJMSDeliveryMode(int deliveryMode) throws JMSException {
		persistent = deliveryMode == DeliveryMode.PERSISTENT;
	}

	@Override
	public boolean getJMSRedelivered() throws JMSException {
		return redeliveryCounter > 0;
	}

	@Override
	public void setJMSRedelivered(boolean redelivered) throws JMSException {
		redeliveryCounter = redelivered ? 1 : 0;
	}

	@Override
	public String getJMSType() throws JMSException {
		return type;
	}

	@Override
	public void setJMSType(String type) throws JMSException {
		this.type = type;
	}

	@Override
	public long getJMSExpiration() throws JMSException {
		return expiration;
	}

	@Override
	public void setJMSExpiration(long expiration) throws JMSException {
		this.expiration = expiration;
	}

	@Deprecated
	@Override
	public long getJMSDeliveryTime() throws JMSException {
		return 0;
	}

	@Deprecated
	@Override
	public void setJMSDeliveryTime(long deliveryTime) throws JMSException {
	}

	@Override
	public int getJMSPriority() throws JMSException {
		return priority;
	}

	@Override
	public void setJMSPriority(int priority) throws JMSException {
		this.priority = (byte)priority;
	}

	@Override
	public void clearProperties() throws JMSException {
		properties.clear();
	}

	@Override
	public boolean propertyExists(String name) throws JMSException {
		return properties.containsKey(name);
	}

	@Override
	public boolean getBooleanProperty(String name) throws JMSException {
		Object value = properties.get(name);
		if (value instanceof Boolean)
			return (Boolean)value;
		throw new JMSException("Property " + name + " is not a boolean.");
	}

	@Override
	public byte getByteProperty(String name) throws JMSException {
		Object value = properties.get(name);
		if (value instanceof Byte)
			return (Byte)value;
		throw new JMSException("Property " + name + " is not a byte.");
	}

	@Override
	public short getShortProperty(String name) throws JMSException {
		Object value = properties.get(name);
		if (value instanceof Short)
			return (Short)value;
		throw new JMSException("Property " + name + " is not a short.");
	}

	@Override
	public int getIntProperty(String name) throws JMSException {
		Object value = properties.get(name);
		if (value instanceof Integer)
			return (Integer)value;
		throw new JMSException("Property " + name + " is not an integer.");
	}

	@Override
	public long getLongProperty(String name) throws JMSException {
		Object value = properties.get(name);
		if (value instanceof Long)
			return (Long)value;
		throw new JMSException("Property " + name + " is not a long.");
	}

	@Override
	public float getFloatProperty(String name) throws JMSException {
		Object value = properties.get(name);
		if (value instanceof Float)
			return (Float)value;
		throw new JMSException("Property " + name + " is not a float.");
	}

	@Override
	public double getDoubleProperty(String name) throws JMSException {
		Object value = properties.get(name);
		if (value instanceof Double)
			return (Double)value;
		throw new JMSException("Property " + name + " is not a double.");
	}

	@Override
	public String getStringProperty(String name) throws JMSException {
		Object value = properties.get(name);
		if (value instanceof String)
			return (String)value;
		throw new JMSException("Property " + name + " is not a string.");
	}

	@Override
	public Object getObjectProperty(String name) throws JMSException {
		return properties.get(name);
	}

	@Deprecated
	@Override
	public Enumeration getPropertyNames() throws JMSException {
		// TODO: maybe would be used in the future, but not intended to implement now
		return null;
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
		// TODO: maybe to check properties readonly in the future, but not now
		properties.put(name, value);
	}

	@Override
	public void acknowledge() throws JMSException {
		// may would be used?
	}

	@Override
	public void clearBody() throws JMSException {
		body = null;
	}

	@Deprecated
	@Override
	public <T> T getBody(Class<T> c) throws JMSException {
		return null;
	}

	@Deprecated
	@Override
	public boolean isBodyAssignableTo(Class c) throws JMSException {
		return false;
	}

	public byte[] getBody() {
		return body;
	}

	public void setBody(byte[] content) {
		this.body = content;
	}
}
