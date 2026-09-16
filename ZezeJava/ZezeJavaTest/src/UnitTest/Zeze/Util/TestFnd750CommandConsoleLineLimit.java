package UnitTest.Zeze.Util;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import Zeze.Util.CommandConsole;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND7-50回归：控制台行协议按'\n'分帧，未终结行在CommandConsole行缓冲中无界累积——
 * CommandConsoleService.OnSocketProcessInputBuffer总是整块消费（ReadIndex=WriteIndex），
 * TcpSocket.processReceive的残留上限检查永不触发，单条无鉴权连接发送无换行字节流
 * 即可把进程堆耗尽（OOM）。修复：消费完整行后检查残留行缓冲长度，超上限抛
 * IllegalStateException（沿输入处理链关闭连接），且抛错前清空行缓冲。
 */
@Fast
public class TestFnd750CommandConsoleLineLimit {

	@Test
	public void testUnterminatedLineOverLimitThrows() {
		var cc = new CommandConsole();
		var limit = CommandConsole.MAX_LINE_BUFFER_SIZE;
		// 无换行输入累积超过上限必须抛错，而不是无界累积
		Assertions.assertThrows(IllegalStateException.class, () -> {
			for (int i = 0, n = limit / 1024 + 2; i < n; i++)
				cc.input(null, "x".repeat(1024));
		});
	}

	@Test
	public void testCompleteLinesDoNotAccumulate() {
		var received = new ArrayList<String>();
		var cc = new CommandConsole();
		cc.register("ok", (sender, args) -> received.add(String.join(" ", args)));
		var limit = CommandConsole.MAX_LINE_BUFFER_SIZE;
		// 单次输入总量远超上限，但每行都有换行终结：残留始终为0，不得误杀
		var line = "ok x\n";
		int lines = limit / line.length() + 10;
		Assertions.assertDoesNotThrow(() -> cc.input(null, line.repeat(lines)));
		Assertions.assertEquals(lines, received.size());
	}

	@Test
	public void testConsoleUsableAfterOverflow() {
		var received = new ArrayList<String>();
		var cc = new CommandConsole();
		cc.register("ok", (sender, args) -> received.add(String.join(" ", args)));
		var limit = CommandConsole.MAX_LINE_BUFFER_SIZE;
		Assertions.assertThrows(IllegalStateException.class, () -> {
			for (int i = 0, n = limit / 1024 + 2; i < n; i++)
				cc.input(null, "y".repeat(1024));
		});
		// 溢出抛错后行缓冲被清空：捕获异常继续使用的调用方不至于永久饱和
		Assertions.assertDoesNotThrow(() -> cc.input(null, "ok 1\n"));
		Assertions.assertEquals(List.of("1"), received);
	}

	/**
	 * R3-U2（D①）：commandConsoleMaxLineSize 是调优旋钮而非安全上限本身——畸形值（非法
	 * 字符）与非正值（0/负数）必须静默回落默认64K并warn，而不是NumberFormatException
	 * 炸掉类初始化（ExceptionInInitializerError使CommandConsole整个类不可用，控制台
	 * 彻底瘫痪，而默认值本身就是安全值）。属性在{@code <clinit>}读取，须子进程验证真实
	 * 初始化路径（同JVM内类已加载，改属性无效）。
	 */
	@Test
	public void testMalformedMaxLineSizePropertyFallsBackToDefault() throws Exception {
		Assertions.assertEquals("65536", runProbe("-DcommandConsoleMaxLineSize=abc"),
				"畸形属性必须回落默认64K（当前实现炸类初始化即红）");
		Assertions.assertEquals("65536", runProbe("-DcommandConsoleMaxLineSize=0"),
				"非正值属性必须回落默认64K（0会使每次input都抛溢出即红）");
		Assertions.assertEquals("65536", runProbe("-DcommandConsoleMaxLineSize=-8K"));
	}

	@Test
	public void testMaxLineSizePropertyOverrideStillWorks() throws Exception {
		// 合法覆写与max（显式无上限逃生门）不受容错影响
		Assertions.assertEquals("1048576", runProbe("-DcommandConsoleMaxLineSize=1M"));
		Assertions.assertEquals(String.valueOf(Integer.MAX_VALUE), runProbe("-DcommandConsoleMaxLineSize=max"));
	}

	/**
	 * 子进程带属性触发CommandConsole类初始化，打印MAX_LINE_BUFFER_SIZE。输出很小无
	 * 管道死锁风险；warn日志走stderr已并入，取最后一行（probe的println）为结果。
	 */
	private static String runProbe(String prop) throws Exception {
		var javaBin = Path.of(System.getProperty("java.home"), "bin",
				System.getProperty("os.name", "").toLowerCase().contains("win") ? "java.exe" : "java").toString();
		var pb = new ProcessBuilder(javaBin, prop, "-cp",
				System.getProperty("java.class.path"), MaxLineSizeProbe.class.getName());
		pb.redirectErrorStream(true);
		var p = pb.start();
		var out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		Assertions.assertTrue(p.waitFor(60, TimeUnit.SECONDS), () -> "probe timeout: " + out);
		Assertions.assertEquals(0, p.exitValue(), () -> "probe failed (类初始化抛错?): " + out);
		var lines = out.strip().split("\r?\n");
		return lines[lines.length - 1].strip();
	}

	/** 独立main：仅触碰CommandConsole.MAX_LINE_BUFFER_SIZE触发{@code <clinit>}。 */
	public static final class MaxLineSizeProbe {
		private MaxLineSizeProbe() {
		}

		public static void main(String[] args) {
			System.out.println(CommandConsole.MAX_LINE_BUFFER_SIZE);
		}
	}
}
