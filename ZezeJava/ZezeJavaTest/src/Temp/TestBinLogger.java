package Temp;

import java.util.concurrent.Executors;
import Zeze.Services.BinLogger;
import Zeze.Util.Task;
import Zeze.Util.ThreadFactoryWithName;
import demo.Module1.BValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TestBinLogger {
	private static final Logger logger = LogManager.getLogger(TestBinLogger.class);

	public static void main(String[] args) throws Exception {
		Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
			//noinspection CallToPrintStackTrace
			e.printStackTrace();
			logger.error("uncaught exception in {}:", t, e);
		});
		Task.initThreadPool(Task.newCriticalThreadPool("ZezeTaskPool"),
				Executors.newSingleThreadScheduledExecutor(new ThreadFactoryWithName("ZezeScheduledPool")));

		var service = new BinLogger.BinLoggerService("binlog");
		service.start(null, 5004);

		var agent = new BinLogger.BinLoggerAgent();
		agent.start("127.0.0.1", 5004).waitReady();

		var b = new BValue.Data();
		b.setInt_1(9);
		logger.info("sendLog: {}", agent.sendLog(12345, b));

		Thread.sleep(1000);
		agent.stop();
		service.stop();
		logger.info("end");
	}
}
