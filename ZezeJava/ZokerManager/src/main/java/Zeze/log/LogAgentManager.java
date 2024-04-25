package Zeze.log;

import java.text.ParseException;
import Zeze.Config;
import Zeze.Netty.HttpEndStreamHandle;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Services.LogAgent;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.log.handle.BrowseLogHandle;
import Zeze.log.handle.GetLogServersHandle;
import Zeze.log.handle.QueryHandle;
import Zeze.log.handle.SearchLogHandle;

public class LogAgentManager {
    private static LogAgentManager logAgentManager;
    public static HttpServer httpServer;
    private static Netty netty;
    private LogAgent logAgent;

    public static LogAgentManager getInstance(){
        return logAgentManager;
    }

    public static void init(String configXml) throws Exception {
        logAgentManager = new LogAgentManager();
        logAgentManager.logAgent = new LogAgent(Config.load(configXml));
        logAgentManager.logAgent.start();
        startHttpServer();
    }

    public LogAgent getLogAgent(){
        return logAgent;
    }


    private static void startHttpServer() throws Exception {
        httpServer = new HttpServer(null, "web", 600);
        netty = new Netty();

        addHandler("/api/get_log_servers", new GetLogServersHandle());
        addHandler("/api/browse", new BrowseLogHandle());
        addHandler("/api/search", new SearchLogHandle());
        addHandler("/api/query", new QueryHandle());
        httpServer.start(netty, 9980);
    }


    private static void addHandler(String path, HttpEndStreamHandle handle){
        httpServer.addHandler(path, Integer.MAX_VALUE, TransactionLevel.Serializable, DispatchMode.Normal, handle);
    }


}
