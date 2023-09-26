package Zeze.Services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Builtin.LogService.BLog;
import Zeze.Builtin.LogService.BRegex;
import Zeze.Builtin.LogService.BWords;
import Zeze.Builtin.LogService.Browse;
import Zeze.Builtin.LogService.CloseSession;
import Zeze.Builtin.LogService.NewSessionRegex;
import Zeze.Builtin.LogService.NewSessionWords;
import Zeze.Builtin.LogService.Search;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Service;
import Zeze.Services.Log4jQuery.Log4jLog;
import Zeze.Services.Log4jQuery.Log4jSession;
import Zeze.Transaction.Procedure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

public class LogService extends AbstractLogService {
    private final AtomicLong sidSeed = new AtomicLong();
    private final int serverId;
    private final LogServiceConf conf;
    private final Server server;

    public LogService(Config config) throws Exception {
        this.serverId = config.getServerId();
        this.conf = new LogServiceConf();
        config.parseCustomize(this.conf);

        this.server = new Server(config);
        this.server.start();
    }

    public void stop() throws Exception {
        this.server.stop();
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

        public void newLogSessionWords(long sid, BWords.Data words) throws IOException {
            var logSession = new Log4jSession(conf.logActive, conf.logRotateDir,
                    words.getBeginTime(), words.getEndTime(),
                    words.getWords(), words.isContainsAll());

            var exist = logSessions.putIfAbsent(sid, logSession);
            if (null != exist) {
                logSession.close();
                throw new IllegalArgumentException("duplicate sid=" + sid);
            }
        }

        public void newLogSessionRegex(long sid, BRegex.Data regex) throws IOException {
            var logSession = new Log4jSession(conf.logActive, conf.logRotateDir,
                    regex.getBeginTime(), regex.getEndTime(),
                    regex.getPattern());

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
    protected long ProcessNewSessionRegexRequest(NewSessionRegex r) throws Exception {
        var agent = (AgentUserState)r.getSender().getUserState();
        r.Result.setId(sidSeed.incrementAndGet());
        agent.newLogSessionRegex(r.Result.getId(), r.Argument);
        r.SendResult();
        return 0;
    }

    @Override
    protected long ProcessNewSessionWordsRequest(NewSessionWords r) throws Exception {
        var agent = (AgentUserState)r.getSender().getUserState();
        r.Result.setId(sidSeed.incrementAndGet());
        agent.newLogSessionWords(r.Result.getId(), r.Argument);
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
        var remain = logSession.browse(result, r.Argument.getLimit(), r.Argument.getOffsetFactor());
        for (var log : result)
            r.Result.getLogs().add(new BLog.Data(log.getTime(), log.getLog()));
        r.Result.setRemain(remain);
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
        var remain = logSession.search(result, r.Argument.getLimit());
        for (var log : result)
            r.Result.getLogs().add(new BLog.Data(log.getTime(), log.getLog()));
        r.Result.setRemain(remain);
        r.SendResult();
        return 0;
    }
}
