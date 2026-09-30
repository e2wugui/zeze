package Zeze.MQ;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Builtin.MQ.BSendMessage;
import Zeze.Builtin.MQ.PushMessage;
import Zeze.Builtin.MQ.SendMessage;
import Zeze.Builtin.MQ.Subscribe;
import Zeze.Builtin.MQ.Unsubscribe;
import Zeze.IModule;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Util.Action0;
import Zeze.Util.OutObject;
import Zeze.Util.TaskSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static Zeze.MQ.Master.AbstractMaster.eConsumerNotFound;

/**
 * MQ 客户端代理（进程级静态共享）：管理与各 Manager 的连接，承载消费者订阅/退订、
 * 消息推送分发与引用计数生命周期。
 */
public class MQAgent extends AbstractMQAgent {
    private static final Logger logger = LogManager.getLogger();

	private final Service service;
	private final ConcurrentHashMap<Long, MQConsumer> consumers = new ConcurrentHashMap<>();

	// 客户端生命周期（引用计数+显式MQ.shutdown()双轨）：
	// 静态共享的agent一旦启动即进程永生——close全部MQ/MQConsumer后connector仍按1..8秒退避
	// 无限重连Manager（端口/线程/连接资源不释放，网络错误日志不停）。引用计数归零时停connector
	// 重连；agent是进程级设施，归零停机不拒绝复活（新引用到达即随getOrAddConnector
	// 重启重连——"close即终态"的口径在MQ/MQConsumer实例层）；MQ.shutdown()为终态强制全停。
	private final Object lifecycleLock = new Object();
	// 持有方计数：MQ实例+MQConsumer实例（构造addRef，close/构造失败release）。
	private int refs;
	// 归零已停connector（可随新引用复活）。
	private boolean idleStopped;
	// MQ.shutdown()终态：此后addRef明确报错（进程停机中，重用需新进程）。
	private volatile boolean terminated;
	// 在飞网络轮计数（subscribe/unsubscribe/reSubscribe的fan-out）：归零停机前有界排空，
	// 避免停socket打断进行中的订阅事务（"先关门再等在飞一轮"，对齐Manager侧管理面排空形态；
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

	/** 取一个引用；MQ.shutdown()后明确报错。MQ/MQConsumer构造（经MQ.clientAddRefs）调用。 */
	public void addRef() {
		synchronized (lifecycleLock) {
			if (terminated)
				throw new IllegalStateException("MQAgent has been shutdown (MQ.shutdown());"
						+ " MQ client is terminated for this process, restart required");
			if (++refs == 1)
				idleStopped = false; // 从归零停机复活：connector由下一次getOrAddConnector重启重连
		}
	}

	/** 释放一个引用；归零时"先关门再等在飞一轮"后有界停connector重连。 */
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

	/** 强制全停（MQ.shutdown()调用，不等引用归零）：停service（含全部connector重连+socket）。幂等。 */
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

	public void subscribe(String topic, long sessionId, MQConsumer consumer, Set<Connector> managers) {
		netRounds.incrementAndGet();
		try {
			subscribeInternal(topic, sessionId, consumer, managers);
		} finally {
			netRounds.decrementAndGet();
		}
	}

