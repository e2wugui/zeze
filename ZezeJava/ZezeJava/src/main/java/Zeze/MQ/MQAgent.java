package Zeze.MQ;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
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

	// 【GB-D04】客户端生命周期（拍板方案A：引用计数+显式MQ.shutdown()双轨）：
	// 静态共享的agent一旦启动即进程永生——close全部MQ/MQConsumer后connector仍按1..8秒退避
	// 无限重连Manager（端口/线程/连接资源不释放，网络错误日志不停）。引用计数归零时停connector
	// 重连（不再续排）；agent是进程级设施，归零停机不拒绝复活（新引用到达即随getOrAddConnector
	// 重启重连——"close即终态"的口径在MQ/MQConsumer实例层）；MQ.shutdown()为终态强制全停。
	private final Object lifecycleLock = new Object();
	// 持有方计数：MQ实例+MQConsumer实例（构造addRef，close/构造失败release）。
	private int refs;
	// 归零已停connector（可随新引用复活）。
	private boolean idleStopped;
	// MQ.shutdown()终态：此后addRef明确报错（进程停机中，重用需新进程）。
	private volatile boolean terminated;
	// 在飞网络轮计数（subscribe/unsubscribe/reSubscribe的fan-out）：归零停机前有界排空，
	// 避免停socket打断进行中的订阅事务（"先关门再等在飞一轮"，对齐Manager侧GB-C02形态；
	// 关门=引用已归零：addRef与本锁互斥，排空期间新引用到达则复活不停）。
	private final AtomicInteger netRounds = new AtomicInteger();
	// 排空预算=Rpc默认超时5s+5s余量（subscribe的SendForWait不传超时，按Rpc字段默认5000ms）。
	private static final long netRoundsDrainBudgetMs = 5_000 + 5_000;

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

	/** 【GB-D04】取一个引用；MQ.shutdown()后明确报错。MQ/MQConsumer构造（经MQ.clientAddRefs）调用。 */
	public void addRef() {
		synchronized (lifecycleLock) {
			if (terminated)
				throw new IllegalStateException("MQAgent has been shutdown (MQ.shutdown());"
						+ " MQ client is terminated for this process, restart required");
			if (++refs == 1)
				idleStopped = false; // 从归零停机复活：connector由下一次getOrAddConnector重启重连
		}
	}

	/** 【GB-D04】释放一个引用；归零时"先关门再等在飞一轮"后有界停connector重连。 */
	public void release() {
		int after;
		synchronized (lifecycleLock) {
			after = --refs;
			if (after < 0) {
				refs = 0; // 防御：多余的release钳回0，不放大为负干扰后续归零判定
				return;
			}
		}
		if (after > 0)
			return;
		awaitNetRoundsDrained();
		synchronized (lifecycleLock) {
			if (refs != 0 || terminated)
				return; // 排空期间有新引用（复活）或已强制停机
			// 只停connector（停重连+关socket）不stop整个service：service.stop会置停机屏障，
			// 复活后addSocket被拒；connector级停止让复活路径仅需重启connector。
			service.getConfig().forEachConnector(Connector::stop);
			idleStopped = true;
		}
	}

	/** 【GB-D04】强制全停（MQ.shutdown()调用，不等引用归零）：停service（含全部connector重连+socket）。幂等。 */
	public void shutdown() {
		synchronized (lifecycleLock) {
			terminated = true;
		}
		try {
			service.stop();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/** 诊断/测试：当前引用计数。 */
	public int getRefs() {
		synchronized (lifecycleLock) {
			return refs;
		}
	}

	/** 诊断/测试：是否处于归零停机（connector已停，可随新引用复活）。 */
	public boolean isIdleStopped() {
		synchronized (lifecycleLock) {
			return idleStopped;
		}
	}

	/** 诊断/测试：是否已被MQ.shutdown()强制停机（终态）。 */
	public boolean isTerminated() {
		return terminated;
	}

	// 归零停机的在飞轮排空：poll直至归零或超预算（超时仅告警继续停——残余轮在socket关闭后
	// 以rpc异常收场，有界无损坏）。netRounds只减自subscribe/unsubscribe/reSubscribe，
	// 归零后consumers恒空，reSubscribe重触发也即刻返回，排空必收敛。
	private void awaitNetRoundsDrained() {
		var deadline = System.currentTimeMillis() + netRoundsDrainBudgetMs;
		while (netRounds.get() != 0) {
			if (System.currentTimeMillis() >= deadline) {
				logger.warn("MQAgent net rounds not drained in {}ms, continue stopping connectors", netRoundsDrainBudgetMs);
				return;
			}
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}
	}

	public Connector getOrAddConnector(String host, int port) {
		var out = new OutObject<Connector>();
		service.getConfig().tryGetOrAddConnector(host, port, true, out);
		// 无条件start（幂等）：新增连接器需要启动；归零停机复活后getOr到的是已stop的存量
		// 连接器，需重启重连（已连接的连接器start为no-op——socket非null直接返回）。
		out.value.start();
		return out.value;
	}

	public void subscribe(String topic, long sessionId, MQConsumer consumer, HashSet<Connector> managers) {
		netRounds.incrementAndGet();
		try {
			subscribeInternal(topic, sessionId, consumer, managers);
		} finally {
			netRounds.decrementAndGet();
		}
	}

	private void subscribeInternal(String topic, long sessionId, MQConsumer consumer, HashSet<Connector> managers) {
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
		netRounds.incrementAndGet();
		try {
			unsubscribeFromManagers(consumer.getTopic(), consumer.getSessionId(), managers);
		} finally {
			// 必达：残留条目会让后续PushMessage继续投递给已关闭的consumer并被ack。
			consumers.remove(consumer.getSessionId(), consumer);
			netRounds.decrementAndGet();
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
	// 修复的Manager→Master方向重注册对称的Consumer→Manager方向机制）。Manager端MQPartition.subscribe
	// 按sessionId幂等：同socket重复无害；旧socket未及关闭时新socket的订阅替换旧条目。
	// 重发失败仅记日志等下次重连再试，不引入新定时器。
	void onManagerConnected(AsyncSocket so) {
		// IO线程回调，不得同步等待rpc，提交任务池异步重发。
		TaskSpec.ofAction(() -> reSubscribe(so)).name("MQAgent.reSubscribe").submitNow();
	}

	// 不另建connector→consumers反向登记表：consumers（生命周期由subscribe/unsubscribe维护，
	// 失败回滚与finally必删保证无泄漏）×consumer.getManagers()（构造时确定）即完整映射，
	// 派生遍历免登记/断连清理，无第二份可失步的状态。
	private void reSubscribe(AsyncSocket so) {
		netRounds.incrementAndGet(); // 归零停机排空的在飞轮之一（consumers空时即刻返回）
		try {
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
		} finally {
			netRounds.decrementAndGet();
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
