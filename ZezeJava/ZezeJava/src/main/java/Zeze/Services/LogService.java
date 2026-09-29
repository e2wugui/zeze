package Zeze.Services;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Application;
import Zeze.Builtin.LogService.BCondition;
import Zeze.Builtin.LogService.BLog;
import Zeze.Builtin.LogService.Browse;
import Zeze.Builtin.LogService.CloseSession;
import Zeze.Builtin.LogService.NewSession;
import Zeze.Builtin.LogService.Query;
import Zeze.Builtin.LogService.Search;
import Zeze.Config;
import Zeze.Services.Log4jQuery.Log4jFileManager;
import Zeze.Services.Log4jQuery.Log4jLog;
import Zeze.Services.Log4jQuery.LogServiceConf;
import Zeze.Services.Log4jQuery.Server;
import Zeze.Services.Log4jQuery.ServerUserState;
import Zeze.Services.Log4jQuery.handler.QueryHandlerManager;
import Zeze.Services.ServiceManager.AbstractAgent;
import Zeze.Services.ServiceManager.Agent;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Transaction.Procedure;
import Zeze.Services.Log4jQuery.Log4jSession;
import org.jetbrains.annotations.NotNull;

import static Zeze.Util.Args.requireValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Log4jQuery 服务入口：按 LogConf 管理各份日志的 Log4jFileManager，处理 NewSession/Search/Browse/
 * CloseSession/Query 协议，并向 ServiceManager 注册服务。
 */
public class LogService extends AbstractLogService {
	private static final @NotNull Logger logger = LogManager.getLogger(LogService.class);

	/**
	 * Search/Browse 的参数级拒绝码（区别于死会话的 {@link Procedure#LogicError}）：非法
	 * containsType、words/pattern 双空、offsetFactor∉[0,1)。两类拒绝共用 LogicError 时，
	 * 客户端只能按非零码整体分诊——参数错误被当成会话级死亡触发整组会话拆建重试后仍恒失败。
	 * 客户端 {@code Session.checked} 依本码抛参数级异常（不拆会话，直接向调用方报参数错误）。
	 * 取值避开框架保留码（Procedure 的 -1..-18 小负数段）。
	 */
	public static final long INVALID_ARGUMENT = -100;

	private final AtomicLong sidSeed = new AtomicLong();
	private final Config conf;
	private final LogServiceConf logConfs;
	private final AbstractAgent serviceManager;
	private final Server server;
	private final String passiveIp;
	private final int passivePort;
	private final ConcurrentHashMap<String, Log4jFileManager> logManagers = new ConcurrentHashMap<>();

	public static void main(String[] args) throws Exception {
		var configXml = "zeze.xml";
		for (var i = 0; i < args.length; ++i) {
			if (args[i].equals("-conf"))
				configXml = requireValue(args, ++i, "-conf");
		}
		var config = Config.load(configXml);
		var logService = new LogService(config);
		logService.start();
		try {
			synchronized (Thread.currentThread()) {
				Thread.currentThread().wait();
			}
		} finally {
			logService.stop();
		}
	}

	public ConcurrentHashMap<String, Log4jFileManager> getLogManagers() {
		return logManagers;
	}

	public Log4jFileManager getLogManager(String logName) {
		return logManagers.get(logName);
	}

	public LogService(Config config) throws Exception {
		this.conf = config;
		logConfs = new LogServiceConf();
		config.parseCustomize(logConfs);

		this.server = new Server(this, config);
		var kv = server.getOnePassiveAddress();
		passiveIp = kv.getKey();
		passivePort = kv.getValue();
		buildLogManagers(logConfs, logManagers);
		logConfs.formatServiceIdentity(conf.getServerId(), passiveIp, passivePort);
		serviceManager = Application.createServiceManager(conf, "LogServiceServer");
		RegisterProtocols(server);
	}

	/**
	 * 逐LogConf构造manager：多manager中途失败时回收前面已成功者的detector线程与索引定时器
	 * （对齐单manager构造内回收形态）——嵌入宿主进程时不泄漏；standalone main随进程退出无害。
	 * 中途失败的manager自身在其构造内回收，未入表无需再管。
	 */
	private static void buildLogManagers(LogServiceConf logConfs,
										 ConcurrentHashMap<String, Log4jFileManager> logManagers) throws Exception {
		try {
			for (var logConf : logConfs.getLogConfs().values()) {
				logManagers.put(logConf.getName(), new Log4jFileManager(logConf));
			}
		} catch (Exception e) {
			for (var manager : logManagers.values())
				manager.stop();
			logManagers.clear();
			throw e;
		}
	}

