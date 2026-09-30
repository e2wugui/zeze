package Zeze.log;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Services.Log4jQuery.Session;
import Zeze.Services.Log4jQuery.SessionAll;
import Zeze.Services.LogAgent;
import Zeze.Util.Func1;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Search/Browse 的会话身份表：键为客户端出口 IP（纯IP，无端口，IPv6 取规范host地址），
 * 值为 {@link LogSessionBinding}（会话对象 + (会话类型, serverName, logName, 条件指纹)
 * 绑定四元组）。HTTP处理器在Normal线程池并发取/存，必须是并发容器。
 *
 * <p>会话回执比对：复用会话前用 {@link LogSessionBinding#matches} 比对
 * 请求四元组（数据源三元组 + 查询条件指纹；allView 另比对服务器键集快照，
 * zoker-04；条件指纹 FND34 zokermanager-02——服务端仅 beginTime 有去重哨兵，
 * 条件变更必须关旧建新防静默漏早段匹配），不匹配（或
 * changeSession 强制重建）时关旧建新——客户端漏置 changeSession 不会串数据源
 * 也不会串条件，changeSession 只是"强制重建"提示符而非正确性前提。比对+重建的
 * 收口见 {@link #resolve}。
 * 死会话自愈（zoker-03）：复用命中但服务端已拒绝本会话（空闲回收后的 LogicError）时，
 * {@link #operateRecovering} 驱逐重建并重试一次，同 IP 同参数查询不再恒 system error。</p>
 *
 * <p><b>闲置回收</b>：绑定记录最后活跃时间（复用命中/新建时刷新，见
 * {@link LogSessionBinding#lastActiveNanos()}），resolve 入口低频惰性清扫（
 * {@link #sweepIdleBindings}）驱逐闲置超 TTL 的绑定并异步关闭会话——条目不再只增不减
 * （原形态：每源 IP 最后一个会话滞留到进程结束）。TTL 大于服务端
 * sessionIdleTimeoutMillis（默认 1h），被驱逐的只会是服务端早已回收的死会话，客户端按需
 * 重建无损。</p>
 *
 * <p><b>总量上限</b>：闲置回收只收敛时间维度（2h 内条目只增不减），无数量维度——HTTP 端口
 * 未认证，多源 IP 各查一次即可各占一条绑定，并在每台日志服务器上各持一个服务端查询会话
 * （服务端 Session/SessionAll 持文件 walker 与打开的日志文件句柄），慢速多 IP 即可撑开
 * 服务端文件句柄。故新键插入使总量超 {@link #MAX_BINDINGS} 时驱逐最久未活跃的绑定并异步
 * 关闭（{@link #put} / {@link #evictLeastActiveForCap}），同时收敛客户端内存与服务端句柄
 * 两处资源；驱逐与拒绝的取舍见后者。每 IP 无需上限：键即源 IP，每 IP 恒一条。</p>
 *
 * <p>已知限制：</p>
 * <ul>
 * <li><b>同IP互顶</b>：键仍是出口 IP，NAT 同出口多用户（或同用户在 Session/SessionAll
 * 视图间切换）交替查询会互相顶掉对方的会话——每次不匹配都重建，游标/过滤条件互相重置，
 * 结果正确但体验差。彻底解耦传输地址与会话身份需要显式会话令牌（响应带回
 * sessionToken、前端后续携带、IP 仅作审计），横跨 web 前端改造。总量上限驱逐（见
 * "总量上限"）与此同构：被逐 IP 下次查询重建，游标重置。</li>
 * <li><b>同IP并发拒绝（N03，FND28）</b>：同 IP 并发同参数请求曾共享同一会话使服务端游标被
 * 并发推进（跳页/重复/丢页且各自 success），现按在飞守卫（见 operateRecovering）快速拒绝
 * 并发者——可见 system error，重试即得串行结果；服务端游标的并发语义不动（留档）。</li>
 * <li><b>替换关闭的竞态</b>：换绑瞬间另一在飞请求
 * 可能正持有旧会话的 future（search/browse 最长 1 分钟），旧会话被关闭后该请求拿到
 * 旧数据源的完整结果或异常，不会拿到混合结果；彻底消除需要引用计数/版本化句柄，
 * 成本与收益不成比例。</li>
 * </ul>
 */
public class FileSessionManager {
	private static final Logger logger = LogManager.getLogger(FileSessionManager.class);

	private static final Map<String, LogSessionBinding> map = new ConcurrentHashMap<>(1000);

	// 绑定总量上限（数量维度收敛，见类注释"总量上限"）：1000 对齐建表时的容量提示
	// （ConcurrentHashMap 初始 1000），内部运维工具的真实用户面（数十~数百浏览器用户）
	// 远低于此，正常部署不可达、不可见；超限即多源 IP 异常撑面的形态。
	private static final int MAX_BINDINGS = 1000;

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

	// N03（FND28）：同源IP的search/browse在飞守卫（键与map同源，值=占用标记）。同IP并发同参数
	// 请求会共享同一绑定会话（resolve复用命中），服务端按会话推进游标——两个并发翻页各推一次
	// =跳页/重复/丢页且各自应答success；不同参数的并发本就会被resolve互顶重建（既有"同IP互顶"
	// 已知限制），按IP粒度守卫一致。条目不随清扫移除：移除与在飞请求的交错会分裂出两个flag放过
	// 并发；也不受 map 总量上限约束（守卫条目不可移除，且只含无会话无句柄的标记对象——残量是
	// 每历史源 IP 一个 AtomicBoolean，会话/句柄资源已由 map 上限收敛）。
	private static final ConcurrentHashMap<String, AtomicBoolean> inFlightByIp = new ConcurrentHashMap<>();

	/** 存入绑定记录，返回被替换的旧绑定（无则null）。旧会话的关闭见{@link #resolve}的收口。
	 * 新键插入使总量超 {@link #MAX_BINDINGS} 时驱逐最久未活跃的绑定（
	 * {@link #evictLeastActiveForCap}）——同键替换（换绑/重建/复用回写）不增总量，不触发。 */
	public static LogSessionBinding put(SocketAddress socketAddress, LogSessionBinding binding) {
		var ip = getIP(socketAddress);
		var old = map.put(ip, binding);
		if (old == null && map.size() > MAX_BINDINGS)
			evictLeastActiveForCap(ip);
		return old;
	}

	public static LogSessionBinding get(SocketAddress socketAddress) {
		return map.get(getIP(socketAddress));
	}

	/**
	 * 会话回执比对 + 替换关闭（SearchLogHandle/BrowseLogHandle 共用）：
	 * 请求四元组（会话类型, serverName, logName, 查询条件指纹——条件指纹见
	 * {@link LogSessionBinding}，FND34 zokermanager-02 起参与会话身份：条件变即
	 * 视同 changeSession 关旧建新，防"改条件再搜复用旧游标静默漏早段匹配"）与现
	 * 绑定一致（且未强制 changeSession、allView 键集未漂移）时复用会话并刷新最后
	 * 活跃时间（闲置回收见 {@link #sweepIdleBindings}）；否则建新会话、替换绑定并
	 * 异步关闭旧会话（替换关闭语义：释放服务端查询句柄，避免替换出的旧会话句柄
	 * 滞留到进程结束）。建新失败（目标服务器不可达等）直接上抛，旧绑定保持原样
	 * 不受影响——下次请求可继续收敛。
	 *
	 * <p>全服视图复用收敛比对（zoker-04 键集漂移 + FND30 zokermanager-02 成员集权威）：
	 * SessionAll 复用前以会话实际成员集比对当前注册表（allViewMembersConverged）——多余
	 * 成员（∈会话∉注册表：服务器摘除/键集漂移）视同 changeSession 走关旧建新缩容；缺失
	 * 成员（∈注册表∉会话：构造期跳过/扩容上台）不重建，由 SessionAll.operate 的缺册补员
	 * 自愈承担，避免持续故障期逐请求全量重建抖动。单服务器视图无集合语义，不比对。</p>
	 *
	 * <p>全服视图成员校验：newSessionAll 对不可达台逐台跳过，服务发现空集或注册表非空但全部
	 * 不可达（进程刚死/SM 分区/重启窗口，租约未过期）时构造出 0 成员会话并成功返回空结果——
	 * 空 BResult.Data 在前端语义是"查完无匹配"，与"一个查询目标都没有/全部不可达"不可混淆。
	 * 构造后按会话实际成员集校验，0 成员抛错且不入库绑定（毒化复用不存在），走 HTTP 层既有
	 * 错误承载（BaseResponse status=500，前端按 desc 报错），见下方重建路径的成员校验。</p>
	 */
	public static Object resolve(LogAgent logAgent, SocketAddress socketAddress, boolean changeSession,
								 boolean requestAll, String serverName, String logName,
								 String conditionKey) throws Exception {
		maybeSweepIdleBindings();
		var bound = get(socketAddress);
		// 复用前置校验——绑定的查询目标必须仍在注册表：单服务器视图下服务器被 SM 摘除后
		// matches 四元组恒命中、死绑定恒复用，Session 内对已摘册名的失败每页必现直到 2h
		// 闲置清扫——摘册即视同 changeSession 走重建（重建对未注册名显式失败）。全服视图
		// 的摘册收敛由 allViewMembersConverged 承担。
		// 校验按请求可达性惰性求值（matches 之后）：matches 已短路视图不一致，单服绑定的
		// contains(serverName) 求值时请求必为单服视图（serverName 非 null，无 NPE 面）；
		// 全服视图请求（serverName=null）对单服绑定走关旧建新，不触碰 contains(null)。
		if (!changeSession && bound != null && bound.matches(requestAll, serverName, logName, conditionKey)
				&& reuseConverged(logAgent, bound, serverName)) {
			// 复用命中刷新活跃时间：条件 replace 只在条目仍是同一绑定时生效——并发 resolve
			// 已换绑（changeSession/参数变化/键集漂移重建）时不回写旧绑定覆盖新会话；本次返回的旧会话
			// 由换绑方的替换关闭收口（既有"替换关闭的竞态"裁量）。
			map.replace(getIP(socketAddress), bound, bound.touched(System.nanoTime()));
			return bound.session();
		}
		var session = requestAll
				? logAgent.newSessionAll(logName)
				: logAgent.newSession(serverName, logName);
		// 全服视图会话成员校验（FND30 zokermanager-01）：newSessionAll 对不可达台逐台 warn 跳过，
		// 注册表非空但全部不可达（进程刚死/网络分区/重启窗口，SM 租约未过期）时构造出 0 成员会话
		// ——operate 零 future 返回空 BResult.Data，HTTP 层包装成空成功："全部查询目标不可达"被
		// 误分类为"查完无匹配"；且 0 成员会话入库绑定后注册表键集不漂移即恒复用，故障恢复后同 IP
		// 仍恒空结果。以构造出的实际成员集为权威单点校验（空注册表→0 成员→同一抛错，原"零服务器
		// 卫语句"并入此处），未 put 即抛，毒化绑定不存在；0 成员无服务端句柄可泄漏，无需收尾。
		// 单服务器视图不查：newSession 对不可达/未知服务器本就显式失败。
		if (requestAll && ((SessionAll) session).memberNames().isEmpty()) {
			var registered = logAgent.getLogServers().size();
			logger.warn("all-servers view rejected: no reachable log server. registered={}, members=0, logName={}",
					registered, logName);
			throw new IllegalStateException("no reachable log server for all-servers view"
					+ " (registered=" + registered + ", members=0)");
		}
		var binding = requestAll
				? LogSessionBinding.allView(logName, conditionKey, session, membersKeyOf(((SessionAll) session).memberNames()))
				: LogSessionBinding.server(serverName, logName, conditionKey, session);
		var old = put(socketAddress, binding);
		if (old != null && old.session() != session)
			closeAsync(old);
		return session;
	}

	/**
	 * 全服视图复用收敛判定（FND30 zokermanager-01/02）：会话为 SessionAll 时以其实际成员集
	 * 为唯一权威——多余成员（∈会话∉注册表：服务器摘除/下线）不收敛，视同 changeSession 走
	 * 关旧建新缩容；缺失成员（∈注册表∉会话：构造期跳过/扩容上台）不重建，由
	 * SessionAll.operate 的缺册补员（reconcileMissingMembers）自愈承担——持续故障期缺员若
	 * 逐请求全量重建，每页查询都重付全服建会话成本且游标归零抖动。会话对象非 SessionAll
	 * （直构测试形态，无成员集可查）退回快照等值比对（空快照恒不匹配，原语义）。
	 */
	static boolean allViewMembersConverged(LogAgent logAgent, LogSessionBinding bound) {
		if (bound.session() instanceof SessionAll sessionAll)
			return allViewMembersConverged(sessionAll.memberNames(), logAgent.getLogServers());
		return bound.allServersKey().equals(allServersKeyOf(logAgent));
	}

	/** 复用前置校验单点（按绑定视图分支）：单服绑定比对 serverName 仍在注册表（matches 已
	 * 短路视图不一致——求值时请求必为单服视图、serverName 非 null，contains 无 NPE 面）；
	 * 全服绑定比对成员集收敛。changeSession=true 不进复用路径，不参与本校验。 */
	private static boolean reuseConverged(LogAgent logAgent, LogSessionBinding bound, String serverName) {
		return bound.all()
				? allViewMembersConverged(logAgent, bound)
				: logAgent.getLogServers().contains(serverName);
	}

	/**
	 * 全服视图收敛判定（纯函数，直测面）：会话成员集为空恒不收敛（0 成员会话不可复用——重建
	 * 路径的成员校验会显式失败）；否则注册表包含全部会话成员（无多余）即收敛——缺失成员由
	 * 补员承担，不算不收敛。
	 */
	static boolean allViewMembersConverged(Set<String> memberNames, Set<String> registeredServers) {
		return !memberNames.isEmpty() && registeredServers.containsAll(memberNames);
	}

	/** 当前日志服务器键集快照（排序 join——集合无序，比对须与顺序无关；键集小，构建开销可忽略）。 */
	private static String allServersKeyOf(LogAgent logAgent) {
		return String.join(",", new TreeSet<>(logAgent.getLogServers()));
	}

	/** 会话实际成员集键串（排序 join，与 {@link #allServersKeyOf} 同形）：全服视图绑定快照
	 * 记录会话构成而非注册表键集（FND30 zokermanager-02）——注册表键集不可证明会话构成
	 * （构造期跳过/注册竞态窗口）；快照仅供会话非 SessionAll 的直构形态回退比对。 */
	private static String membersKeyOf(Set<String> memberNames) {
		return String.join(",", new TreeSet<>(memberNames));
	}

	/**
	 * resolve + operate 的会话级错误自愈包装（zoker-03）：首次 operate 抛
	 * {@link Session.SessionLevelException}（服务端已拒绝本会话——典型：闲置被回收后的
	 * LogicError）时：以 changeSession 语义驱逐重建（resolve 的关旧建新路径，旧会话走
	 * closeExecutor 关闭）→ 同参数重建 → 重试一次；重试仍失败原样上抛（只一层，防循环）。
	 * 非会话级错误（网络/超时；参数级拒绝 {@link Session.InvalidArgumentException}——参数
	 * 错误拆会话只会重蹈覆辙）原样上抛不重建——重建会白白丢弃仍有效的会话与游标。
	 * 重建后游标归零：continuation 请求（reset=false）的重试返回首页数据——死会话本无
	 * 正确续页可言，首页数据优于永久报错。
	 * 外层套 N03（FND28）的同IP在飞守卫：并发共享会话使服务端游标被并发推进（跳页/重复/
	 * 丢页且各自success），并发者快速失败重试，不排队。
	 */
	public static <T> T operateRecovering(LogAgent logAgent, SocketAddress socketAddress,
										  boolean changeSession, boolean requestAll,
										  String serverName, String logName, String conditionKey,
										  Func1<Object, T> operate) throws Exception {
		// N03（FND28）：同IP同时只允许一个search/browse在飞（守卫 rationale 见inFlightByIp注释）；
		// 并发者快速失败（可见system error+warn）不排队——慢查询（最长60s）下排队只会堆积放大。
		var ip = getIP(socketAddress);
		var inFlight = inFlightByIp.computeIfAbsent(ip, __ -> new AtomicBoolean());
		if (!inFlight.compareAndSet(false, true)) {
			logger.warn("concurrent search/browse rejected: same client session in flight"
					+ " (server session cursor would advance concurrently -> skipped/duplicated/lost pages). ip={}", ip);
			throw new IllegalStateException("concurrent search/browse on same session, retry after current request completes");
		}
		try {
			var session = resolve(logAgent, socketAddress, changeSession, requestAll, serverName, logName,
					conditionKey);
			try {
				return operate.call(session);
			} catch (Session.SessionLevelException e) {
				var fresh = resolve(logAgent, socketAddress, true, requestAll, serverName, logName,
						conditionKey);
				return operate.call(fresh);
			}
		} finally {
			inFlight.set(false);
		}
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

	/**
	 * 总量超限驱逐（put 触发）：挑最久未活跃（lastActiveNanos 最小）且当前无在飞查询的绑定，
	 * 条件移除（remove(key,binding) 防误关：并发已换绑的条目不是迭代读到的实例，同
	 * {@link #sweepIdleBindings}）并异步关闭其会话。取舍：
	 * <ul>
	 * <li><b>驱逐而非拒绝</b>：拒绝（直接报错）下，垃圾源 IP 各查一次即可占满全部名额，
	 * 新用户直到 TTL 清扫（最长 2h）都进不来——供给被最廉价的攻击独占；驱逐下新 IP 顶掉
	 * 最旧绑定，查询路径保持可用。被驱逐者下次请求按需重建（会话本就可安全重建，见类注释
	 * "同IP互顶"），且驱逐即关闭，同时收敛客户端内存与服务端查询句柄。</li>
	 * <li><b>跳过在飞</b>（inFlightByIp 为 true 的 IP）：有非在飞候选时不惊扰正持有会话
	 * future 的在飞请求（关闭在飞会话安全——见"替换关闭的竞态"，但该请求只能拿到异常）；
	 * 全部在飞时不驱逐，瞬时超限自愈于下次插入。</li>
	 * <li>O(n) 选最旧仅在超限的新键插入路径执行（n≤上限+并发过冲），遍历微秒级，不挡查询。</li>
	 * </ul>
	 */
	private static void evictLeastActiveForCap(String excludeIp) {
		Map.Entry<String, LogSessionBinding> victim = null;
		for (var e : map.entrySet()) {
			if (e.getKey().equals(excludeIp))
				continue; // 刚插入的本键时间戳最新，显式排除自证（并发插入时间戳相近时的自保）
			var inFlight = inFlightByIp.get(e.getKey());
			if (inFlight != null && inFlight.get())
				continue;
			if (victim == null || e.getValue().lastActiveNanos() < victim.getValue().lastActiveNanos())
				victim = e;
		}
		if (victim != null && map.remove(victim.getKey(), victim.getValue())) {
			logger.warn("session binding evicted: bindings over cap {}. evicted ip={}", MAX_BINDINGS, victim.getKey());
			closeAsync(victim.getValue());
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
