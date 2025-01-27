package Zeze.MQ;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.MQ.BSendMessage;
import Zeze.Builtin.MQ.PushMessage;
import Zeze.Builtin.MQ.SendMessage;
import Zeze.Builtin.MQ.Subscribe;
import Zeze.Builtin.MQ.Unsubscribe;
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

	public void start() throws Exception {
		service.start();
	}

	public void stop() throws Exception {
		service.stop();
	}

	public Connector getOrAddConnector(String host, int port) {
		var out = new OutObject<Connector>();
		if (service.getConfig().tryGetOrAddConnector(host, port, true, out))
			out.value.start();
		return out.value;
	}

	public void subscribe(String topic, long sessionId, MQConsumer consumer, HashSet<Connector> managers) {
		if (consumers.putIfAbsent(sessionId, consumer) == null) {
			var futures = new ArrayList<Subscribe>();
			for (var manager : managers) {
				var r = new Subscribe();
				r.Argument.setTopic(topic);
				r.Argument.setSessionId(sessionId);
				r.SendForWait(manager.GetReadySocket());
				futures.add(r);
			}
			// await all
			for (var future : futures) {
				assert future.getFuture() != null;
				future.getFuture().await();
			}
			// check all result code
			for (var future : futures) {
				if (future.getResultCode() != 0)
					throw new RuntimeException("subscribe consumer error=" + IModule.getErrorCode(future.getResultCode()));
			}
		}
	}

	public void unsubscribe(MQConsumer consumer, HashSet<Connector> managers) {
		// unsubscribe all
		var futures = new ArrayList<Unsubscribe>();
		for (var manager : managers) {
			var r = new Unsubscribe();
			r.Argument.setTopic(consumer.getTopic());
			r.Argument.setSessionId(consumer.getSessionId());
			r.SendForWait(manager.GetReadySocket());
			futures.add(r);
		}
		// await all
		for (var future : futures) {
			assert future.getFuture() != null;
			future.getFuture().await();
		}
		// check all result code
		for (var future : futures) {
			if (future.getResultCode() != 0)
				throw new RuntimeException("unsubscribe consumer error=" + IModule.getErrorCode(future.getResultCode()));
		}

		consumers.remove(consumer.getSessionId());
	}

	@Override
	protected long ProcessPushMessageRequest(PushMessage r) {
		var consumer = consumers.get(r.Argument.getSessionId());
		if (null == consumer)
			return errorCode(eConsumerNotFound);
		consumer.getListener().onMessage(r.Argument);
		r.SendResult();
		return 0;
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

	public static class Service extends Zeze.Net.Service {
		public Service() {
			super("Zeze.MQ.MQAgent");
		}
	}
}