	private void subscribeInternal(String topic, long sessionId, MQConsumer consumer, Set<Connector> managers) {
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
				for (var future : futures) {
					assert future.getFuture() != null;
					future.getFuture().await();
				}
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
			armRouteRefresh(); // 订阅成功即武装路由对账链（幂等，链已在位则空转）
		}
	}

	public void unsubscribe(MQConsumer consumer, Set<Connector> managers) {
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
	// 否则全部既有消费者静默饿死（与Manager→Master方向重注册对称的Consumer→Manager方向机制）。
	// Manager端MQPartition.subscribe按sessionId幂等：同socket重复无害；旧socket未及关闭时新socket
	// 的订阅替换旧条目。
	void onManagerConnected(AsyncSocket so) {
		// IO线程回调，不得同步等待rpc，提交任务池异步重发（failCount=0：新连接新链，退避重新起算）。
		TaskSpec.ofAction(() -> reSubscribeRound(so, 0)).name("MQAgent.reSubscribe").submitNow();
	}

	// mq-03（reconciler 兜底）：reSubscribe 失败不再只记日志"等下次重连"——OnHandshakeDone 是
	// 唯一触发点，socket 保持健康时它不再来（Manager 端 MQPartition 锁竞争的 Subscribe rpc 超时
	// 等单次瞬时失败即命中），该会话在这台 Manager 上的分区订阅永久丢失、静默饿死。
	// 失败进指数退避重试（重发幂等；订阅表 vs Manager 侧登记的差异即重试依据，全部成功=收敛即清）；
	// socket 关闭取消排期（重连新 socket 由 OnHandshakeDone 重新起链，无僵尸任务）。
	// 每socket单槽排期句柄，pending 数自限。
	private final ConcurrentHashMap<AsyncSocket, Future<?>> reSubscribeRetryFutures = new ConcurrentHashMap<>();

	// 包内可见：重订阅重试调度器（默认 TaskSpec 延迟调度，DaemonTimer 续约同形态）；
	// 测试注入捕获延迟序列/手动驱动以测退避形态（MQSingle.RetryScheduler 先例）。
	@FunctionalInterface
	interface RetryScheduler {
		Future<?> schedule(long delayMs, Action0 action) throws Exception;
	}
	@NotNull RetryScheduler reSubscribeRetryScheduler =
			(delayMs, action) -> TaskSpec.ofAction(action).name("MQAgent.reSubscribeRetry").scheduleNow(delayMs);

	// 指数退避：min(Cap, Base<<failCount)，移位钳制21位防溢出（MQSingle.retryBackoffMs 同公式，
	// 客户端侧常量：本类无 MQConfig）。封顶使持续故障下重试频率有界，收敛后自动清零。
	static long reSubscribeBackoffMs(int failCount) {
		return Math.min(1_000L << Math.min(failCount, 21), 60_000L);
	}

	// 不另建connector→consumers反向登记表：consumers（生命周期由subscribe/unsubscribe维护，
	// 失败回滚与finally必删保证无泄漏）×consumer.getManagers()（构造建立+路由对账增补，
	// 见 routeRefreshRound）即完整映射，派生遍历免登记/断连清理，无第二份可失步的状态。
	// 包内可见（测试直驱一轮重订阅）：返回本轮失败数（>0 由调用侧排期重试）。
	int reSubscribeRound(AsyncSocket so, int failCount) {
		netRounds.incrementAndGet(); // 归零停机排空的在飞轮之一（consumers空时即刻返回）
		try {
			// 兜底双保险：OnSocketClose 取消后的迟到触发/取消面遗漏——死 socket 上重发必然
			// 逐个超时（每消费者 Rpc 默认 5s），还把重试链挂在已死的 so 上，到此为止。
			if (so.isClosed())
				return 0;
			int failed = 0;
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
					if (r.getResultCode() != 0) {
						++failed;
						logger.error("re-subscribe error={} manager={} topic={} sessionId={}",
								IModule.getErrorCode(r.getResultCode()), connector.getName(),
								consumer.getTopic(), consumer.getSessionId());
					}
				} catch (Exception e) {
					++failed;
					logger.error("re-subscribe failed, will retry with backoff. manager={} topic={} sessionId={}",
							connector.getName(), consumer.getTopic(), consumer.getSessionId(), e);
				}
			}
			if (failed > 0)
				scheduleReSubscribeRetry(so, failCount + 1);
			else
				reSubscribeRetryFutures.remove(so); // 收敛：订阅表与 Manager 侧登记重新一致
			return failed;
		} finally {
			netRounds.decrementAndGet();
		}
	}

	// 失败排期下一轮（reSubscribeRound 锁外调用）：指数退避，单槽句柄（旧排期 cancel+replace）。
	// 调度失败（调度池关闭等停机窗口）链止于本轮：连接仍在时下次事件（推送/新订阅）不触发重订阅，
	// 但停机场景 socket 关闭后本链亦无意义；残余=进程存活且调度池关闭的窄窗口，接受。
	private void scheduleReSubscribeRetry(AsyncSocket so, int failCount) {
		var delayMs = reSubscribeBackoffMs(failCount);
		try {
			var future = reSubscribeRetryScheduler.schedule(delayMs, () -> reSubscribeRound(so, failCount));
			var old = reSubscribeRetryFutures.put(so, future);
			if (null != old)
				old.cancel(false); // 单槽：新排期作废旧排期（正常链上槽空入，防御替换）
		} catch (Exception e) {
			reSubscribeRetryFutures.remove(so);
			logger.error("re-subscribe retry schedule failed, backoff chain ends (next reconnect re-triggers)"
					+ " manager={}", so.getConnector() != null ? so.getConnector().getName() : so, e);
		}
	}

	// socket 关闭取消重试排期（Service.OnSocketClose 调用）：重连的新 socket 由 OnHandshakeDone
	// 重新起链，旧句柄不取消则成为指向死 socket 的僵尸重试（每轮逐消费者超时后退避再排）。
	// 包内可见（测试直驱）。
	void cancelReSubscribeRetry(AsyncSocket so) {
		var future = reSubscribeRetryFutures.remove(so);
		if (null != future)
			future.cancel(false);
	}

	// 路由快照对账（周期拉式）：managers 连接集在消费者构造时固化、不随 Master 路由刷新，
	// Manager 换址迁移后既存消费者持旧址连接器静默饿死（新 Manager 订阅表为空、消息积压
	// 且应用侧零信号）。每轮对每个存活消费者重取一次 Master 路由，地址集差量补建 connector
	// 入 managers（只增不减），新连接器握手后由既有重订阅链以原 sessionId 幂等补发
	// Subscribe；失败记日志等下一周期，有界时间内最终一致。
	private final Object routeRefreshLock = new Object();
	// 单飞排期句柄（锁内维护，不用布尔标志——对账轮见空终止与并发订阅武装的窄窗下标志形态
	// 会漏排期成死链）：本轮执行即清空重排；消费者清空则本轮终止不续排，链的生命周期=首个
	// 订阅武装、末个消费者退订后的下一轮终止。
	private Future<?> routeRefreshFuture;
	// 包内可见（测试反射缩短周期求确定性）：路由对账周期。
	long routeRefreshPeriodMs = 30_000;

	// 订阅成功即确保对账链在排期（幂等：已有在飞排期则空转）。
	private void armRouteRefresh() {
		synchronized (routeRefreshLock) {
			scheduleRouteRefreshLocked();
		}
	}

	private void scheduleRouteRefreshLocked() {
		if (null != routeRefreshFuture && !routeRefreshFuture.isDone())
			return; // 已有在飞排期
		if (consumers.isEmpty())
			return; // 无消费者不排期（链终止态，重新武装由下一次订阅）
		try {
			routeRefreshFuture = TaskSpec.ofAction(this::routeRefreshRound).name("MQAgent.routeRefresh")
					.scheduleNow(routeRefreshPeriodMs);
		} catch (Exception e) {
			routeRefreshFuture = null; // 链止于本轮（调度池关闭等停机窗口），下次订阅重新武装
			logger.error("route refresh schedule failed (next subscribe re-arms)", e);
		}
	}

	// 一轮路由对账（周期排期驱动，任务池线程）：锁内先清句柄续排期再做慢路径（master rpc
	// 不持锁）。计入 netRounds 使归零停机的排空等待覆盖本轮，最后一个消费者 close 与对账
	// 增补 connector 的竞态不造出引用已空的僵尸连接器。
	private void routeRefreshRound() {
		synchronized (routeRefreshLock) {
			routeRefreshFuture = null; // 本轮即排期之执行
			scheduleRouteRefreshLocked(); // 消费者仍在则续排下一轮；见空则链终止
		}
		netRounds.incrementAndGet();
		try {
			for (var consumer : consumers.values()) {
				try {
					// close()竞态防护（对齐 reSubscribeRound）：条目身份校验仍在才增补。
					if (consumers.get(consumer.getSessionId()) != consumer)
						continue;
					var servers = MQ.masterAgent.subscribe(consumer.getTopic());
					for (var server : servers.getServers()) {
						// getOrAddConnector 按地址幂等（同址返回既有实例），set.add 的去重即地址集差量。
						var connector = getOrAddConnector(server.getHost(), server.getPort());
						if (!consumer.getManagers().add(connector))
							continue;
						logger.info("consumer route refreshed (manager address migration followed)."
								+ " topic={} sessionId={} manager={}",
								consumer.getTopic(), consumer.getSessionId(), connector.getName());
						// 竞态收口：握手可能早于入集完成（OnHandshakeDone 触发的重订阅轮见不到该
						// 连接器而漏订），已就绪则直接驱动一轮重订阅（幂等；未就绪由即将到来的握手触发）。
						var so = connector.TryGetReadySocket();
						if (null != so && !so.isClosed())
							reSubscribeRound(so, 0);
					}
				} catch (Exception e) {
					logger.error("consumer route refresh failed, retry next round. topic={} sessionId={}",
							consumer.getTopic(), consumer.getSessionId(), e);
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

		@Override
		public void OnSocketClose(@NotNull AsyncSocket so, @Nullable Throwable e) throws Exception {
			super.OnSocketClose(so, e);
			// 只关心连向Manager的连接（connector方向）：其上的重订阅重试随 socket 失效一并取消。
			var a = agent;
			if (null != a && null != so.getConnector())
				a.cancelReSubscribeRetry(so);
		}
	}
}
