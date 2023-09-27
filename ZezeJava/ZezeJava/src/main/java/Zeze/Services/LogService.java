package Zeze.Services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Builtin.LogService.BLog;
import Zeze.Builtin.LogService.Browse;
import Zeze.Builtin.LogService.CloseSession;
import Zeze.Builtin.LogService.NewSession;
import Zeze.Builtin.LogService.Search;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Service;
import Zeze.Services.Log4jQuery.Log4jLog;
import Zeze.Services.Log4jQuery.Log4jSession;
import Zeze.Services.ServiceManager.AbstractAgent;
import Zeze.Services.ServiceManager.Agent;
import Zeze.Transaction.Procedure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

public class LogService extends AbstractLogService {
    private final AtomicLong sidSeed = new AtomicLong();
    private final Config conf;
    private final LogServiceConf logConf;
    private final Server server;
    private final AbstractAgent serviceManager;

    public LogService(Config config) throws Exception {
        this.conf = config;
        this.logConf = new LogServiceConf();
        config.parseCustomize(this.logConf);

        this.server = new Server(config);

        switch (conf.getServiceManager()) {
        case "raft":
            if (conf.getServiceManagerConf().getSessionName().isEmpty()) {
                conf.getServiceManagerConf().setSessionName("LogServiceServer#" + conf.getServerId());
            }
            serviceManager = new ServiceManagerAgentWithRaft(this.conf);
            break;

        case "disable":
            serviceManager = null;
            break;

        default:
            serviceManager = new Agent(this.conf);
            break;
        }
    }

    public void start() throws Exception {
        var serviceManagerConf = conf.getServiceConf(Agent.defaultServiceName);
        if (serviceManagerConf != null && serviceManager != null) {
            serviceManager.start();
            try {
                serviceManager.waitReady();
            } catch (Exception ignored) {
                // raft 版第一次等待由于选择leader原因肯定会失败一次。
                serviceManager.waitReady();
            }
        }
        server.start();
    }

    public void stop() throws Exception {
        this.server.stop();
        if (serviceManager != null)
            serviceManager.close();
    }

    public static class LogServiceConf implements Config.ICustomize {
        public String logActive;
        public String logRotateDir;
        public String logTimeFormat;

        @Override
        public @NotNull String getName() {
            return "LogServiceConf";
        }

        @Override
        public void parse(@NotNull Element self) {
            logActive = self.getAttribute("LogActive");
            logRotateDir = self.getAttribute("LogRotateDir");
            logTimeFormat = self.getAttribute("LogTimeFormat");
            if (!logTimeFormat.isBlank())
                Log4jLog.LogTimeFormat = logTimeFormat;
        }
    }

    public class Server extends Service {
        public Server(Config config) {
            super("LogServiceServer", config);
        }

        @Override
        public void OnHandshakeDone(@NotNull AsyncSocket so) throws Exception {
            so.setUserState(new AgentUserState());
            super.OnHandshakeDone(so);
        }

        @Override
        public void OnSocketClose(@NotNull AsyncSocket so, @Nullable Throwable e) throws Exception {
            var agent = (AgentUserState)so.getUserState();
            if (null != agent)
                agent.close();
            super.OnSocketClose(so, e);
        }
    }

    public class AgentUserState {
        private final ConcurrentHashMap<Long, Log4jSession> logSessions = new ConcurrentHashMap<>();

        public Log4jSession getLogSession(long sid) {
            return logSessions.get(sid);
        }

        public void newLogSession(long sid) throws IOException {
            var logSession = new Log4jSession(logConf.logActive, logConf.logRotateDir);
            var exist = logSessions.putIfAbsent(sid, logSession);
            if (null != exist) {
                logSession.close();
                throw new IllegalArgumentException("duplicate sid=" + sid);
            }
        }

        public void closeLogSession(long sid) throws IOException {
            var logSession = logSessions.remove(sid);
            if (null != logSession)
                logSession.close();
        }

        public void close() throws IOException {
            for (var logSession : logSessions.values())
                logSession.close();
        }
    }

    @Override
    protected long ProcessCloseSessionRequest(CloseSession r) throws Exception {
        var agent = (AgentUserState)r.getSender().getUserState();
        agent.closeLogSession(r.Argument.getId());
        r.SendResult();
        return 0;
    }

    @Override
    protected long ProcessNewSessionRequest(NewSession r) throws Exception {
        var agent = (AgentUserState)r.getSender().getUserState();
        r.Result.setId(sidSeed.incrementAndGet());
        agent.newLogSession(r.Result.getId());
        r.SendResult();
        return 0;
    }

    @Override
    protected long ProcessBrowseRequest(Browse r) throws Exception {
        var agent = (AgentUserState)r.getSender().getUserState();
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
                    r.Argument.getCondition().getWords(), r.Argument.getCondition().isContainsAll(),
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
        var agent = (AgentUserState)r.getSender().getUserState();
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
                    r.Argument.getCondition().getWords(), r.Argument.getCondition().isContainsAll(),
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
}
