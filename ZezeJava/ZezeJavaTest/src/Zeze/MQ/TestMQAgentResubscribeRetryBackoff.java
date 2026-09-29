package Zeze.MQ;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.MQ.Master.BMQInfo;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Net.Protocol;
import Zeze.Net.Service;
import Zeze.Util.Action0;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * mq-03 回归（模式A：reconciler 兜底）：reSubscribe 失败不再只记日志等下次重连。
 * <p>
 * 修复前：OnHandshakeDone 是重订阅的唯一触发点，失败（Manager 端 MQPartition 锁竞争的
 * Subscribe rpc 超时等单次瞬时失败）仅记日志——socket 保持健康则握手回调不再来，该会话
 * 在这台 Manager 上的全部分区订阅永久丢失、静默饿死（消息在盘上无限积压，仅一条 error
 * 日志可循）。
 * <p>
 * 修复后不变式：任一 (socket, consumer) 重订阅失败，最终在有界延迟内必有下一次尝试——
 * 指数退避（base<<n 封顶）单槽排期；全部成功即清（订阅表与 Manager 侧登记重新一致）；
 * socket 关闭取消排期（重连新 socket 由握手回调重新起链），且死 socket 上的迟到触发短路
 * 不再排期（取消面遗漏的双保险）。
 * <p>
 * 判别：失败注入=替身 socket Send 恒 false（SendForWait 即刻 setException，不等 rpc 5s
 * 超时）；MQConsumer 经 Unsafe 免构造铸造（构造器走网络，测试只读其 managers/topic/
 * sessionId 三者）；调度器/轮次/取消缝均反射访问（baseline 无这些成员判红——测试须在
 * baseline 编译通过，TestGBD03FillRetrySelfSchedule 先例）。全程无网络，@Fast。
 */
@Fast
public class TestMQAgentResubscribeRetryBackoff {

	/** 一次捕获的排期（延迟+动作+返回句柄，测试手动驱动=确定性时钟）。 */
	private static final class CapturedSchedule {
		final long delayMs;
		final Action0 action;
		final CompletableFuture<Void> handle = new CompletableFuture<>();

		CapturedSchedule(long delayMs, Action0 action) {
			this.delayMs = delayMs;
			this.action = action;
		}
	}

	/** Send 恒 false 的替身 socket（Fnd19 FakeSocket 形态）：SendForWait 的 future 即刻失败
	 * （不等 rpc 5s 超时）；可指定 connector 与 closed 形态。 */
	private static final class FailSendSocket extends AsyncSocket {
		Connector connector;
		volatile boolean closed;

		FailSendSocket(Service service) {
			super(service);
		}

		@Override
		public Type getType() {
			return Type.eClient;
		}

		@Override
		protected void doClose(Throwable ex, boolean gracefully) {
		}

		@Override
		public boolean Send(Protocol<?> p) {
			return false;
		}

		@Override
		public boolean Send(byte[] bytes, int offset, int length) {
			return false;
		}

		@Override
		public Connector getConnector() {
			return connector;
		}

		@Override
		public boolean isClosed() {
			return closed;
		}

		@Override
		public Zeze.Util.TimeThrottle getTimeThrottle() {
			return null;
		}

		@Override
		public java.net.SocketAddress getRemoteAddress() {
			return null;
		}
	}

	/** Unsafe 免构造铸造 MQConsumer（只填 round 读的三个字段：sessionId/info/managers）。 */
	private static MQConsumer fakeConsumer(long sessionId, String topic, Connector connector) throws Exception {
		var theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
		theUnsafe.setAccessible(true);
		var unsafe = (sun.misc.Unsafe)theUnsafe.get(null);
		var consumer = (MQConsumer)unsafe.allocateInstance(MQConsumer.class);
		var sessionIdField = MQConsumer.class.getDeclaredField("sessionId");
		sessionIdField.setAccessible(true);
		sessionIdField.setLong(consumer, sessionId);
		var info = new BMQInfo.Data();
		info.setTopic(topic);
		setFinal(consumer, "info", info);
		var managers = new HashSet<Connector>();
		managers.add(connector);
		setFinal(consumer, "managers", managers);
		return consumer;
	}

	private static void setFinal(Object obj, String name, Object value) throws Exception {
		var f = obj.getClass().getDeclaredField(name);
		f.setAccessible(true);
		f.set(obj, value);
	}

	/** 反射注入 reSubscribeRetryScheduler（捕获不执行）；baseline 无此字段判红。 */
	private static List<CapturedSchedule> injectRetryScheduler(MQAgent agent) throws Exception {
		var captured = new ArrayList<CapturedSchedule>();
		try {
			Field f = MQAgent.class.getDeclaredField("reSubscribeRetryScheduler");
			f.setAccessible(true);
			f.set(agent, (MQAgent.RetryScheduler)(delayMs, action) -> {
				var schedule = new CapturedSchedule(delayMs, action);
				captured.add(schedule);
				return schedule.handle;
			});
		} catch (NoSuchFieldException e) {
			throw new AssertionError("reSubscribe 失败退避重试机制缺失（mq-03 修复不存在）", e);
		}
		return captured;
	}

	/** 反射直驱一轮重订阅；baseline 无此方法判红。 */
	private static int invokeRound(MQAgent agent, AsyncSocket so, int failCount) throws Throwable {
		Method m;
		try {
			m = MQAgent.class.getDeclaredMethod("reSubscribeRound", AsyncSocket.class, int.class);
		} catch (NoSuchMethodException e) {
			throw new AssertionError("reSubscribeRound 缺失（mq-03 修复不存在）", e);
		}
		m.setAccessible(true);
		try {
			return (Integer)m.invoke(agent, so, failCount);
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}
	}

