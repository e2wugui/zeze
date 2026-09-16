package UnitTest.Zeze.Services;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;

import java.io.IOException;
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
		int destroyForciblyCount;
		boolean waitForTimedOut;

		FakeProcess() {
		}

		FakeProcess(boolean waitForTimedOut) {
			this.waitForTimedOut = waitForTimedOut;
		}

		@Override
		public Process destroyForcibly() {
			destroyForciblyCount++;
			return this;
		}

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
			return !waitForTimedOut;
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

	@Test
	public void testReapDiagnosticForciblyOnTimeout() throws Exception {
		// FND6-31补：诊断子进程收尾finally化——waitFor超时/中断必须强杀，防jstack孤儿。
		Method reap = Daemon.class.getDeclaredMethod("reapDiagnosticProcess", Process.class);
		reap.setAccessible(true);

		var timedOut = new FakeProcess(true); // waitFor(30s)返回false=超时
		reap.invoke(null, timedOut);
		assertEquals(1, timedOut.destroyForciblyCount, "超时必须destroyForcibly收尸");

		var normal = new FakeProcess(); // waitFor返回true=正常退出
		reap.invoke(null, normal);
		assertEquals(0, normal.destroyForciblyCount, "正常退出不强杀");
	}

	@Test
	public void testJstackOverwritesPreexistingFileOnPidReuse() throws Exception {
		// FND6-31原始场景行为级钉桩：pid复用时目标jstack文件已存在，copy必须覆盖写入
		// 保留最新现场（无REPLACE_EXISTING时FileAlreadyExistsException跳到外层catch，
		// 文件保留旧内容、收尾不执行）。
		Assumptions.assumeTrue(jstackAvailable(), "环境无jstack，跳过");

		var fake = new FakeProcess();
		Field field = Daemon.class.getDeclaredField("subprocess");
		field.setAccessible(true);
		Method method = Daemon.class.getDeclaredMethod("destroySubprocess");
		method.setAccessible(true);
		var jstackFile = Path.of("jstack." + FAKE_PID);
		try {
			Files.writeString(jstackFile, "OLD");
			field.set(null, fake);
			method.invoke(null);
			var content = Files.readString(jstackFile);
			Assertions.assertNotEquals("OLD", content, "pid复用时必须覆盖旧现场文件");
		} finally {
			field.set(null, null);
			Files.deleteIfExists(jstackFile);
		}
		assertEquals(1, fake.destroyCount);
	}

	private static boolean jstackAvailable() {
		try {
			new ProcessBuilder("jstack").start().destroyForcibly();
			return true;
		} catch (IOException e) {
			return false;
		}
	}
}
