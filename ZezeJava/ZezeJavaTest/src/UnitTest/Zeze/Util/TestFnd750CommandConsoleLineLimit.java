package UnitTest.Zeze.Util;

import java.util.ArrayList;
import java.util.List;
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
}
