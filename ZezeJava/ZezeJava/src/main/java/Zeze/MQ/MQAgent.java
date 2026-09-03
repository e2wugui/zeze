package Zeze.MQ;

import java.util.ArrayList;
import java.util.Collection;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import static Zeze.MQ.Master.AbstractMaster.eConsumerNotFound;

public class MQAgent extends AbstractMQAgent {
    private static final Logger logger = LogManager.getLogger();

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
			// 与futures同步：只记录已实际发出Subscribe的manager，回滚时精确撤销。
			var sentManagers = new ArrayList<Connector>();
			try {
				for (var manager : managers) {
					var r = new Subscribe();
					r.Argument.setTopic(topic);
					r.Argument.setSessionId(sessionId);
					r.SendForWait(manager.GetReadySocket());
					futures.add(r);
					sentManagers.add(manager);
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
			} catch (Exception ex) {
				// 半成功必须回滚：已发出的Subscribe在Manager端持续推送，而MQConsumer构造失败后
				// 引用被应用丢弃——幽灵消费者继续收消息并ack；consumers条目也随之泄漏。
				// 回滚失败仅记日志（不可达的manager本就没收到Subscribe），本地条目必须移除。
				unsubscribeFromManagers(topic, sessionId, sentManagers);
				consumers.remove(sessionId, consumer);
				throw ex;
			}
		}
	}

	public void unsubscribe(MQConsumer consumer, HashSet<Connector> managers) {
		try {
			unsubscribeFromManagers(consumer.getTopic(), consumer.getSessionId(), managers);
		} finally {
			// 必达：残留条目会让后续PushMessage继续投递给已关闭的consumer并被ack。
			consumers.remove(consumer.getSessionId(), consumer);
		}
	}

	// 逐台best-effort退订：单台失败仅记日志不中断，也不抛出（调用方无法补救）。
	private static void unsubscribeFromManagers(String topic, long sessionId, Collection<Connector> managers) {
		for (var manager : managers) {
			try {
				var r = new Unsubscribe();
				r.Argument.setTopic(topic);
				r.Argument.setSessionId(sessionId);
				r.SendForWait(manager.GetReadySocket()).await();
				if (r.getResultCode() != 0)
					logger.error("unsubscribe error={} topic={} sessionId={}",
							IModule.getErrorCode(r.getResultCode()), topic, sessionId);
			} catch (Exception e) {
				logger.error("unsubscribe failed. manager={} topic={} sessionId={}",
						manager.getName(), topic, sessionId, e);
			}
		}
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
		r.SendForWait(connector.GetReadySocket()).await();
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
