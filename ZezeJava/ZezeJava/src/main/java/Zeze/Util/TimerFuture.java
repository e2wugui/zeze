package Zeze.Util;

import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 周期任务句柄（{@link Task#schedulePeriodCore} 创建，TaskSpec.schedulePeriodNow 返回）。
 * 目的：补 JDK ScheduledFuture.cancel 的缺口——它只阻断后续触发，对已开始的当前轮既不阻止
 * 也不可等待，调用方"cancel 后即拆资源"会与在飞一轮竞态。
 * 机制：任务体在本对象锁内执行并复查取消标志，cancel 取同一把锁，因此
 * ①与"检查→执行"互斥：cancel 返回后不会再启动新的一轮；
 * ②隐式 join 在飞的一轮：返回即可安全拆除任务体访问的资源。
 * 代价：cancel 成为阻塞原语，受 {@link #cancel} 的 ABBA 调用约束；等锁超过任务自身的看门狗
 * 预算+{@link #CANCEL_HANG_MARGIN_MS} 时发出双栈告警（无声挂死→有声，契约不变）。
 */
public class TimerFuture<V> extends ReentrantLock implements ScheduledFuture<V> {
	// 告警阈值的余量：诊断线程按30s周期扫描超时（ThreadDiagnosable.startDiagnose(30_000)），
	// 到达阈值即"合法长体应已被看门狗打断仍未结束"——告警近乎确诊而非噪音。volatile：测试可调短。
	public static volatile long CANCEL_HANG_MARGIN_MS = 35_000;

	private static final @NotNull Logger logger = LogManager.getLogger(TimerFuture.class);

	private final @Nullable String name; // 日志名（body.logName(name)），仅用于告警定位
	private final long timeoutMs; // 任务体看门狗预算，与余量共同构成挂死告警阈值

	private ScheduledFuture<V> future;
	// volatile: isCancelled() 在任务线程中无锁轮询，需与 cancel() 的写入保持可见性
	private volatile boolean canceled;

	TimerFuture(@Nullable String name, long timeoutMs) {
		this.name = name;
		this.timeoutMs = timeoutMs;
	}

	@SuppressWarnings("unchecked")
	public void setFuture(@NotNull ScheduledFuture<?> future) {
		this.future = (ScheduledFuture<V>)future;
	}

	/**
	 * 取消并 join：取本对象锁，阻塞至在飞一轮任务体结束；返回后不再启动新的一轮
	 * （mayInterruptIfRunning 只作用于底层 future，不中断任务体）。
	 * 【调用约束】不得持有任务体执行期间可能获取的任何锁，否则与在飞任务体互等（ABBA）永久挂起；
	 * 需与停机门禁互斥时，锁内只置关门标志并捕获句柄，释放锁后再 cancel。周期守护的停机
	 * 请优先用 DaemonTimer——它把"关门→cancel→限时等待在飞"内聚为组件，cancel对象为
	 * 普通JDK ScheduledFuture，本约束对其不适用。
	 * <p>
	 * 挂死告警：先 tryLock 等待 timeoutMs+{@link #CANCEL_HANG_MARGIN_MS}——到达即超出了任务体
	 * 自身的看门狗预算，大概率是上述ABBA约束被违反或任务体不可中断阻塞——未获取则告警
	 * 持锁线程与当前线程双栈后继续无界等待（契约不变，仅把无声挂死变成有声）。
	 * 等待期间被中断时保留中断标志并直接进入无界等待，不告警（"等满阈值"的前提不成立）。
	 */
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		boolean acquired = false;
		boolean interrupted = false;
		try {
			acquired = tryLock(timeoutMs + CANCEL_HANG_MARGIN_MS, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt(); // 保留标志：与原先非中断lock()的行为一致
			interrupted = true;
		}
		if (!acquired) {
			if (!interrupted)
				warnHang();
			lock(); // 契约不变：无界等到获取为止（!acquired时此处才持锁一次，避免重入计数多一）
		}
		try {
			canceled = true;
			return future.cancel(mayInterruptIfRunning);
		} finally {
			unlock();
		}
	}

	// 挂死告警：dump持锁线程（在飞任务体线程）与当前取消线程的双栈。诊断性质，绝不向外抛；
	// 竞态下owner可能已释放（null），措辞用"可能"对冲。
	private void warnHang() {
		try {
			var owner = getOwner();
			var sb = new StringBuilder();
			if (owner != null) {
				sb.append("owner thread '").append(owner.getName()).append("' stack:\n");
				ThreadDiagnosable.formatStackTrace(owner.getStackTrace(), sb);
			} else
				sb.append("owner already released\n");
			sb.append("cancel thread '").append(Thread.currentThread().getName()).append("' stack:\n");
			ThreadDiagnosable.formatStackTrace(Thread.currentThread().getStackTrace(), sb);
			logger.warn("TimerFuture({}) cancel blocked > {}ms acquiring lock, possible ABBA "
					+ "(caller holds a lock the body needs) or uninterruptible body;\n{}", name,
					timeoutMs + CANCEL_HANG_MARGIN_MS, sb);
		} catch (Throwable e) { // 诊断不得弄坏cancel本体；logger自身故障时的最后出口
			//noinspection CallToPrintStackTrace
			e.printStackTrace();
		}
	}

	@Override
	public long getDelay(@NotNull TimeUnit unit) {
		return future.getDelay(unit);
	}

	@Override
	public int compareTo(@NotNull Delayed o) {
		return future.compareTo(o);
	}

	@Override
	public boolean isCancelled() {
		return canceled; // future.isCancelled(); // 调用此方法可能future字段还没赋值
	}

	@Override
	public boolean isDone() {
		return future.isDone();
	}

	@Override
	public V get() throws InterruptedException, ExecutionException {
		return future.get();
	}

	@Override
	public V get(long timeout, @NotNull TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		return future.get(timeout, unit);
	}
}
