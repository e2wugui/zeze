package Temp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/*
开启异步输出日志只需要增加JVM参数(log4j2.xml无需配置任何异步,但要依赖disruptor库):
-Dlog4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector
开启后,会多出一个名字类似 Log4j2-TF-1-AsyncLogger[AsyncContext@4e25154f]-1 的线程,
其栈信息有 com.lmax.disruptor.ProcessingSequenceBarrier.waitFor
 */
public class TestLog4j {
	private static final Logger logger = LogManager.getLogger(TestLog4j.class);

	public static void main(String[] args) throws InterruptedException {
		//noinspection InfiniteLoopStatement
		for (int i = 0; ; i++) {
			logger.info("{}", i);
			//noinspection BusyWait
			Thread.sleep(1000);
		}
	}
}
