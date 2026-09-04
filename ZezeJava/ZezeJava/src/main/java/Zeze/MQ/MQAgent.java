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
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Util.OutObject;
import Zeze.Util.TaskSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import static Zeze.MQ.Master.AbstractMaster.eConsumerNotFound;

public class MQAgent extends AbstractMQAgent {
    private static final Logger logger = LogManager.getLogger();

	private final Service service;
	private final ConcurrentHashMap<Long, MQConsumer> consumers = new ConcurrentHashMap<>();

	public MQAgent() {
		service = new Service();
		service.setAgent(this);
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

	// Manager重启即丢失subscribes（纯内存态，不持久化），消费者连接自动重连后必须重发Subscribe，
	// 否则全部既有消费者静默饿死（修复前仅MQConsumer构造时订阅一次，无重连钩子——与ef63301b8
	// 修复的Manager→Master方向重注册对称的Consumer→Manager方向机制）。Subscribe在Manager端
	// MQPartition.subscribe按putIfAbsent幂等，与首连时subscribe()的发送重叠无害。
	// 重发失败仅记日志等下次重连再试，不引入新定时器。
	void onManagerConnected(AsyncSocket so) {
		// IO线程回调，不得同步等待rpc，提交任务池异步重发。
		TaskSpec.ofAction(() -> reSubscribe(so)).name("MQAgent.reSubscribe").submitNow();
	}

	// 不另建connector→consumers反向登记表：consumers（生命周期由subscribe/unsubscribe维护，
	// 失败回滚与finally必删保证无泄漏）×consumer.getManagers()（构造时确定）即完整映射，
	// 派生遍历免登记/断连清理，无第二份可失步的状态。
	private void reSubscribe(AsyncSocket so) {
		var connector = so.getConnector();
		for (var consumer : consumers.values()) {
			if (!consumer.getManagers().contains(connector))
				continue;
			// close()竞态防护：条目身份校验仍在才重发。即便校验后瞬断竞态在Manager端留下幽灵订阅，
			// PushMessage回eConsumerNotFound后由Manager端handlePushResult的自动unsubscribe清理。
			if (consumers.get(consumer.getSessionId()) != consumer)
				continue;
			try {
				var r = new Subscribe();
				r.Argument.setTopic(consumer.getTopic());
				r.Argument.setSessionId(consumer.getSessionId());
				r.SendForWait(so).await();
				if (r.getResultCode() != 0)
					logger.error("re-subscribe error={} manager={} topic={} sessionId={}",
							IModule.getErrorCode(r.getResultCode()), connector.getName(),
							consumer.getTopic(), consumer.getSessionId());
			} catch (Exception e) {
				logger.error("re-subscribe failed, wait for next reconnect. manager={} topic={} sessionId={}",
						connector.getName(), consumer.getTopic(), consumer.getSessionId(), e);
			}
		}
	}

	public static class Service extends Zeze.Net.Service {
		private volatile MQAgent agent;

		public Service() {
			super("Zeze.MQ.MQAgent");
		}

		public void setAgent(MQAgent agent) {
			this.agent = agent;
		}

		@Override
		public void OnHandshakeDone(@NotNull AsyncSocket so) throws Exception {
			super.OnHandshakeDone(so);
			// 只对连向Manager的连接（connector方向）触发；此时socket已完成握手可安全发送协议。
			var a = agent;
			if (null != a && null != so.getConnector())
				a.onManagerConnected(so);
		}
	}
}
