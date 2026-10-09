package Zeze.Util;

import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 行注释的合法结束：LF、CR、CRLF、声明的JSON5 Unicode行终止符（U+2028/U+2029）
 * 与EOF。旧实现只认LF且不查边界——行注释到输入末尾（无末尾换行）直接
 * ArrayIndexOutOfBoundsException；CR换行的合法文本无法读取。
 * 块注释未终结到EOF按耗尽处理（next()走NUL哨兵），不用越界代替诊断。
 */
@Fast
public class TestJsonCommentTermination {


	private static Object parse(String s) {
		try {
			return new JsonReader().buf(s).parse();
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void lineCommentToEndOfInputReturnsNull() throws Exception {
		Assertions.assertNull(parse("// comment"));
		Assertions.assertNull(parse("  // comment")); // 前导空白
	}

	@Test
	public void carriageReturnTerminatesLineComment() throws Exception {
		var bean = parse("//comment\r1");
		Assertions.assertEquals(1L, ((Number)bean).longValue(), "CR是合法行终止符，注释后的值必须可见");
		var crlf = parse("//comment\r\n1");
		Assertions.assertEquals(1L, ((Number)crlf).longValue(), "CRLF同样结束行注释");
	}

	@Test
	public void lfLineCommentStillWorks() throws Exception {
		Assertions.assertNull(parse("//comment\n"));
		Assertions.assertEquals(1L, ((Number)parse("//comment\n1")).longValue());
		Assertions.assertNull(parse("/* comment */"));
	}

	@Test
	public void unterminatedBlockCommentAtEofIsExhaustion() throws Exception {
		// 顶层未终结块注释：不再越界，按耗尽处理（next走NUL哨兵，parse得到null默认值）
		Assertions.assertNull(parse("/* comment"));
	}
}
