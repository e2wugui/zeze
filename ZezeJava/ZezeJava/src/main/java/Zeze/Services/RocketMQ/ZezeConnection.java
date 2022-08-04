package Zeze.Services.RocketMQ;

import java.util.ArrayList;
import java.util.List;
import javax.jms.ConnectionConsumer;
import javax.jms.ConnectionMetaData;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.ServerSessionPool;
import javax.jms.Session;
import javax.jms.Topic;
import org.apache.rocketmq.client.ClientConfig;
import org.apache.rocketmq.client.impl.MQClientManager;
import org.apache.rocketmq.client.impl.factory.MQClientInstance;

public class ZezeConnection implements javax.jms.Connection {
	private String clientID;
	private ClientConfig clientConfig = new ClientConfig();
	private MQClientInstance clientInstance;
	private List<ZezeSession> sessionList = new ArrayList();
	private int sendTimeout = 0;

	public ZezeConnection(String nameServerAddress, String clientID, String instanceName) {
		this.clientID = clientID;
		this.clientConfig.setNamesrvAddr(nameServerAddress);
		this.clientConfig.setInstanceName(instanceName);

//		startClientInstance();
	}

	@Override
	public Session createSession(boolean transacted, int acknowledgeMode) throws JMSException {
		if (transacted) {
			// TODO:
		}
		if (acknowledgeMode == Session.AUTO_ACKNOWLEDGE) {
			// TODO:
		}

		ZezeSession session = new ZezeSession(this, acknowledgeMode, transacted);
		sessionList.add(session);
		return session;
	}

	@Override
	public Session createSession(int sessionMode) throws JMSException {
		return createSession(sessionMode == Session.SESSION_TRANSACTED, sessionMode);
	}

	@Override
	public Session createSession() throws JMSException {
		return createSession(false, Session.AUTO_ACKNOWLEDGE);
	}

	@Override
	public String getClientID() throws JMSException {
		return clientID;
	}

	@Override
	public void setClientID(String clientID) throws JMSException {
		this.clientID = clientID;
	}

	@Deprecated
	@Override
	public ConnectionMetaData getMetaData() throws JMSException {
		// We won't use this.
		return null;
	}

	@Deprecated
	@Override
	public ExceptionListener getExceptionListener() throws JMSException {
		// We won't use this.
		return null;
	}

	@Deprecated
	@Override
	public void setExceptionListener(ExceptionListener listener) throws JMSException {
		// We won't use this.
	}

	@Override
	public void start() throws JMSException {

	}

	@Override
	public void stop() throws JMSException {

	}

	@Override
	public void close() throws JMSException {

	}

	@Override
	public ConnectionConsumer createConnectionConsumer(Destination destination, String messageSelector, ServerSessionPool sessionPool, int maxMessages) throws JMSException {
		return null;
	}

	@Override
	public ConnectionConsumer createSharedConnectionConsumer(Topic topic, String subscriptionName, String messageSelector, ServerSessionPool sessionPool, int maxMessages) throws JMSException {
		return null;
	}

	@Override
	public ConnectionConsumer createDurableConnectionConsumer(Topic topic, String subscriptionName, String messageSelector, ServerSessionPool sessionPool, int maxMessages) throws JMSException {
		return null;
	}

	@Override
	public ConnectionConsumer createSharedDurableConnectionConsumer(Topic topic, String subscriptionName, String messageSelector, ServerSessionPool sessionPool, int maxMessages) throws JMSException {
		return null;
	}

	public int getSendTimeout() {
		return sendTimeout;
	}

	public void setSendTimeout(int sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	public ClientConfig getClientConfig() {
		return clientConfig;
	}

	private void startClientInstance() {
		try {
			this.clientInstance = MQClientManager.getInstance().getOrCreateMQClientInstance(this.clientConfig);
		} catch (Exception e) {
			// TODO: solve exception
			e.printStackTrace();
		}
	}
}
