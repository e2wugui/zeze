package Zeze.MQ;

import Zeze.Builtin.MQ.BMessage;
import Zeze.Builtin.MQ.SendMessage;
import Zeze.IModule;
import Zeze.Net.Connector;
import Zeze.Util.OutObject;

public class MQAgent extends AbstractMQAgent {

	private final Service service;

	public MQAgent() {
		service = new Service();
		RegisterProtocols(service);
	}

	public Connector getOrAddConnector(String host, int port) {
		var out = new OutObject<Connector>();
		if (service.getConfig().tryGetOrAddConnector(host, port, true, out))
			out.value.start();
		return out.value;
	}

	public static void sendMessageTo(BMessage.Data message, Connector connector) {
		var r = new SendMessage();
		r.Argument = message;
		r.SendForWait(connector.GetReadySocket());
		if (r.getResultCode() != 0)
			throw new RuntimeException("sendMessage error=" + IModule.getErrorCode(r.getResultCode()));
	}

	public static class Service extends Zeze.Net.Service {
		public Service() {
			super("Zeze.MQ.MQAgent");
		}
	}
}
