package UnitTest.Zeze.Util;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import Zeze.Util.DumpRocksDb;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND5-07：DumpRocksDb.isUtf8 两重错误——连续字节数不按引导字节区分（2字节序列查两个续字节、
 * 3字节查三个，多查的是下一字符首字节或越界），且续字节掩码用0xC0（真续字节&0xC0==0x80必被
 * 误拒）——合法多字节UTF-8几乎全被误判，dumpVar的BYTES分支回退十六进制转义。
 */
@Fast
public class TestDumpRocksDbIsUtf8 {

	@Test
	public void testIsUtf8() throws Exception {
		var m = DumpRocksDb.class.getDeclaredMethod("isUtf8", byte[].class);
		m.setAccessible(true);

		Assertions.assertTrue((boolean)m.invoke(null, (Object)"hello".getBytes(StandardCharsets.UTF_8)),
				"纯ASCII可打印必须通过");

		// 修复前红：3字节"中"末次检查越界、2字节"ÀA"第二查读到'A'，且真续字节被0xC0掩码误拒。
		Assertions.assertTrue((boolean)m.invoke(null, (Object)"中".getBytes(StandardCharsets.UTF_8)),
				"合法3字节UTF-8必须通过");
		Assertions.assertTrue((boolean)m.invoke(null, (Object)"ÀA".getBytes(StandardCharsets.UTF_8)),
				"合法2字节UTF-8后跟ASCII必须通过");
		Assertions.assertTrue((boolean)m.invoke(null, (Object)"中A中".getBytes(StandardCharsets.UTF_8)),
				"多字节与ASCII交错必须通过");

		// 拒绝面：不可打印控制字符、截断序列（引导字节后缺续字节）、4字节（策略不支持）、裸续字节
		Assertions.assertFalse((boolean)m.invoke(null, (Object)new byte[]{'a', 0x01}));
		Assertions.assertFalse((boolean)m.invoke(null, (Object)new byte[]{(byte)0xe4, (byte)0xb8}),
				"3字节序列截断必须拒绝");
		Assertions.assertFalse((boolean)m.invoke(null, (Object)new byte[]{(byte)0xc3}),
				"2字节序列截断必须拒绝");
		Assertions.assertFalse((boolean)m.invoke(null, (Object)"😀".getBytes(StandardCharsets.UTF_8)),
				"4字节序列（策略不含）必须拒绝");
		Assertions.assertFalse((boolean)m.invoke(null, (Object)new byte[]{(byte)0x80, 'a'}),
				"裸续字节必须拒绝");
	}
}
