package Zeze.Services.Log4jQuery;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchService;
import java.nio.file.ClosedWatchServiceException;
import java.util.function.Consumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 目录文件创建监视器：后台线程消费 WatchService 的 ENTRY_CREATE 事件，
 * 并在 OVERFLOW 与监听失效时触发回调补偿。
 * 构造只完成注册（事件在内核排队不丢），消费线程由 {@link #start()} 显式启动。
 */
public class FileCreateDetector {
	private static final @NotNull Logger logger = LogManager.getLogger(FileCreateDetector.class);
	private final WatchService watchService;
	private final Path watchDir;
	// start()前为null（构造不启动消费线程）；volatile：stopAndJoin可能与start由不同线程读写。
	private volatile @Nullable Thread watchThread;
	private volatile boolean running = true;
	private final Consumer<Path> consumer;
	// OVERFLOW：warn+节流对账；监听失效（key.reset()==false）：error+最终对账。可为null（不关心）。
	private final Runnable onOverflowConsumer;
	private final Runnable onWatchInvalidConsumer;

	public FileCreateDetector(String watchDir, Consumer<Path> onCreateConsumer) throws IOException {
		this(watchDir, onCreateConsumer, null, null);
	}

	public FileCreateDetector(String watchDir, Consumer<Path> onCreateConsumer,
							  Runnable onOverflowConsumer, Runnable onWatchInvalidConsumer) throws IOException {
		this.consumer = onCreateConsumer;
		this.onOverflowConsumer = onOverflowConsumer;
		this.onWatchInvalidConsumer = onWatchInvalidConsumer;
		// watchDir先解析再建watchService：Paths.get对非法路径抛运行时异常，顺序反了会泄漏已建实例。
		this.watchDir = Paths.get(watchDir);
		this.watchService = FileSystems.getDefault().newWatchService();
		// 构造只注册不启动：事件在内核排队不丢。消费线程不得抢在调用方装载完成前处理事件——
		// 调用方状态未就绪时的早退分支会跳过本应随事件执行的处置（见Log4jFileManager.start调用处）。
		try {
			this.watchDir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
		} catch (IOException e) {
			// 注册失败（目录缺失NoSuchFileException等）上抛：此时未start、调用方拿不到本实例引用，
			// 已创建的watchService无人回收（stopAndJoin不可达）——必须自关，否则泄漏至进程退出。
			try {
				watchService.close();
			} catch (IOException closeEx) {
				e.addSuppressed(closeEx);
			}
			throw e;
		}
	}

	/**
	 * 启动事件消费线程：必须在调用方完成与onCreateConsumer存在竞态的初始化（manager的装载）之后调用一次。
	 * 注册到start之间发生的创建事件已在内核排队，start后按序补处理。
	 */
	public void start() {
		var thread = new Thread(this::run, "log4j-watch-" + watchDir);
		// daemon：线程生命周期由 stopAndJoin 显式管理（close watchService + join），不改
		// 正常停机语义；宿主启动失败/异常退出路径不被阻塞在 take() 的 watch 线程钉住。
		thread.setDaemon(true);
		watchThread = thread; // 先发布再start：stopAndJoin并发读到的线程join立即返回或正常join
		thread.start();
	}

	public Path getWatchDir() {
		return watchDir;
	}

	private void run() {
		while (running) {
			try {
				var key = watchService.take();
				try {
					for (var event : key.pollEvents()) {
						var kind = event.kind();
						if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
							@SuppressWarnings("unchecked") WatchEvent<Path> eventPath = (WatchEvent<Path>)event;
							consumer.accept(eventPath.context());
						} else if (kind == StandardWatchEventKinds.OVERFLOW) {
							// 事件溢出=可能已丢失：warn+节流触发一次对账补偿，不静默吞掉。
							logger.warn("watch OVERFLOW, events may be lost: {}", watchDir);
							if (null != onOverflowConsumer)
								onOverflowConsumer.run();
						}
					}
				} catch (Throwable eventEx) {
					// 事件处理抛Error（OOM等）不得杀线程也不得跳过reset：signalled的key不reset
					// 永不再排队——等效监听静默死亡。记error后继续消费（停止中不记，保留原语义）。
					if (running)
						logger.error("watch event process fail: {}", watchDir, eventEx);
				}
				if (!key.reset()) {
					// 目录不可访问/被删除，监听从此死亡：error告警后退出循环，退出前触发最终对账。
					logger.error("watch key reset fail, watch dead: {}", watchDir);
					if (null != onWatchInvalidConsumer)
						onWatchInvalidConsumer.run();
					break;
				}
			} catch (ClosedWatchServiceException ex) {
				break; // stopAndJoin关闭了watchService，正常退出
			} catch (Throwable ex) {
				// take()抛Error（OOM等）同样不得静默杀线程：Error只记日志继续循环（无限重试take），
				// 停止中的异常不当错误处理（保留原退出语义）。
				if (!running)
					break;
				logger.error("", ex);
			}
		}
	}

	public void stopAndJoin() {
		running = false;
		try {
			// take()无超时阻塞，必须先关闭watchService解除阻塞（抛ClosedWatchServiceException），否则join永久挂起。
			// close幂等：未start（构造失败半途被回收）时同样安全。
			watchService.close();
		} catch (IOException e) {
			logger.error("close watchService failed", e);
		}
		var thread = watchThread;
		if (thread == null)
			return; // 未start：watchService已关闭即完成回收，无线程可join。
		try {
			thread.join();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}