	public void start() throws Exception {
		server.start();
		var serviceManagerConf = conf.getServiceConf(Agent.defaultServiceName);
		// raft版SM的地址来自raftXml而非ServiceConf节点，按Agent服务名查serviceConfMap必为null，
		// 门槛若仅凭serviceConfMap非空判断会跳过serviceManager.start()，raft部署下日志服务静默失效（对齐Application.start）。
		var isRaftServiceManager = "raft".equals(conf.getServiceManager());
		if ((serviceManagerConf != null || isRaftServiceManager) && serviceManager != null) {
			serviceManager.start();
			try {
				serviceManager.waitReady();
			} catch (Exception ex) {
				// raft 版第一次等待由于选择leader原因肯定会失败一次。
				//noinspection ConstantValue
				if (ex instanceof InterruptedException)
					Thread.currentThread().interrupt(); // 恢复被底层清除的中断标志
				serviceManager.waitReady();
			}
			serviceManager.registerService(new BServiceInfo(
					"Zeze.LogService", logConfs.serviceIdentity, 0, passiveIp, passivePort));
		}
	}

	public void stop() throws Exception {
		this.server.stop();
		for (var manager : logManagers.values())
			manager.stop(); // 停掉日志文件监视线程与索引定时器
		if (serviceManager != null)
			serviceManager.close();
	}

