package Zeze.Services.RocketMQ;

import java.io.Serializable;
import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.StreamMessage;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.TopicSubscriber;

public class Session implements javax.jms.Session {
	private final Connection connection;
	private final int acknowledgeMode;
	private final boolean transacted;

	protected boolean closed;
	private MessageListener messageListener;
	private int nextProducerId = 0;

	public Session(Connection connection, int acknowledgeMode, boolean transacted) {
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
	public javax.jms.Message createMessage() throws JMSException {
		return new Message();
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
	public javax.jms.TextMessage createTextMessage() throws JMSException {
		return new TextMessage();
	}

	@Override
	public javax.jms.TextMessage createTextMessage(String text) throws JMSException {
		return new TextMessage(text);
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
	public javax.jms.MessageProducer createProducer(Destination destination) throws JMSException {
		return new MessageProducer(this, nextProducerId++, destination, connection.getSendTimeout());
	}

	public javax.jms.MessageProducer createTransactionProducer(Destination destination) throws JMSException {
		return new TransactionProducer(this, nextProducerId++, destination, connection.getSendTimeout());
	}

	@Override
	public javax.jms.MessageConsumer createConsumer(Destination destination) throws JMSException {
		return createConsumer(destination, null);
	}

	@Override
	public javax.jms.MessageConsumer createConsumer(Destination destination, String messageSelector) throws JMSException {
		return createConsumer(destination, messageSelector, false);
	}

	@Override
	public javax.jms.MessageConsumer createConsumer(Destination destination, String messageSelector, boolean noLocal) throws JMSException {
		// TODO: what does noLocal mean?
		return new MessageConsumer(this, destination, 1, messageSelector);
	}

	@Override
	public javax.jms.MessageConsumer createSharedConsumer(javax.jms.Topic topic, String sharedSubscriptionName) throws JMSException {
		return null;
	}

	@Override
	public javax.jms.MessageConsumer createSharedConsumer(javax.jms.Topic topic, String sharedSubscriptionName, String messageSelector) throws JMSException {
		return null;
	}

	@Override
	public Queue createQueue(String queueName) throws JMSException {
		return null;
	}

	@Override
	public javax.jms.Topic createTopic(String topicName) throws JMSException {
		return new Topic(topicName);
	}

	@Override
	public TopicSubscriber createDurableSubscriber(javax.jms.Topic topic, String name) throws JMSException {
		return null;
	}

	@Override
	public TopicSubscriber createDurableSubscriber(javax.jms.Topic topic, String name, String messageSelector, boolean noLocal) throws JMSException {
		return null;
	}

	@Override
	public javax.jms.MessageConsumer createDurableConsumer(javax.jms.Topic topic, String name) throws JMSException {
		return null;
	}

	@Override
	public javax.jms.MessageConsumer createDurableConsumer(javax.jms.Topic topic, String name, String messageSelector, boolean noLocal) throws JMSException {
		return null;
	}

	@Override
	public javax.jms.MessageConsumer createSharedDurableConsumer(javax.jms.Topic topic, String name) throws JMSException {
		return null;
	}

	@Override
	public javax.jms.MessageConsumer createSharedDurableConsumer(javax.jms.Topic topic, String name, String messageSelector) throws JMSException {
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

	public Connection getConnection() {
		return connection;
	}
}
