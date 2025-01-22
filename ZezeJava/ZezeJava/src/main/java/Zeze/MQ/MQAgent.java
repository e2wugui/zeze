package Zeze.MQ;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.MQ.BSendMessage;
import Zeze.Builtin.MQ.PushMessage;
import Zeze.Builtin.MQ.SendMessage;
import Zeze.Builtin.MQ.Subscribe;
import Zeze.IModule;
import Zeze.Net.Connector;
import Zeze.Util.OutObject;
import static Zeze.MQ.Master.AbstractMaster.eConsumerNotFound;

public class MQAgent extends AbstractMQAgent {

	private final Service service;
	private final ConcurrentHashMap<Long, MQConsumer> consumers = new ConcurrentHashMap<>();

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

	public Subscribe subscribe(String topic, long sessionId, MQConsumer consumer, Connector connector) {
		if (consumers.putIfAbsent(sessionId, consumer) == null) {
			var r = new Subscribe();
			r.Argument.setTopic(topic);
			r.Argument.setSessionId(sessionId);
			r.SendForWait(connector.GetReadySocket());
			return r;
		}
		return null;
	}

	public void unsubscribe(MQConsumer consumer) {
		// todo unsubscribe
		consumers.remove(consumer.getSessionId());
	}

	public static void sendMessageTo(BSendMessage.Data message, Connector connector) {
		var r = new SendMessage();
		r.Argument = message;
		r.SendForWait(connector.GetReadySocket());
		if (r.getResultCode() != 0)
			throw new RuntimeException("sendMessage error=" + IModule.getErrorCode(r.getResultCode()));
	}

	public ConcurrentHashMap<Long, MQConsumer> getConsumers() {
		return consumers;
	}

	@Override
	protected long ProcessPushMessageRequest(PushMessage r) throws Exception {
		var consumer = consumers.get(r.Argument.getSessionId());
		if (null == consumer)
			return errorCode(eConsumerNotFound);
		consumer.getListener().onMessage(r.Argument);
		r.SendResult();
		return 0;
	}

	public static class Service extends Zeze.Net.Service {
		public Service() {
			super("Zeze.MQ.MQAgent");
		}
	}
}
