import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import zezeboot.App;

public class ZezeBootServer {
	static {
		System.getProperties().putIfAbsent("logname", "server");
	}

	private static final Logger logger = LogManager.getLogger(ZezeBootServer.class);

	public synchronized static void main(String[] args) throws Exception {
		Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
			//noinspection CallToPrintStackTrace
			e.printStackTrace();
			logger.error("uncaught exception in {}:", t, e);
		});
		try {
			App.instance.start(args);
			ZezeBootServer.class.wait();
		} catch (Throwable e) { // logger.error
			logger.error("main exception:", e);
		} finally {
			try {
				App.instance.stop();
			} finally {
				System.exit(0);
			}
		}
	}
}
