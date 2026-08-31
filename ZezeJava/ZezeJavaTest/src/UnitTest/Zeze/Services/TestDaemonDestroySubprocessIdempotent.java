package UnitTest.Zeze.Services;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import Zeze.Services.Daemon;

@Fast
public class TestDaemonDestroySubprocessIdempotent {
	private static final long FAKE_PID = 424242;

	/** 只需要pid与destroy被用到；destroySubprocess不触碰流与waitFor。 */
	private static class FakeProcess extends Process {
		int destroyCount;

		@Override
		public long pid() {
			return FAKE_PID;
		}

		@Override
		public void destroy() {
			destroyCount++;
		}

		@Override
		public boolean isAlive() {
			return false;
		}

		@Override
		public int exitValue() {
			return 0;
		}

		@Override
		public boolean waitFor(long timeout, TimeUnit unit) {
			return true;
		}

		@Override
		public int waitFor() {
			return 0;
		}

		@Override
		public OutputStream getOutputStream() {
			return OutputStream.nullOutputStream();
		}

		@Override
		public InputStream getInputStream() {
			return InputStream.nullInputStream();
		}

		@Override
		public InputStream getErrorStream() {
			return InputStream.nullInputStream();
		}
	}

	@Test
	public void testDestroySubprocessIdempotent() throws Exception {
		// 场景：Monitor.run对同一快照多个global同轮超时连续两次destroySubprocess
		//（多GCM部署下服务器hang时同步冻结是常态），第二次必须幂等返回而非NPE→fatalExit(halt)。
		var fake = new FakeProcess();
		Field field = Daemon.class.getDeclaredField("subprocess");
		field.setAccessible(true);
		Method method = Daemon.class.getDeclaredMethod("destroySubprocess");
		method.setAccessible(true);
		var jstackFile = Path.of("jstack." + FAKE_PID);
		try {
			field.set(null, fake);
			method.invoke(null); // 第一次：销毁并置空
			method.invoke(null); // 第二次：幂等返回（修复前此处InvocationTargetException(NPE)→Monitor.run catch→halt）
		} finally {
			field.set(null, null);
			Files.deleteIfExists(jstackFile); // jstack存在的环境下第一次调用可能写出诊断文件
		}
		assertEquals(1, fake.destroyCount); // 恰好销毁一次
	}
}
