package Zeze.Services.RocketMQ;

import java.util.UUID;
import javax.jms.Connection;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import Zeze.Services.RocketMQ.ZezeConnection;

public class ZezeConnectionFactory implements javax.jms.ConnectionFactory {

	private String nameServerAddress;
	private String clientId;

	public ZezeConnectionFactory(String nameServerAddress) {
		this.nameServerAddress = nameServerAddress;
		this.clientId = UUID.randomUUID().toString();
	}

	public ZezeConnectionFactory(String nameServerAddress, String clientId) {
		this.nameServerAddress = nameServerAddress;
		this.clientId = clientId;
	}

	@Override
	public Connection createConnection() throws JMSException {
		return createConnection(null, null);
	}

	@Override
	public Connection createConnection(String userName, String password) throws JMSException {
		return new ZezeConnection(nameServerAddress, clientId, "clientInstance" + clientId);
	}

	@Deprecated
	@Override
	public JMSContext createContext() {
		// TODO: not used now
		return null;
	}

	@Deprecated
	@Override
	public JMSContext createContext(String userName, String password) {
		// TODO: not used now
		return null;
	}

	@Deprecated
	@Override
	public JMSContext createContext(String userName, String password, int sessionMode) {
		// TODO: not used now
		return null;
	}

	@Deprecated
	@Override
	public JMSContext createContext(int sessionMode) {
		// TODO: not used now
		return null;
	}

	private Connection createZezeConnection(String userName, String password) {
		final String instanceName = UUID.randomUUID().toString();
		return new ZezeConnection(nameServerAddress, clientId, instanceName);
	}
}