	@Override
	protected long ProcessCloseSessionRequest(CloseSession r) throws Exception {
		var agent = (ServerUserState)r.getSender().getUserState();
		agent.closeLogSession(r.Argument.getId());
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessNewSessionRequest(NewSession r) throws Exception {
		// 未知logName直接返回错误码；不校验的话getLogManager返回null，walker构造Objects.requireNonNull抛NPE。
		if (null == getLogManager(r.Argument.getLogName()))
			return Procedure.LogicError;
		var agent = (ServerUserState)r.getSender().getUserState();
		// 顺带惰性清理空闲超龄会话：新建会话时不持任何会话锁，无死锁面。
		agent.cleanIdleLogSessions(logConfs.sessionIdleTimeoutMillis);
		r.Result.setId(sidSeed.incrementAndGet());
		agent.newLogSession(r.Argument.getLogName(), r.Result.getId());
		r.SendResult();
		return 0;
	}

	/**
	 * Search/Browse 参数校验（入口单点）：containsType 枚举、words/pattern 双空、browse 的
	 * offsetFactor∈[0,1)——offsetFactor 负值曾静默退化（上下文行逐条 poll 掉，browse 变无上下文
	 * 过滤搜索），≥1 在深路径抛异常转 Exception 码。参数级拒绝统一回
	 * {@link #INVALID_ARGUMENT}，与死会话的 LogicError 分离，客户端按码分诊不拆会话。
	 * offsetFactor 传 null 表示 search（无该参数）。
	 */
	private static long validateArgument(BCondition.Data condition, Float offsetFactor) {
		var containsType = condition.getContainsType();
		if (containsType != BCondition.ContainsAll && containsType != BCondition.ContainsAny
				&& containsType != BCondition.ContainsNone)
			return INVALID_ARGUMENT;
		var pattern = condition.getPattern();
		if (condition.getWords().isEmpty() && (pattern == null || pattern.isEmpty()))
			return INVALID_ARGUMENT;
		if (offsetFactor != null && !(offsetFactor >= 0f && offsetFactor < 1f)) // NaN 落 false 同拒
			return INVALID_ARGUMENT;
		return Procedure.Success;
	}

	@Override
	protected long ProcessBrowseRequest(Browse r) throws Exception {
		// 参数级拒绝入口单点（见 INVALID_ARGUMENT）：在触碰会话前校验，参数错误与死会话分诊。
		var argCode = validateArgument(r.Argument.getCondition(), r.Argument.getOffsetFactor());
		if (argCode != Procedure.Success)
			return argCode;
		var agent = (ServerUserState)r.getSender().getUserState();
		var logSession = agent.getLogSession(r.Argument.getId());
		if (null == logSession)
			return Procedure.LogicError;
		var result = new LinkedList<Log4jLog>();

		// limit clamp：协议字段是客户端可控的裸int，超出服务端上限按上限执行，clamp记一条可辨识日志。
		var limit = Log4jSession.clampLimit(r.Argument.getLimit());
		if (limit != r.Argument.getLimit())
			logger.info("browse limit clamped: {} -> {}", r.Argument.getLimit(), limit);

		boolean remain;
		// Log4jFileWalker非线程安全（currentIndex/current无锁），同sid并发Browse/Search会损坏游标并泄漏文件句柄，按会话串行化。
		synchronized (logSession) {
			logSession.touchActive(); // 查询即活跃：进锁首行刷新，惰性清理据此判定空闲。
			if (!r.Argument.getCondition().getWords().isEmpty()) {
				if (r.Argument.isReset())
					logSession.reset();
				remain = logSession.browseContains(result,
						r.Argument.getCondition().getBeginTime(), r.Argument.getCondition().getEndTime(),
						r.Argument.getCondition().getWords(), r.Argument.getCondition().getContainsType(),
						limit, r.Argument.getOffsetFactor());
			} else if (!r.Argument.getCondition().getPattern().isEmpty()) {
				if (r.Argument.isReset())
					logSession.reset();
				remain = logSession.browseRegex(result,
						r.Argument.getCondition().getBeginTime(), r.Argument.getCondition().getEndTime(),
						r.Argument.getCondition().getPattern(),
						limit, r.Argument.getOffsetFactor());
			} else
				return INVALID_ARGUMENT; // 空条件参数级拒绝（入口单点已拦，此处防御重复）；精确码应答而非异常通道
		}

		r.Result.setRemain(remain);
		for (var log : result)
			r.Result.getLogs().add(new BLog.Data(log.getTime(), log.getLog()));
		r.SendResult();
		// 顺带惰性清理：锁外调用（不持本会话锁再去获取其他会话锁，避免锁序环），响应已发出不增加时延。
		agent.cleanIdleLogSessions(logConfs.sessionIdleTimeoutMillis);
		return 0;
	}

	@Override
	protected long ProcessSearchRequest(Search r) throws Exception {
		// 参数级拒绝入口单点（见 INVALID_ARGUMENT）：在触碰会话前校验，参数错误与死会话分诊。
		var argCode = validateArgument(r.Argument.getCondition(), null);
		if (argCode != Procedure.Success)
			return argCode;
		var agent = (ServerUserState)r.getSender().getUserState();
		var logSession = agent.getLogSession(r.Argument.getId());
		if (null == logSession)
			return Procedure.LogicError;
		var result = new ArrayList<Log4jLog>();

		// limit clamp：同Browse。
		var limit = Log4jSession.clampLimit(r.Argument.getLimit());
		if (limit != r.Argument.getLimit())
			logger.info("search limit clamped: {} -> {}", r.Argument.getLimit(), limit);

		boolean remain;
		// 同Browse：walker非线程安全，按会话串行化。
		synchronized (logSession) {
			logSession.touchActive(); // 查询即活跃：进锁首行刷新。
			if (!r.Argument.getCondition().getWords().isEmpty()) {
				if (r.Argument.isReset())
					logSession.reset();
				remain = logSession.searchContains(result,
						r.Argument.getCondition().getBeginTime(), r.Argument.getCondition().getEndTime(),
						r.Argument.getCondition().getWords(), r.Argument.getCondition().getContainsType(),
						limit);
			} else if (!r.Argument.getCondition().getPattern().isEmpty()) {
				if (r.Argument.isReset())
					logSession.reset();
				remain = logSession.searchRegex(result,
						r.Argument.getCondition().getBeginTime(), r.Argument.getCondition().getEndTime(),
						r.Argument.getCondition().getPattern(),
						limit);
			} else
				return INVALID_ARGUMENT; // 空条件参数级拒绝（入口单点已拦，此处防御重复）；精确码应答而非异常通道
		}

		r.Result.setRemain(remain);
		for (var log : result)
			r.Result.getLogs().add(new BLog.Data(log.getTime(), log.getLog()));
		r.SendResult();
		// 顺带惰性清理：同Browse，锁外调用。
		agent.cleanIdleLogSessions(logConfs.sessionIdleTimeoutMillis);
		return 0;
	}

	@Override
	protected long ProcessQueryRequest(Query r) throws Exception {
		String json = r.Argument.getJson();
		String result = QueryHandlerManager.invokeHandler(json);
		r.Result.setJson(result);
		r.SendResult();
		return Procedure.Success;
	}

}
