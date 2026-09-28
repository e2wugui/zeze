package Zeze.log;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Services.LogAgent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Search/Browse 的会话身份表：键为客户端出口 IP（纯IP，无端口，IPv6 取规范host地址），
 * 值为 {@link LogSessionBinding}（会话对象 + (会话类型, serverName, logName) 绑定三元组）。
 * HTTP处理器在Normal线程池并发取/存，必须是并发容器。
 *
 * <p>会话回执比对：复用会话前用 {@link LogSessionBinding#matches} 比对
 * 请求三元组与绑定记录，不匹配（或 changeSession 强制重建）时关旧建新——客户端漏置
 * changeSession 不会串数据源，changeSession 只是"强制重建"提示符而非正确性前提。
 * 比对+重建的收口见 {@link #resolve}。</p>
 *
 * <p><b>闲置回收</b>：绑定记录最后活跃时间（复用命中/新建时刷新，见
 * {@link LogSessionBinding#lastActiveNanos()}），resolve 入口低频惰性清扫（
 * {@link #sweepIdleBindings}）驱逐闲置超 TTL 的绑定并异步关闭会话——条目不再只增不减
 * （原形态：每源 IP 最后一个会话滞留到进程结束）。TTL 大于服务端
 * sessionIdleTimeoutMillis（默认 1h），被驱逐的只会是服务端早已回收的死会话，客户端按需
 * 重建无损。</p>
 *
 * <p>已知限制：</p>
 * <ul>
 * <li><b>同IP互顶</b>：键仍是出口 IP，NAT 同出口多用户（或同用户在 Session/SessionAll
 * 视图间切换）交替查询会互相顶掉对方的会话——每次不匹配都重建，游标/过滤条件互相重置，
 * 结果正确但体验差。彻底解耦传输地址与会话身份需要显式会话令牌（响应带回
 * sessionToken、前端后续携带、IP 仅作审计），横跨 web 前端改造。</li>
 * <li><b>替换关闭的竞态</b>：换绑瞬间另一在飞请求
 * 可能正持有旧会话的 future（search/browse 最长 1 分钟），旧会话被关闭后该请求拿到
 * 旧数据源的完整结果或异常，不会拿到混合结果；彻底消除需要引用计数/版本化句柄，
 * 成本与收益不成比例。</li>
 * </ul>
 */
public class FileSessionManager {
	private static final Logger logger = LogManager.getLogger(FileSessionManager.class);

	private static final Map<String, LogSessionBinding> map = new ConcurrentHashMap<>(1000);

	// 绑定闲置TTL：须大于服务端 sessionIdleTimeoutMillis（LogServiceConf 默认 1h）——客户端 TTL
	// 大于服务端空闲回收，驱逐的只会是服务端早已回收的死会话（客户端按需重建无损：下次请求
	// 经 matches 比对重建，游标等状态本就随重建重置）。取 2h=默认服务端回收窗口的两倍。
	private static final long IDLE_TTL_NANOS = Duration.ofHours(2).toNanos();
	// 惰性清扫最小间隔：resolve 入口节流，不足间隔直接跳过；条目以源 IP 为界、遍历微秒级，
	// 清扫与查询同线程，会话关闭走 closeExecutor 不挡查询线程。
	private static final long SWEEP_INTERVAL_NANOS = Duration.ofMinutes(10).toNanos();
	// 上次清扫时刻（nanoTime 单调时基）：CAS 到点只放行一个线程，避免并发重复清扫。
	private static final AtomicLong lastSweepNanos = new AtomicLong();

	// 替换/闲置驱逐的关闭串行执行：Session.close 含最长60s的CloseSession RPC等待，不能挡住
	// HTTP响应线程；单线程串行也避免并发close互踩。守护线程不阻止进程退出。
	private static final ExecutorService closeExecutor = Executors.newSingleThreadExecutor(r -> {
		var t = new Thread(r, "zoker-file-session-close");
		t.setDaemon(true);
		return t;
	});

	/** 存入绑定记录，返回被替换的旧绑定（无则null）。旧会话的关闭见{@link #resolve}的收口。 */
	public static LogSessionBinding put(SocketAddress socketAddress, LogSessionBinding binding) {
		return map.put(getIP(socketAddress), binding);
	}

	public static LogSessionBinding get(SocketAddress socketAddress) {
		return map.get(getIP(socketAddress));
	}

	/**
	 * 会话回执比对 + 替换关闭（SearchLogHandle/BrowseLogHandle 共用）：
	 * 请求三元组与现绑定一致（且未强制 changeSession）时复用会话并刷新最后活跃时间
	 * （闲置回收见 {@link #sweepIdleBindings}）；否则建新会话、替换绑定并异步关闭旧会话
	 * （替换关闭语义：释放服务端查询句柄，避免替换出的旧会话句柄滞留到进程结束）。
	 * 建新失败（目标服务器不可达等）直接上抛，旧绑定保持原样不受影响——下次请求可继续收敛。
	 */
	public static Object resolve(LogAgent logAgent, SocketAddress socketAddress, boolean changeSession,
								 boolean requestAll, String serverName, String logName) throws Exception {
		maybeSweepIdleBindings();
		var bound = get(socketAddress);
		if (!changeSession && bound != null && bound.matches(requestAll, serverName, logName)) {
			// 复用命中刷新活跃时间：条件 replace 只在条目仍是同一绑定时生效——并发 resolve
			// 已换绑（changeSession/参数变化重建）时不回写旧绑定覆盖新会话；本次返回的旧会话
			// 由换绑方的替换关闭收口（既有"替换关闭的竞态"裁量）。
			map.replace(getIP(socketAddress), bound, bound.touched(System.nanoTime()));
			return bound.session();
		}
		var session = requestAll
				? logAgent.newSessionAll(logName)
				: logAgent.newSession(serverName, logName);
		var binding = requestAll
				? LogSessionBinding.allView(logName, session)
				: LogSessionBinding.server(serverName, logName, session);
		var old = put(socketAddress, binding);
		if (old != null && old.session() != session)
			closeAsync(old);
		return session;
	}

	/** 清扫节流：距上次清扫不足 {@link #SWEEP_INTERVAL_NANOS} 直接跳过；CAS 到点只放行一个线程。 */
	private static void maybeSweepIdleBindings() {
		var now = System.nanoTime();
		var last = lastSweepNanos.get();
		if (now - last < SWEEP_INTERVAL_NANOS)
			return;
		if (lastSweepNanos.compareAndSet(last, now))
			sweepIdleBindings(now);
	}

	/**
	 * 驱逐闲置超 {@link #IDLE_TTL_NANOS} 的绑定并异步关闭其会话。条件移除
	 * （remove(key, binding)）防误关：并发 resolve 已换绑时条目非读到的实例，不摘新绑定
	 * （新会话有自己的生命周期，由后续替换/清扫管理）。极小竞态：判闲置到移除之间恰被
	 * 复用命中的绑定仍会被逐，其关闭与在飞请求的交错同"替换关闭的竞态"（拿到完整结果或
	 * 异常，不混合），重试即重建，无损。
	 */
	private static void sweepIdleBindings(long nowNanos) {
		for (var e : map.entrySet()) {
			var binding = e.getValue();
			if (nowNanos - binding.lastActiveNanos() < IDLE_TTL_NANOS)
				continue;
			if (map.remove(e.getKey(), binding))
				closeAsync(binding);
		}
	}

	/** 会话关闭统一走 closeExecutor：Session.close 含最长 60s 的 CloseSession RPC 等待，不得挡查询线程。 */
	private static void closeAsync(LogSessionBinding binding) {
		closeExecutor.execute(() -> {
			try {
				if (binding.session() instanceof AutoCloseable c)
					c.close();
			} catch (Throwable e) {
				logger.warn("close replaced/idle session fail: {}", binding, e);
			}
		});
	}

	private static String getIP(SocketAddress socketAddress) {
		// InetSocketAddress.toString()对IPv6形如"/[0:0:0:0:0:0:0:1]:5678"，字符串切分会把
		// 所有IPv6客户端坍缩成同一个键；取规范host地址。
		if (socketAddress instanceof InetSocketAddress inet) {
			var address = inet.getAddress();
			return address != null ? address.getHostAddress() : inet.getHostString();
		}
		return socketAddress.toString();
	}
}
