package Zeze.Services.Log4jQuery;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchService;
import java.util.function.Consumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FileCreateDetector {
	private static final Logger logger = LogManager.getLogger();
	private final WatchService watchService;
	private final Path watchDir;
	private final Thread watchThread;
	private volatile boolean running = true;
	private final Consumer<Path> consumer;

	public FileCreateDetector(String watchDir, Consumer<Path> onCreateConsumer) throws IOException {
		this.consumer = onCreateConsumer;
		this.watchService = FileSystems.getDefault().newWatchService();
		this.watchDir = Paths.get(watchDir);
		this.watchDir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
		this.watchThread = new Thread(this::run);
		this.watchThread.start();
	}

	public Path getWatchDir() {
		return watchDir;
	}

	private void run() {
		while (running) {
			try {
				var key = watchService.take();
				for (var event : key.pollEvents()) {
					var kind = event.kind();
					if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
						@SuppressWarnings("unchecked") WatchEvent<Path> eventPath = (WatchEvent<Path>)event;
						consumer.accept(eventPath.context());
					}
				}
				if (!key.reset())
					break;
			} catch (Exception ex) {
				logger.error("", ex);
			}
		}
	}

	public void stopAndJoin() {
		running = false;
		try {
			watchThread.join();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}
