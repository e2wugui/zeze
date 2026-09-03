package UnitTest.Zeze.Util;
import harness.Fast;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import Zeze.Util.CommandConsole;

/**
 * 控制台一行带未闭合引号的命令不得：1) 向调用方抛未捕获异常；2) 使该连接的行缓冲永久毒化（见 FND-U1-6）。
 */
@Fast
public final class TestCommandConsole {
	@Test
	public void testUnclosedQuoteNotPoisonBuffer() {
		var received = new ArrayList<String>();
		var cc = new CommandConsole();
		cc.register("ok", (sender, args) -> received.add(String.join(" ", args)));

		// 第二行有奇数个引号（未闭合）；sender 为 null（命令实现不使用 sender），验证不抛且后续行正常执行
		assertDoesNotThrow(() -> cc.input(null, "ok 1\nbad \"unclosed\nok 2\n"));
		assertEquals(List.of("1", "2"), received);

		// 坏行已从缓冲移除：同一会话继续输入新命令正常执行
		assertDoesNotThrow(() -> cc.input(null, "ok 3\n"));
		assertEquals(List.of("1", "2", "3"), received);
	}

	@Test
	public void testUnclosedQuoteFollowedByGoodLineInSameInput() {
		var received = new ArrayList<String>();
		var cc = new CommandConsole();
		cc.register("ok", (sender, args) -> received.add(String.join(" ", args)));

		assertDoesNotThrow(() -> cc.input(null, "say \"hello\nok x\nok \"y z\"\n"));
		assertEquals(List.of("x", "y z"), received);
	}

	@Test
	public void testNormalQuotedArgsStillWork() {
		var received = new ArrayList<String>();
		var cc = new CommandConsole();
		cc.register("a", (sender, args) -> received.add(String.join("|", args)));

		assertDoesNotThrow(() -> cc.input(null, "a -Dn1=v -D\"n3=v v\" d -Dn2=\"v v\" \"x x\"\n"));
		assertEquals(1, received.size());
		assertEquals("-Dn1=v|-Dn3=v v|d|-Dn2=v v|x x", received.get(0));
	}

	@Test
	public void testParseWordsStillThrows() {
		// public static parseWords 的契约保持不变：未闭合引号仍抛 IllegalStateException
		assertThrows(IllegalStateException.class, () -> CommandConsole.parseWords("say \"unclosed"));
	}

	public static void main(String[] args) {
		var t = new TestCommandConsole();
		t.testUnclosedQuoteNotPoisonBuffer();
		t.testUnclosedQuoteFollowedByGoodLineInSameInput();
		t.testNormalQuotedArgsStillWork();
		t.testParseWordsStillThrows();
		System.out.println(t.getClass().getSimpleName() + ": 4 tests OK!");
	}
}
