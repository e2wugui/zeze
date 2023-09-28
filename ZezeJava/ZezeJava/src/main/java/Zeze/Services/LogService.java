package Zeze.Services;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Application;
import Zeze.Builtin.LogService.BLog;
import Zeze.Builtin.LogService.Browse;
import Zeze.Builtin.LogService.CloseSession;
import Zeze.Builtin.LogService.NewSession;
import Zeze.Builtin.LogService.Search;
import Zeze.Config;
import Zeze.Services.Log4jQuery.Log4jLog;
import Zeze.Services.Log4jQuery.LogServiceConf;
import Zeze.Services.Log4jQuery.Server;
import Zeze.Services.Log4jQuery.ServerUserState;
import Zeze.Services.ServiceManager.AbstractAgent;
import Zeze.Services.ServiceManager.Agent;
import Zeze.Transaction.Procedure;

public class LogService extends AbstractLogService {
    private final AtomicLong sidSeed = new AtomicLong();
    private final Config conf;
    private final LogServiceConf logConf;
    private final AbstractAgent serviceManager;
    private final Server server;

    public LogService(Config config) throws Exception {
        this.conf = config;
        logConf = new LogServiceConf();
        config.parseCustomize(logConf);

        this.server = new Server(logConf, config);
        var kv = server.getOnePassiveAddress();
        logConf.formatServiceIdentity(conf.getServerId(), kv.getKey(), kv.getValue());
        serviceManager = Application.createServiceManager(conf, "LogServiceServer");
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
            serviceManager.registerService("Zeze.LogService", logConf.serviceIdentity);
        }
        server.start();
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
        agent.newLogSession(r.Result.getId());
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
