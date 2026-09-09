package UnitTest.Zeze.Serialize;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.NioByteBuffer;
import Zeze.Transaction.EmptyBean;

@Fast
public class TestByteBufferSafety {
	// 恶意/损坏流防御：ensureRead溢出绕过(Z1-1)、负Skip回退(Z1-3)、
	// decode(Collection)负size(Z1-2)、SkipUnknownField无界递归(Z1-4)。

	@Test
	public void testEnsureReadOverflow() {
		// FND2-Z1-1：恶意5字节varint巨值让ReadIndex+n溢出回绕为负，旧检查
		// "ReadIndex+size>WriteIndex"不成立而静默放行，ReadBytes先尝试~2GB数组分配
		// （n=0x7FFFFFFD，ReadIndex=5：5+n=0x80000002回绕为负）再失败——OOME/GC风暴。
		// 修复后ensureRead按差值比较并拒绝负size，直接抛ISE。
		var malicious = new byte[]{(byte)0xF0, 0x7F, (byte)0xFF, (byte)0xFF, (byte)0xFD};
		var bb = ByteBuffer.Wrap(malicious.clone());
		assertThrows(IllegalStateException.class, bb::ReadBytes); // 修复前：new byte[2147483645]

		var bb2 = ByteBuffer.Wrap(malicious.clone());
		assertEquals(0x7FFFFFFD, bb2.ReadUInt()); // 确认该字节流确实是正的巨值长度
		assertEquals(5, bb2.ReadIndex);

		// NioByteBuffer同型（position()+size溢出）
		var nio = NioByteBuffer.Wrap(malicious.clone(), malicious.length);
		assertThrows(IllegalStateException.class, nio::ReadBytes);

		// FND2-Z1-3：负size不再放行——修复前Skip(-1)会回退ReadIndex读旧数据
		var skip = ByteBuffer.Wrap(new byte[]{1, 2, 3});
		assertEquals(1, skip.ReadByte());
		assertThrows(IllegalStateException.class, () -> skip.ensureRead(-1));
		assertThrows(IllegalStateException.class, () -> skip.Skip(-1));
		assertEquals(1, skip.ReadIndex); // 游标未回退

		var nioSkip = NioByteBuffer.Wrap(new byte[]{1, 2, 3}, 3);
		assertEquals(1, nioSkip.ReadByte());
		assertThrows(IllegalStateException.class, () -> nioSkip.ensureRead(-1));
		assertThrows(IllegalStateException.class, () -> nioSkip.Skip(-1));
		assertEquals(1, nioSkip.getReadIndex());

		// 正常读取行为不变
		var normal = ByteBuffer.Wrap(new byte[]{2, 1, 2});
		assertArrayEquals(new byte[]{1, 2}, normal.ReadBytes());
	}

	@Test
	public void testDecodeCollectionNegativeSize() {
		// FND2-Z1-2：集合长度varint落在[2^31,2^32)（如F0-80-00-00-00）时ReadUInt返回负数，
		// 修复前decode(Collection)静默返回空集合——Edit日志重放等路径静默丢日志；
		// 修复后抛ISE（对齐同文件SkipBytes的入口拒绝负数模式）。
		var bytes = new byte[]{(byte)0xF0, (byte)0x80, 0, 0, 0};
		var c = new ArrayList<EmptyBean>();
		assertThrows(IllegalStateException.class, () -> ByteBuffer.Wrap(bytes.clone()).decode(c, EmptyBean::new));
		assertTrue(c.isEmpty());

		var nio = NioByteBuffer.Wrap(bytes.clone(), bytes.length);
		assertThrows(IllegalStateException.class, () -> nio.decode(new ArrayList<EmptyBean>(), EmptyBean::new));

		// 正常路径不变：2个EmptyBean（encode只写一个0终结符）
		var ok = ByteBuffer.Allocate();
		ok.WriteUInt(2);
		new EmptyBean().encode(ok);
		new EmptyBean().encode(ok);
		ok.ReadIndex = 0;
		var c2 = new ArrayList<EmptyBean>();
		ok.decode(c2, EmptyBean::new);
		assertEquals(2, c2.size());
	}

	private static byte[] nestedBeanBytes(int tag, int depth) {
		// N层嵌套BEAN流：BEAN是"字段序列+终结0"结构，每层1个tag、每层1个终结符，
		// 即(N-1)个嵌套tag + N个0，共2N-1字节；顶层tag由参数传入。
		var bytes = new byte[2 * depth - 1];
		Arrays.fill(bytes, 0, depth - 1, (byte)tag);
		Arrays.fill(bytes, depth - 1, bytes.length, (byte)0);
		return bytes;
	}

