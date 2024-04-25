package Zeze.Services;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Application;
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

public class LogService extends AbstractLogService {
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
				configXml = args[++i];
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
		// build serviceIdentity
		for (var logConf : logConfs.getLogConfs().values()) {
			logManagers.put(logConf.getName(), new Log4jFileManager(logConf));
		}
		logConfs.formatServiceIdentity(conf.getServerId(), passiveIp, passivePort);
		serviceManager = Application.createServiceManager(conf, "LogServiceServer");
		RegisterProtocols(server);
	}

	public void start() throws Exception {
		server.start();
		var serviceManagerConf = conf.getServiceConf(Agent.defaultServiceName);
		if (serviceManagerConf != null && serviceManager != null) {
			serviceManager.start();
			try {
				serviceManager.waitReady();
			} catch (Exception ignored) {
				// raft 版第一次等待由于选择leader原因肯定会失败一次。
				serviceManager.waitReady();
			}
			serviceManager.registerService(new BServiceInfo(
					"Zeze.LogService", logConfs.serviceIdentity, 0, passiveIp, passivePort));
		}
	}

	public void stop() throws Exception {
		this.server.stop();
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
		var agent = (ServerUserState)r.getSender().getUserState();
		r.Result.setId(sidSeed.incrementAndGet());
		agent.newLogSession(r.Argument.getLogName(), r.Result.getId());
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessBrowseRequest(Browse r) throws Exception {
		var agent = (ServerUserState)r.getSender().getUserState();
		var logSession = agent.getLogSession(r.Argument.getId());
		if (null == logSession)
			return Procedure.LogicError;
		var result = new LinkedList<Log4jLog>();

		boolean remain;
		if (!r.Argument.getCondition().getWords().isEmpty()) {
			if (r.Argument.isReset())
				logSession.reset();
			remain = logSession.browseContains(result,
					r.Argument.getCondition().getBeginTime(), r.Argument.getCondition().getEndTime(),
					r.Argument.getCondition().getWords(), r.Argument.getCondition().getContainsType(),
					r.Argument.getLimit(), r.Argument.getOffsetFactor());
		} else if (!r.Argument.getCondition().getPattern().isEmpty()) {
			if (r.Argument.isReset())
				logSession.reset();
			remain = logSession.browseRegex(result,
					r.Argument.getCondition().getBeginTime(), r.Argument.getCondition().getEndTime(),
					r.Argument.getCondition().getPattern(),
					r.Argument.getLimit(), r.Argument.getOffsetFactor());
		} else
			throw new IllegalArgumentException("no condition.");

		r.Result.setRemain(remain);
		for (var log : result)
			r.Result.getLogs().add(new BLog.Data(log.getTime(), log.getLog()));
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessSearchRequest(Search r) throws Exception {
		var agent = (ServerUserState)r.getSender().getUserState();
		var logSession = agent.getLogSession(r.Argument.getId());
		if (null == logSession)
			return Procedure.LogicError;
		var result = new ArrayList<Log4jLog>();
		boolean remain;
		if (!r.Argument.getCondition().getWords().isEmpty()) {
			if (r.Argument.isReset())
				logSession.reset();
			remain = logSession.searchContains(result,
					r.Argument.getCondition().getBeginTime(), r.Argument.getCondition().getEndTime(),
					r.Argument.getCondition().getWords(), r.Argument.getCondition().getContainsType(),
					r.Argument.getLimit());
		} else if (!r.Argument.getCondition().getPattern().isEmpty()) {
			if (r.Argument.isReset())
				logSession.reset();
			remain = logSession.searchRegex(result,
					r.Argument.getCondition().getBeginTime(), r.Argument.getCondition().getEndTime(),
					r.Argument.getCondition().getPattern(),
					r.Argument.getLimit());
		} else
			throw new IllegalArgumentException("no condition.");

		r.Result.setRemain(remain);
		for (var log : result)
			r.Result.getLogs().add(new BLog.Data(log.getTime(), log.getLog()));
		r.SendResult();
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