	private static void invokeCancel(MQAgent agent, AsyncSocket so) throws Throwable {
		Method m;
		try {
			m = MQAgent.class.getDeclaredMethod("cancelReSubscribeRetry", AsyncSocket.class);
		} catch (NoSuchMethodException e) {
			throw new AssertionError("cancelReSubscribeRetry 缺失（mq-03 修复不存在）", e);
		}
		m.setAccessible(true);
		try {
			m.invoke(agent, so);
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}
	}

	@SuppressWarnings("unchecked")
	private static ConcurrentHashMap<AsyncSocket, Future<?>> retrySlots(MQAgent agent) throws Exception {
		Field f = MQAgent.class.getDeclaredField("reSubscribeRetryFutures");
		f.setAccessible(true);
		return (ConcurrentHashMap<AsyncSocket, Future<?>>)f.get(agent);
	}

	private static long backoffOf(int failCount) throws Throwable {
		Method m;
		try {
			m = MQAgent.class.getDeclaredMethod("reSubscribeBackoffMs", int.class);
		} catch (NoSuchMethodException e) {
			throw new AssertionError("reSubscribeBackoffMs 缺失（mq-03 修复不存在）", e);
		}
		m.setAccessible(true);
		try {
			return (Long)m.invoke(null, failCount);
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}
	}

	/**
	 * 核心不变式：重订阅失败后无任何外部事件（无重连、无新订阅），退避排期驱动重试；
	 * 连续失败指数退避、单槽句柄 cancel+replace；全部成功即清（收敛后不再排期）。
	 */
	@Test
	public void testFailedRoundRetriesWithBackoffAndConverges() throws Throwable {
		Task.tryInitThreadPool();
		var agent = new MQAgent();
		var captured = injectRetryScheduler(agent);
		var connector = new Connector("127.0.0.1", 1, false);
		var so = new FailSendSocket(new MQAgent.Service());
		so.connector = connector;
		agent.getConsumers().put(99L, fakeConsumer(99L, "topicRetry", connector));

		// 第 1 轮失败（Send 恒 false → 每个目标消费者计入失败）→ 排期退避 base<<1。
		Assertions.assertEquals(1, invokeRound(agent, so, 0), "发送失败计入失败数");
		Assertions.assertEquals(1, captured.size(), "失败必须排期重试（baseline 仅记日志：订阅永久丢失）");
		Assertions.assertEquals(2000L, captured.get(0).delayMs, "首次失败退避=base<<1（MQSingle.retryBackoffMs 同式）");
		Assertions.assertTrue(retrySlots(agent).containsKey(so), "单槽排期句柄在位");

		// 退避到期驱动第 2 轮（无外部事件，仍失败）→ 指数退避 + 单槽替换。
		captured.get(0).action.run();
		Assertions.assertEquals(2, captured.size(), "重试轮失败必须继续排期（自驱动恢复链不断）");
		Assertions.assertEquals(4000L, captured.get(1).delayMs, "连续失败指数退避（base<<2）");
		Assertions.assertTrue(captured.get(0).handle.isCancelled(), "单槽句柄：新排期 cancel 旧排期");
		Assertions.assertTrue(retrySlots(agent).containsKey(so));

		// 收敛：消费者退订后该 socket 无重发面 → 本轮零失败 → 链清（不再排期）。
		agent.getConsumers().clear();
		captured.get(1).action.run();
		Assertions.assertEquals(2, captured.size(), "收敛后不再排期（成功即清）");
		Assertions.assertTrue(retrySlots(agent).isEmpty(), "槽清：订阅表与 Manager 侧登记一致");
	}

	/**
	 * socket 关闭取消排期；取消面遗漏（迟到触发）时死 socket 短路——不重发也不再排期
	 * （否则每轮逐消费者超时后再退避，僵尸链挂在死 socket 上）。
	 */
	@Test
	public void testSocketCloseCancelsAndDeadSocketShortCircuits() throws Throwable {
		Task.tryInitThreadPool();
		var agent = new MQAgent();
		var captured = injectRetryScheduler(agent);
		var connector = new Connector("127.0.0.1", 1, false);
		var so = new FailSendSocket(new MQAgent.Service());
		so.connector = connector;
		agent.getConsumers().put(7L, fakeConsumer(7L, "topicCancel", connector));

		Assertions.assertEquals(1, invokeRound(agent, so, 0), "前置：失败一轮并排期");
		Assertions.assertEquals(1, captured.size());

		// socket 关闭（OnSocketClose 面）：排期取消、槽清。
		invokeCancel(agent, so);
		Assertions.assertTrue(captured.get(0).handle.isCancelled(), "socket 关闭必须取消重试排期");
		Assertions.assertTrue(retrySlots(agent).isEmpty(), "取消后槽清（重连新 socket 由握手回调重新起链）");

		// 兜底双保险：取消面遗漏/迟到触发到达死 socket——短路返回，不重发不排期。
		so.closed = true;
		Assertions.assertEquals(0, invokeRound(agent, so, 5), "死 socket 短路：不计失败");
		Assertions.assertEquals(1, captured.size(), "死 socket 不得再排期（无僵尸重试链）");
	}

	/** 退避公式：min(Cap, Base<<failCount)，移位钳制21位防溢出。 */
	@Test
	public void testBackoffFormulaCaps() throws Throwable {
		Assertions.assertEquals(2000L, backoffOf(1));
		Assertions.assertEquals(16000L, backoffOf(4));
		Assertions.assertEquals(60000L, backoffOf(6), "base<<6=64000 越过封顶");
		Assertions.assertEquals(60000L, backoffOf(30), "持续失败重试频率有界=退避封顶");
	}
}
