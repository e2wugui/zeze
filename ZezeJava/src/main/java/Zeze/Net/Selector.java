package Zeze.Net;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Selector extends Thread {
	private final static Logger logger = LogManager.getLogger(Selector.class);
	private volatile boolean running = true;
	private final java.nio.channels.Selector selector;

	public Selector(String threadName) throws IOException {
		super(threadName);
		this.setDaemon(true);
		selector = java.nio.channels.Selector.open();
	}

	public SelectionKey register(SelectableChannel sc, int ops, SelectorHandle handle) {
		try {
			sc.configureBlocking(false);
			var key = sc.register(this.selector, ops, handle);
			// 当引擎线程执行register时，wakeup会导致一次多余唤醒。
			// 这在连接建立不是很繁忙的应用中问题不大。
			// 下面通过判断是否本线程来决定是否调用wakeup。
			if (Thread.currentThread().getId() != this.getId())
				this.selector.wakeup(); // 不会丢失。
			return key;
		} catch (java.io.IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void close() {
		this.running = false;
		this.selector.wakeup();

		// join
		while (true) {
			try {
				this.join();
				break;
			} catch (Throwable ex) {
				logger.error("{} close skip. ", this.getClass().getName(), ex);
			}
		}

		try {
			selector.close();
		} catch (Throwable e) {
			logger.error("{} selector.close skip. ", this.getClass().getName(), e);
		}
	}

	@Override
	public void run() {
		while (running) {
			try {
				// 如果在这个时间窗口 wakeup，下面的 select 会马上返回。wakeup 不会丢失。
				if (selector.select() <= 0)
					continue; // 没有事件，可能是 wakeup 或者 shutdown。

				java.util.Set<SelectionKey> selected = selector.selectedKeys();
				for (SelectionKey key : selected) {
					if (!key.isValid())
						continue; // key maybe cancel in loop

					SelectorHandle handle = null;
					try {
						handle = (SelectorHandle) key.attachment();
						handle.doHandle(key);
					}
					catch (Throwable e) {
						try {
							key.channel().close();
						}
						catch (Throwable e2) {
							logger.error(e2);
						}
						try {
							if (null != handle)
								handle.doException(key, e);
						}
						catch (Throwable e3) {
							logger.error("doHandle", e);
							logger.error("doException" + e, e3);
						}
					}
				}
				selected.clear();

			} catch (Throwable e) {
				logger.error("Selector.run", e);
			}
		}
	}
}
