package Zeze.Services.RocketMQ;

import java.util.UUID;
import javax.jms.JMSContext;
import javax.jms.JMSException;

public class ConnectionFactory implements javax.jms.ConnectionFactory {

	private String nameServerAddress;
	private String clientId;

	public ConnectionFactory(String nameServerAddress) {
		this.nameServerAddress = nameServerAddress;
		this.clientId = UUID.randomUUID().toString();
	}

	public ConnectionFactory(String nameServerAddress, String clientId) {
		this.nameServerAddress = nameServerAddress;
		this.clientId = clientId;
	}

	@Override
	public javax.jms.Connection createConnection() throws JMSException {
		return createConnection(null, null);
	}

	@Override
	public javax.jms.Connection createConnection(String userName, String password) throws JMSException {
		return new Connection(nameServerAddress, clientId, "clientInstance" + clientId);
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

	private javax.jms.Connection createZezeConnection(String userName, String password) {
		final String instanceName = UUID.randomUUID().toString();
		return new Connection(nameServerAddress, clientId, instanceName);
	}
}
