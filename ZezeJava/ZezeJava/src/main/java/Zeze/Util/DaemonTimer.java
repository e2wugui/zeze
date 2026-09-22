package Zeze.Util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * 周期守护定时器：把"调度线程只派发、守护体进worker池、stop两段式（关门→cancel→限时等待
 * 在飞一轮）"的停机并发协议固化成组件，替代各服务手抄的 shutdown标志+running标志+awaitIdle
 * 三件套（该模式曾手抄于GCM同步/异步/Raft三版与LoginQueue，第N份拷贝过期即成缺陷）。
 * <p>
 * 机制：
 * <ul>
 * <li>自续约链：一轮守护体结束才续约下一轮（{@link TaskSpec#scheduleNow}单次排期），
 * 语义对齐scheduleWithFixedDelay——间隔自本轮结束起算；正常路径不可能重叠执行，仅
 * stop超预算逃逸的轮次例外（逃逸轮不占链，见rescheduleLocked与runBody对inFlight的
 * 归属检查——restart后逃逸轮与新一轮可能并发，与被替代的手抄版行为一致）；</li>
 * <li>fire（调度线程上）只做gate内判关门+置在飞标志+runNow派发，微秒级返回，守护体的阻塞
 * 不会占用调度线程（FND7-17）；</li>
 * <li>pending是普通JDK ScheduledFuture（scheduleCore不包TimerFuture锁）：cancel无锁序约束，
 * "cancel不得持业务锁"从调用纪律变成结构属性（无ABBA可能）；</li>
 * <li>stop()：gate内置关门标志并捕获pending/inFlight，锁外cancel pending、按预算
 * （{@link Task#defaultTimeout}+5s，对齐executeCore看门狗）限时等待在飞一轮；超预算告警返回，
 * 调用方对被守护资源的访问必须自身容错（如在已停止的Service上GetSocket返回null）。</li>
 * </ul>
 * 【调用约束】stop()不得持有body执行期间可能获取的任何锁——限时等待与在飞body互等即死锁。
 * start()/stop()幂等，支持stop后restart。
 */
public final class DaemonTimer {
	@FunctionalInterface
	public interface Body {
		void run() throws Exception;
	}

	private static final @NotNull Logger logger = LogManager.getLogger(DaemonTimer.class);

	private final @NotNull String name;
	private final long periodMs;
	private final @NotNull Body body;
	// gate守护shutdown/pending/inFlight的全部读写；gate内不做任何等待与阻塞调用。
	private final ReentrantLock gate = new ReentrantLock();
	private boolean shutdown = true; // 未启动即关门
	// 在飞一轮的完成信号：fire派发前置位，守护体结束时完成（无论成败）。stop据此限时等待。
	private volatile CompletableFuture<Void> inFlight;
	private ScheduledFuture<?> pending; // 已排期未触发的下一次fire；fire触发时消费置null

	public DaemonTimer(@NotNull String name, long periodMs, @NotNull Body body) {
		if (periodMs <= 0)
			throw new IllegalArgumentException("periodMs <= 0");
		this.name = name;
		this.periodMs = periodMs;
		this.body = body;
	}

	/** 启动（首轮在periodMs后触发）。幂等：运行中调用空转。stop后可再次start。 */
	public void start() {
		gate.lock();
		try {
			if (!shutdown)
				return;
			shutdown = false;
			rescheduleLocked();
		} finally {
			gate.unlock();
		}
	}

	/**
	 * 停止：关门（不再触发/续约）、cancel已排期的下一次、限时等待在飞一轮结束。幂等。
	 * 超预算或中断仅告警返回——残余的晚到一轮由调用方的资源容错兜住（墓碑化等）。
	 */
	public void stop() {
		ScheduledFuture<?> p;
		CompletableFuture<Void> f;
		gate.lock();
		try {
			if (shutdown)
				return;
			shutdown = true;
			p = pending;
			pending = null;
			f = inFlight;
			inFlight = null;
		} finally {
			gate.unlock();
		}
		if (p != null)
			p.cancel(false); // 普通ScheduledFuture：cancel不经TimerFuture锁，无ABBA
		if (f != null)
			awaitIdle(f);
	}

	/** 诊断/测试：当前是否有在飞的一轮（含已派发未开始）。 */
	public boolean isBusy() {
		return inFlight != null;
	}

	/** 诊断/测试：是否处于关门状态（未启动或已stop）。 */
	public boolean isShutdown() {
		gate.lock();
		try {
			return shutdown;
		} finally {
			gate.unlock();
		}
	}

	// 调度线程上的tick入口：只判关门+置在飞+派发，微秒级，绝不内联执行守护体。
	private void fire() {
		final CompletableFuture<Void> f;
		gate.lock();
		try {
			if (shutdown)
				return; // stop()已关门：本次触发作废
			pending = null; // 消费本次排期（链上恒至多一个pending）
			f = new CompletableFuture<>();
			inFlight = f;
		} finally {
			gate.unlock();
		}
		try {
			TaskSpec.ofAction(() -> runBody(f)).name(name).runNow();
		} catch (Throwable e) { // 派发失败（worker池关闭等）：守护链不得断裂，直接续约下轮
			gate.lock();
			try {
				if (inFlight == f)
					inFlight = null;
				rescheduleLocked();
			} finally {
				gate.unlock();
			}
			f.complete(null);
			logger.error("DaemonTimer {} dispatch failed, rescheduled", name, e);
		}
	}

	private void runBody(@NotNull CompletableFuture<Void> f) {
		try {
			body.run();
		} catch (Throwable e) { // 守护体异常不致命：记日志，链继续（executeCore还有一层兜底）
			logger.error("DaemonTimer {} body exception", name, e);
		} finally {
			gate.lock();
			try {
				if (inFlight == f)
					inFlight = null;
				rescheduleLocked();
			} finally {
				gate.unlock();
			}
			f.complete(null); // 锁外完成：stop的等待线程醒来后不再触碰gate
		}
	}

	// gate内。pending非null说明已有一条排期（如stop等待超预算逃逸后旧一轮才结束又逢restart），
	// 不再重复排期——保证任意时刻链上至多一个pending。
	private void rescheduleLocked() {
		if (shutdown || pending != null)
			return;
		try {
			pending = TaskSpec.ofAction(this::fire).name(name).scheduleNow(periodMs);
		} catch (Throwable e) { // 调度池已关（进程停机路径）：链断，记error
			logger.error("DaemonTimer {} reschedule failed, daemon terminated", name, e);
		}
	}

	private void awaitIdle(@NotNull CompletableFuture<Void> f) {
		var budget = Math.max(1, Task.defaultTimeout + 5_000);
		try {
			f.get(budget, TimeUnit.MILLISECONDS);
		} catch (TimeoutException e) {
			logger.warn("DaemonTimer {} body still running, skip waiting", name);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (ExecutionException e) { // 不会发生：inFlight只以complete(null)完成
			logger.error("DaemonTimer {} await error", name, e);
		}
	}
}
