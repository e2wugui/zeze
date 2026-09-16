package UnitTest.Zeze.Util;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import Zeze.Util.CommandConsole;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND7-51回归：TcpSocket.processReceive把每次read得到的任意长度块交给
 * OnSocketProcessInputBuffer，控制台对每个块单独new String(bytes,UTF_8)再拼行缓冲
 * ——多字节UTF-8字符被TCP分段边界切开时两次解码各自遇到非法前缀/续字节，
 * 均产出U+FFFD，命令参数静默损坏（无任何报错）。
 * 修复：字节层累积，仅对完整行做一次UTF-8解码。
 * 复现方式：受控字节流按指定位置切块输入（确定性，不依赖真实TCP分段）。
 */
@Fast
public class TestFnd751CommandConsoleUtf8AcrossChunks {

	private static void feedSplit(@SuppressWarnings("SameParameterValue") CommandConsole cc,
								  String text, int splitAt) {
		var bytes = text.getBytes(StandardCharsets.UTF_8);
		cc.input(null, bytes, 0, splitAt);
		cc.input(null, bytes, splitAt, bytes.length - splitAt);
	}

	@Test
	public void test3ByteCharSplitAcrossChunks() {
		var received = new ArrayList<String>();
		var cc = new CommandConsole();
		cc.register("say", (sender, args) -> received.add(String.join(" ", args)));
		// "中"=E4 B8 AD，切在第1字节后
		feedSplit(cc, "say 中\n", "say ".getBytes(StandardCharsets.UTF_8).length + 1);
		Assertions.assertEquals(List.of("中"), received, "跨块3字节字符不得损坏为U+FFFD");
	}

	@Test
	public void test4ByteEmojiSplitAtEveryBoundary() {
		// 4字节emoji 😀=F0 9F 98 80，逐字节切块（最细粒度），拼合后必须完整
		var received = new ArrayList<String>();
		var cc = new CommandConsole();
		cc.register("say", (sender, args) -> received.add(String.join(" ", args)));
		var bytes = "say 😀\n".getBytes(StandardCharsets.UTF_8);
		for (var b : bytes)
			cc.input(null, new byte[] {b}, 0, 1);
		Assertions.assertEquals(List.of("😀"), received);
	}

	@Test
	public void testMultiCharAndAsciiMix() {
		var received = new ArrayList<String>();
		var cc = new CommandConsole();
		cc.register("say", (sender, args) -> received.add(String.join(" ", args)));
		var text = "say 中文abc😀 x\n";
		var bytes = text.getBytes(StandardCharsets.UTF_8);
		for (var splitAt = 1; splitAt < bytes.length; splitAt++) { // 每个可能的边界都不得损坏
			var fresh = new CommandConsole();
			fresh.register("say", (sender, args) -> received.add(String.join(" ", args)));
			fresh.input(null, bytes, 0, splitAt);
			fresh.input(null, bytes, splitAt, bytes.length - splitAt);
		}
		var expected = "中文abc😀 x";
		Assertions.assertTrue(received.stream().allMatch(expected::equals),
				"all splits must decode intact, got: " + received);
	}

	@Test
	public void testAsciiUnchanged() {
		var received = new ArrayList<String>();
		var cc = new CommandConsole();
		cc.register("ok", (sender, args) -> received.add(String.join(" ", args)));
		// 纯ASCII跨块行为不变
		var bytes = "ok abc\n".getBytes(StandardCharsets.UTF_8);
		cc.input(null, bytes, 0, 3);
		cc.input(null, bytes, 3, bytes.length - 3);
		Assertions.assertEquals(List.of("abc"), received);
	}
}