	@Test
	public void testSkipUnknownFieldDepthLimit() {
		// FND2-Z1-4：恶意深嵌套流（每层BEAN最少2字节）让SkipUnknownField无界递归，
		// 触发StackOverflowError穿透catch(Exception)路径；修复后嵌套深度超过上限抛ISE。
		var tag = (1 << IByteBuffer.TAG_SHIFT) | IByteBuffer.BEAN; // 0x16: delta=1, type=BEAN
		assertEquals(100, IByteBuffer.MAX_SKIP_UNKNOWN_FIELD_DEPTH);

		// 恰好100层（合法上限内）：正常skip并全部消费
		var deepest = nestedBeanBytes(tag, IByteBuffer.MAX_SKIP_UNKNOWN_FIELD_DEPTH);
		var bb = ByteBuffer.Wrap(deepest.clone());
		bb.SkipUnknownField(tag);
		assertTrue(bb.isEmpty());

		// 101层：超上限抛ISE
		var tooDeep = nestedBeanBytes(tag, IByteBuffer.MAX_SKIP_UNKNOWN_FIELD_DEPTH + 1);
		assertThrows(IllegalStateException.class,
				() -> ByteBuffer.Wrap(tooDeep.clone()).SkipUnknownField(tag));

		// 异常路径不泄漏深度计数：同线程继续skip深层（合法）成功——
		// 若finally复位缺失，残留depth会让本线程任何skip都误抛
		var bb2 = ByteBuffer.Wrap(tooDeep.clone());
		assertThrows(IllegalStateException.class, () -> bb2.SkipUnknownField(tag));
		var bb3 = ByteBuffer.Wrap(deepest.clone());
		bb3.SkipUnknownField(tag);
		assertTrue(bb3.isEmpty());

		// NioByteBuffer走同一个default方法，同型
		assertThrows(IllegalStateException.class,
				() -> NioByteBuffer.Wrap(tooDeep.clone(), tooDeep.length).SkipUnknownField(tag));
		var nbb = NioByteBuffer.Wrap(deepest.clone(), deepest.length);
		nbb.SkipUnknownField(tag);
		assertTrue(nbb.isEmpty());
	}

	@Test
	public void testPersistentCollectionDecodeNegativeSize() {
		// FND3-05：PList2/PMap1/PMap2/PSet1/PSortedMap1/PSortedMap2/BeanMap1/BeanMap2 的 decode
		// 旧代码先clear()再读无符号长度，长度varint落在[2^31,2^32)（F0-80-00-00-00=0x80000000）读回为负
		// 时循环判假跳过、容器被静默清空且decode正常返回——TableX记录加载静默丢字段内容，
		// 之后checkpoint全量写回即不可恢复。修复：校验收进IByteBuffer.ReadUIntPositive()原语
		// （避免生成代码膨胀），容器decode循环头改用ReadUIntPositive，负长度抛ISE、不再静默成功；
		// 生成器模板(Gen java/javadata/Arch)同发ReadUIntPositive。抛异常后bean整体作废，
		// 容器内容无需保留。
		var malicious = new byte[]{(byte)0xF0, (byte)0x80, 0, 0, 0};

		// ReadUIntPositive原语：负长度抛ISE，正常值透传（NioByteBuffer继承同一default）
		assertThrows(IllegalStateException.class, () -> ByteBuffer.Wrap(malicious.clone()).ReadUIntPositive());
		assertThrows(IllegalStateException.class,
				() -> NioByteBuffer.Wrap(malicious.clone(), malicious.length).ReadUIntPositive());
		var positive = ByteBuffer.Allocate();
		positive.WriteUInt(5);
		assertEquals(5, positive.ReadUIntPositive());

		// PMap1：Map系代表。修复前静默清空返回"空map"且decode成功，修复后抛ISE。
		var map = new Zeze.Transaction.Collections.PMap1<String, Long>(String.class, Long.class);
		map.put("a", 1L);
		map.put("b", 2L);
		assertThrows(IllegalStateException.class, () -> map.decode(ByteBuffer.Wrap(malicious.clone())));

		// PList2：try/catch(MethodHandle)包裹循环的变体，ISE经Task.forceThrow原样穿透。
		var list = new Zeze.Transaction.Collections.PList2<>(EmptyBean.class);
		list.add(new EmptyBean());
		assertThrows(IllegalStateException.class, () -> list.decode(ByteBuffer.Wrap(malicious.clone())));

		// 正常路径不变：encode/decode往返
		var ok = ByteBuffer.Allocate();
		var normal = new Zeze.Transaction.Collections.PMap1<String, Long>(String.class, Long.class);
		normal.put("x", 9L);
		normal.encode(ok);
		var restored = new Zeze.Transaction.Collections.PMap1<String, Long>(String.class, Long.class);
		restored.decode(ok);
		assertEquals(1, restored.size());
		assertEquals(9L, restored.get("x"));
	}
}
