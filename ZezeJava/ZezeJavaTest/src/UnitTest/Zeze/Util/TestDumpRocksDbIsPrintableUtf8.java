package UnitTest.Zeze.Util;

import java.nio.charset.StandardCharsets;

import Zeze.Util.DumpRocksDb;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * DumpRocksDb.isPrintableUtf8：dumpVar的BYTES分支显示启发式——string/binary共用BYTES标签，
 * 序列化后类型丢失，只能按内容猜"是否可打印UTF-8文本"（可打印性同时保住每行一条记录的输出格式）。
 * FND5-07修复连续字节数与续字节掩码两重误判；后续再支持4字节emoji（游戏聊天数据常见）
 * 并拒绝0xC0/0xC1过长编码。
 */
@Fast
public class TestDumpRocksDbIsPrintableUtf8 {

	@Test
	public void testIsPrintableUtf8() throws Exception {
		var m = DumpRocksDb.class.getDeclaredMethod("isPrintableUtf8", byte[].class);
		m.setAccessible(true);

		Assertions.assertTrue((boolean)m.invoke(null, (Object)"hello".getBytes(StandardCharsets.UTF_8)),
				"纯ASCII可打印必须通过");

		// FND5-07修复前红：3字节"中"末次检查越界、2字节"ÀA"第二查读到'A'，且真续字节被0xC0掩码误拒。
		Assertions.assertTrue((boolean)m.invoke(null, (Object)"中".getBytes(StandardCharsets.UTF_8)),
				"合法3字节UTF-8必须通过");
		Assertions.assertTrue((boolean)m.invoke(null, (Object)"ÀA".getBytes(StandardCharsets.UTF_8)),
				"合法2字节UTF-8后跟ASCII必须通过");
		Assertions.assertTrue((boolean)m.invoke(null, (Object)"中A中".getBytes(StandardCharsets.UTF_8)),
				"多字节与ASCII交错必须通过");
		Assertions.assertTrue((boolean)m.invoke(null, (Object)"😀".getBytes(StandardCharsets.UTF_8)),
				"4字节emoji必须通过（游戏聊天数据常见）");
		Assertions.assertTrue((boolean)m.invoke(null, (Object)"中😀A".getBytes(StandardCharsets.UTF_8)),
				"3/4字节与ASCII交错必须通过");

		// 拒绝面：不可打印控制字符、截断序列（引导字节后缺续字节）、裸续字节、非法/过长引导
		Assertions.assertFalse((boolean)m.invoke(null, (Object)new byte[]{'a', 0x01}));
		Assertions.assertFalse((boolean)m.invoke(null, (Object)new byte[]{(byte)0xe4, (byte)0xb8}),
				"3字节序列截断必须拒绝");
		Assertions.assertFalse((boolean)m.invoke(null, (Object)new byte[]{(byte)0xc3}),
				"2字节序列截断必须拒绝");
		Assertions.assertFalse((boolean)m.invoke(null, (Object)new byte[]{(byte)0xf0, (byte)0x9f, (byte)0x98}),
				"4字节序列截断必须拒绝");
		Assertions.assertFalse((boolean)m.invoke(null, (Object)new byte[]{(byte)0x80, 'a'}),
				"裸续字节必须拒绝");
		Assertions.assertFalse((boolean)m.invoke(null, (Object)new byte[]{(byte)0xc0, (byte)0xaf}),
				"0xC0引导的过长编码必须拒绝");
		Assertions.assertFalse((boolean)m.invoke(null, (Object)new byte[]{(byte)0xc1, (byte)0xbf}),
				"0xC1引导的过长编码必须拒绝");
		Assertions.assertFalse((boolean)m.invoke(null, (Object)new byte[]{(byte)0xf5, (byte)0x80, (byte)0x80, (byte)0x80}),
				"0xF5+非法引导字节必须拒绝");
	}
}
