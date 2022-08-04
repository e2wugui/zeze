package Zeze.Services.RocketMQ;

import java.io.Serializable;
import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.StreamMessage;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicSubscriber;
import Zeze.Services.RocketMQ.consumer.ZezeMessageConsumer;
import Zeze.Services.RocketMQ.msg.ZezeMessage;
import Zeze.Services.RocketMQ.msg.ZezeTextMessage;
import Zeze.Services.RocketMQ.producer.ZezeMessageProducer;
import Zeze.Services.RocketMQ.producer.ZezeTransactionProducer;

public class ZezeSession implements javax.jms.Session {
	private ZezeConnection connection;
	private int acknowledgeMode;
	private boolean transacted;

	protected boolean closed;
	private MessageListener messageListener;
	private int nextProducerId = 0;

	public ZezeSession(ZezeConnection connection, int acknowledgeMode, boolean transacted) {
		this.connection = connection;
		this.acknowledgeMode = acknowledgeMode;
		this.transacted = transacted;
	}

	@Override
	public BytesMessage createBytesMessage() throws JMSException {
		return null;
	}

	@Override
	public MapMessage createMapMessage() throws JMSException {
		return null;
	}

	@Override
	public Message createMessage() throws JMSException {
		return new ZezeMessage();
	}

	@Override
	public ObjectMessage createObjectMessage() throws JMSException {
		return null;
	}

	@Override
	public ObjectMessage createObjectMessage(Serializable object) throws JMSException {
		return null;
	}

	@Override
	public StreamMessage createStreamMessage() throws JMSException {
		return null;
	}

	@Override
	public TextMessage createTextMessage() throws JMSException {
		return new ZezeTextMessage();
	}

	@Override
	public TextMessage createTextMessage(String text) throws JMSException {
		return new ZezeTextMessage(text);
	}

	@Override
	public boolean getTransacted() throws JMSException {
		return transacted;
	}

	@Override
	public int getAcknowledgeMode() throws JMSException {
		return acknowledgeMode;
	}

	@Override
	public void commit() throws JMSException {

	}

	@Override
	public void rollback() throws JMSException {

	}

	@Override
	public void close() throws JMSException {

	}

	@Override
	public void recover() throws JMSException {

	}

	@Override
	public MessageListener getMessageListener() throws JMSException {
		return messageListener;
	}

	@Override
	public void setMessageListener(MessageListener listener) throws JMSException {
		this.messageListener = listener;
	}

	@Override
	public void run() {

	}

	@Override
	public MessageProducer createProducer(Destination destination) throws JMSException {
		return new ZezeMessageProducer(this, nextProducerId++, destination, connection.getSendTimeout());
	}

	public MessageProducer createTransactionProducer(Destination destination) throws JMSException {
		return new ZezeTransactionProducer(this, nextProducerId++, destination, connection.getSendTimeout());
	}

	@Override
	public MessageConsumer createConsumer(Destination destination) throws JMSException {
		return createConsumer(destination, (String)null);
	}

	@Override
	public MessageConsumer createConsumer(Destination destination, String messageSelector) throws JMSException {
		return createConsumer(destination, messageSelector, false);
	}

	@Override
	public MessageConsumer createConsumer(Destination destination, String messageSelector, boolean noLocal) throws JMSException {
		// TODO: what does noLocal mean?
		return new ZezeMessageConsumer(this, destination, 1, messageSelector);
	}

	@Override
	public MessageConsumer createSharedConsumer(Topic topic, String sharedSubscriptionName) throws JMSException {
		return null;
	}

	@Override
	public MessageConsumer createSharedConsumer(Topic topic, String sharedSubscriptionName, String messageSelector) throws JMSException {
		return null;
	}

	@Override
	public Queue createQueue(String queueName) throws JMSException {
		return null;
	}

	@Override
	public Topic createTopic(String topicName) throws JMSException {
		return new ZezeTopic(topicName);
	}

	@Override
	public TopicSubscriber createDurableSubscriber(Topic topic, String name) throws JMSException {
		return null;
	}

	@Override
	public TopicSubscriber createDurableSubscriber(Topic topic, String name, String messageSelector, boolean noLocal) throws JMSException {
		return null;
	}

	@Override
	public MessageConsumer createDurableConsumer(Topic topic, String name) throws JMSException {
		return null;
	}

	@Override
	public MessageConsumer createDurableConsumer(Topic topic, String name, String messageSelector, boolean noLocal) throws JMSException {
		return null;
	}

	@Override
	public MessageConsumer createSharedDurableConsumer(Topic topic, String name) throws JMSException {
		return null;
	}

	@Override
	public MessageConsumer createSharedDurableConsumer(Topic topic, String name, String messageSelector) throws JMSException {
		return null;
	}

	@Override
	public QueueBrowser createBrowser(Queue queue) throws JMSException {
		return null;
	}

	@Override
	public QueueBrowser createBrowser(Queue queue, String messageSelector) throws JMSException {
		return null;
	}

	@Override
	public TemporaryQueue createTemporaryQueue() throws JMSException {
		return null;
	}

	@Override
	public TemporaryTopic createTemporaryTopic() throws JMSException {
		return null;
	}

	@Override
	public void unsubscribe(String name) throws JMSException {

	}

	public ZezeConnection getConnection() {
		return connection;
	}
}
