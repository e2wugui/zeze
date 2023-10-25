package Zeze.log;

import Zeze.Config;
import Zeze.Services.LogService;
import Zeze.Util.Task;

public class MainLogWeb {


    public static void main(String[] args) throws Exception {

        Task.tryInitThreadPool(null, null, null);

        var configXml = "server.xml";
        var logService = new LogService(Config.load(configXml));
        logService.start();
        LogAgentManager.init(configXml);

    }

}
