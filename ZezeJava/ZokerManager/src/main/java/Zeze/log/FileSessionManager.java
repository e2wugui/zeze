package Zeze.log;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

	// 替换关闭串行执行：Session.close 含最长60s的CloseSession RPC等待，不能挡住HTTP响应线程；
	// 单线程串行也避免并发close互踩。守护线程不阻止进程退出。
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
	 * 请求三元组与现绑定一致（且未强制 changeSession）时复用会话；否则建新会话、
	 * 替换绑定并异步关闭旧会话（替换关闭语义：释放服务端查询句柄，避免替换出的
	 * 旧会话句柄滞留到进程结束）。
	 * 建新失败（目标服务器不可达等）直接上抛，旧绑定保持原样不受影响——下次请求可继续收敛。
	 */
	public static Object resolve(LogAgent logAgent, SocketAddress socketAddress, boolean changeSession,
								 boolean requestAll, String serverName, String logName) throws Exception {
		var bound = get(socketAddress);
		if (!changeSession && bound != null && bound.matches(requestAll, serverName, logName))
			return bound.session();
		var session = requestAll
				? logAgent.newSessionAll(logName)
				: logAgent.newSession(serverName, logName);
		var binding = requestAll
				? LogSessionBinding.allView(logName, session)
				: LogSessionBinding.server(serverName, logName, session);
		var old = put(socketAddress, binding);
		if (old != null && old.session() != session)
			closeExecutor.execute(() -> {
				try {
					if (old.session() instanceof AutoCloseable c)
						c.close();
				} catch (Throwable e) {
					logger.warn("close replaced session fail: {}", old, e);
				}
			});
		return session;
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
