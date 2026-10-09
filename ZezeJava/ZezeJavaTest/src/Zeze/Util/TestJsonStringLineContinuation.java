package Zeze.Util;

import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * JSON5声明的行续写语义：引号字符串中反斜杠后跟实际LF/CR/CRLF/U+2028/
 * U+2029时不产生字符（消费换行继续字符串）。旧实现把换行原样放进结果——
 * 跨行配置串（URL、命令、标识符）被静默插入LF。字面转义\n（反斜杠+n）
 * 的正常LF解码语义保持。
 */
@Fast
public class TestJsonStringLineContinuation {

	private static String parse(String s) {
		try {
			return (String)new JsonReader().buf(s).parse();
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void realLineBreaksAreConsumedNotKept() {
		Assertions.assertEquals("ab", parse("'a" + (char)92 + (char)10 + "b'"), "反斜杠+实际LF：消费换行");
		Assertions.assertEquals("ab", parse("'a" + (char)92 + (char)13 + "b'"), "反斜杠+实际CR");
		Assertions.assertEquals("ab", parse("'a" + (char)92 + (char)13 + (char)10 + "b'"), "反斜杠+CRLF按一个换行");
		Assertions.assertEquals("ab", parse("'a" + (char)92 + (char)0x2028 + "b'"), "反斜杠+U+2028");
		Assertions.assertEquals("ab", parse("'a" + (char)92 + (char)0x2029 + "b'"), "反斜杠+U+2029");
	}

	@Test
	public void escapedLineFeedLiteralStillDecodes() {
		Assertions.assertEquals("a\nb", parse("'a\\nb'"), "字面转义\\n仍是LF");
		Assertions.assertEquals("a\\b", parse("'a\\\\b'"), "字面转义反斜杠不受影响");
		Assertions.assertEquals("a\tb", parse("'a\\tb'"));
	}
}
