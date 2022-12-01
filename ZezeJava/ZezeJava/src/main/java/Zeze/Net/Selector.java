package Zeze.Net;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class Selector extends Thread {
	private final static Logger logger = LogManager.getLogger(Selector.class);

	private final java.nio.channels.Selector selector;
	private final java.nio.ByteBuffer readBuffer = java.nio.ByteBuffer.allocate(32 * 1024); // 此线程共享的buffer,只能临时使用
	private final AtomicInteger wakeupNotified = new AtomicInteger();
	private boolean firstAction;
	private volatile boolean running = true;

	Selector(String threadName) throws IOException {
		super(threadName);
		setDaemon(true);
		selector = java.nio.channels.Selector.open();
	}

	ByteBuffer getReadBuffer() {
		return readBuffer;
	}

	SelectionKey register(SelectableChannel sc, int ops, SelectorHandle handle) {
		try {
			SelectionKey key = sc.register(selector, ops, handle);
			// 当引擎线程执行register时，wakeup会导致一次多余唤醒。
			// 这在连接建立不是很繁忙的应用中问题不大。
			// 下面通过判断是否本线程来决定是否调用wakeup。
			if (Thread.currentThread() != this)
				selector.wakeup(); // 不会丢失。
			return key;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	void close() {
		running = false;
		selector.wakeup();

		// join
		while (true) {
			try {
				join();
				break;
			} catch (Throwable ex) {
				logger.error("{} close skip.", getClass().getName(), ex);
			}
		}

		try {
			selector.close();
		} catch (Throwable e) {
			logger.error("{} selector.close skip.", getClass().getName(), e);
		}
	}

	public void wakeup() {
		if (Thread.currentThread() != this && wakeupNotified.compareAndSet(0, 1))
			selector.wakeup();
	}

	@Override
	public void run() {
		while (running) {
			try {
				// 如果在这个时间窗口 wakeup，下面的 select 会马上返回。wakeup 不会丢失。
				firstAction = true;
				selector.select(key -> {
					if (firstAction) {
						firstAction = false;
						wakeupNotified.set(0);
					}
					if (!key.isValid())
						return; // key maybe cancel in loop
					SelectorHandle handle = null;
					try {
						handle = (SelectorHandle)key.attachment();
						handle.doHandle(key);
					} catch (Throwable e) {
						if (handle != null) {
							try {
								handle.doException(key, e);
							} catch (Throwable e3) {
								logger.error("Selector.run", e);
								logger.error("SelectorHandle.doException: {}", e, e3);
							}
						} else
							logger.error("Selector.run", e);
						try {
							key.channel().close();
						} catch (Throwable e2) {
							logger.error("SocketChannel.close", e2);
						}
					}
				});
			} catch (Throwable e) {
				logger.error("Selector.run", e);
			}
		}
	}
}
