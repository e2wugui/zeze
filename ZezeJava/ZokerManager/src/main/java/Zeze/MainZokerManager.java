package Zeze;

import Zeze.Config;
import Zeze.Services.LogService;
import Zeze.Util.Task;
import Zeze.log.LogAgentManager;

public class MainZokerManager {


    public static void main(String[] args) throws Exception {

        Task.tryInitThreadPool(null, null, null);

        var configXml = "server.xml";
        var logService = new LogService(Config.load(configXml));
        logService.start();
        LogAgentManager.init(configXml);

    }

}
