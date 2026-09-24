package UnitTest.Zeze.Transaction;

import java.util.function.LongFunction;
import java.util.function.ToLongFunction;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import Zeze.History.Helper;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Collections.LogMap2;
import Zeze.Transaction.GTable.BeanMap1;
import Zeze.Transaction.GTable.GTable1;
import Zeze.Transaction.GTable.GTable2;
import Zeze.Transaction.Log;
import harness.Fast;

/**
 * coll-01根治钉板：GTable外层logTypeId/name由(row,col,val)完整身份参与（GTable1/GTable2
 * 各自专用家族头）。修复前外层typeId=hashLog(家族头,rowClass,擦除BeanMap1/2.class)——同kind
 * 同rowClass不同列/值类型的两个GTable共享typeId，History注册先到先得+同名仅debug留痕，
 * 回放端Log.create拿到第一个表的行工厂解码第二个表的整行日志（跨wire家族中断回放/
 * 同数值家族静默有损转换）。History无生产启用（用户裁定2026-09-24），typeId直接切换，
 * 无存量日志兼容问题；RocksRaft不支持gtable同样不在兼容面内。
 * 766b409da的最小防御fail-fast（checkGTableIdentity）已随根治移除：合法schema不再误伤。
 */
@Fast
public class TestGTableOuterLogTypeIdIdentity {

	/** 测试值bean（三参版需要无参构造器）。 */
	public static final class B1 extends Bean {
		public long v;

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteLong(v);
		}

		@Override
		public void decode(IByteBuffer bb) {
			v = bb.ReadLong();
		}
	}

	/** 测试值bean（与B1不同类，验证GTable2外层按值bean类分化）。 */
	public static final class B2 extends Bean {
		public long v;

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteLong(v);
		}

		@Override
		public void decode(IByteBuffer bb) {
			v = bb.ReadLong();
		}
	}

	@Test
	public void testSameRowDifferentColValueTypeIdsDiverge() {
		// GTable1：同row不同列类型（跨wire家族 string vs long）
		var fStr = GTable1.getFactory(long.class, String.class, Integer.class);
		var fLongCol = GTable1.getFactory(long.class, Long.class, Integer.class);
		Assertions.assertNotEquals(fStr.getPmapMeta().logTypeId, fLongCol.getPmapMeta().logTypeId,
				"同row不同列类型的两个GTable1外层typeId必须分化");
		Assertions.assertNotEquals(fStr.getPmapMeta().name, fLongCol.getPmapMeta().name,
				"外层name必须分化（排错可辨）");
		Assertions.assertTrue(fStr.getPmapMeta().name.contains("string")
				&& fLongCol.getPmapMeta().name.contains("long"),
				"name必须含列类型身份，实际: " + fStr.getPmapMeta().name + " / " + fLongCol.getPmapMeta().name);

		// GTable1：同row同col不同值类型（同数值家族 int vs long）
		var fIntVal = GTable1.getFactory(long.class, Long.class, Integer.class);
		var fLongVal = GTable1.getFactory(long.class, Long.class, Long.class);
		Assertions.assertNotEquals(fIntVal.getPmapMeta().logTypeId, fLongVal.getPmapMeta().logTypeId,
				"同row同col不同值类型的两个GTable1外层typeId必须分化");

		// GTable2：同row同col不同值bean
		var gB1 = GTable2.getFactory(long.class, String.class, B1.class);
		var gB2 = GTable2.getFactory(long.class, String.class, B2.class);
		Assertions.assertNotEquals(gB1.getPmapMeta().logTypeId, gB2.getPmapMeta().logTypeId,
				"同row同col不同值bean的两个GTable2外层typeId必须分化");

		// 双注册各得其所（模拟History.registerAllTableLogs→回放端Log.create按typeId查表）
		Helper.registerLogMap2Meta(fStr.getPmapMeta());
		Helper.registerLogMap2Meta(fLongCol.getPmapMeta());
		Assertions.assertEquals(fStr.getPmapMeta().logTypeId,
				Log.create(fStr.getPmapMeta().logTypeId, 1).getTypeId(),
				"修复前第二个注册被先到先得吞掉，两typeId查表同工厂");
		Assertions.assertEquals(fLongCol.getPmapMeta().logTypeId,
				Log.create(fLongCol.getPmapMeta().logTypeId, 1).getTypeId());
	}

	/** 构造整行replaced日志（GTable外层PMap2的行写入形态）并编码。 */
	private static <R, C, V> ByteBuffer encodeReplacedRow(GTable1.Factory<R, C, V> f, R rowKey, C col, V val) {
		var row = new BeanMap1<>(f.getBmapMeta());
		row.put(col, val);
		var log = new LogMap2<R, BeanMap1<C, V>>(null, 1, null, org.pcollections.Empty.map(), f.getPmapMeta());
		log.getReplaced().put(rowKey, row);
		var bb = ByteBuffer.Allocate();
		log.encode(bb);
		return bb;
	}

	@Test
	public void testReplayOwnFactoryCrossWireFamily() {
		// audit触发序列反转：t2=GTable1<long,long,int>整行写入，回放端必须用t2自己的行工厂解码。
		// 修复前：t2.typeId==t1(列string).typeId，Log.create拿到t1工厂→ReadString(tag=INTEGER)抛
		// IllegalStateException，回放批中断。
		var fStr = GTable1.getFactory(long.class, String.class, Integer.class);
		var fLong = GTable1.getFactory(long.class, Long.class, Integer.class);
		Helper.registerLogMap2Meta(fStr.getPmapMeta());
		Helper.registerLogMap2Meta(fLong.getPmapMeta());

		var bb = encodeReplacedRow(fLong, 1L, 7L, 42);
		var decoded = Log.create(fLong.getPmapMeta().logTypeId, 1);
		Assertions.assertDoesNotThrow(() -> decoded.decode(bb),
				"跨wire家族：long列的字节不得再被string行工厂解码（修复前抛IllegalStateException）");
		@SuppressWarnings("unchecked")
		var decodedRow = ((LogMap2<Long, BeanMap1<Long, Integer>>)decoded).getReplaced().get(1L);
		Assertions.assertNotNull(decodedRow, "整行必须恢复");
		Assertions.assertEquals(42, decodedRow.get(7L), "行数据必须无损");
	}

	@Test
	public void testReplayNumericFamilyLossless() {
		// 同数值家族（值int vs long）：修复前共享typeId，int行工厂解long值静默截断（主从/历史分歧
		// 潜伏，未开Verify对账则永久）——钉超int域值2^32的有损点。
		var fInt = GTable1.getFactory(long.class, Long.class, Integer.class);
		var fLongVal = GTable1.getFactory(long.class, Long.class, Long.class);
		Helper.registerLogMap2Meta(fInt.getPmapMeta());
		Helper.registerLogMap2Meta(fLongVal.getPmapMeta());

		var bb = encodeReplacedRow(fLongVal, 1L, 7L, 4_294_967_296L);
		var decoded = Log.create(fLongVal.getPmapMeta().logTypeId, 1);
		Assertions.assertDoesNotThrow(() -> decoded.decode(bb));
		@SuppressWarnings("unchecked")
		var decodedRow = ((LogMap2<Long, BeanMap1<Long, Long>>)decoded).getReplaced().get(1L);
		Assertions.assertNotNull(decodedRow, "整行必须恢复");
		Assertions.assertEquals(4_294_967_296L, decodedRow.get(7L),
				"超int域的long值必须无损（修复前int行工厂静默截断为0）");
	}

	@Test
	public void testDynamicOuterIdentityEquivalentContract() {
		// FND8-33等价契约钉板：dynamic值身份固定DynamicBean（内层DYNAMIC编码自描述，解码不依赖
		// get/create闭包）——同(row,col)的两个dynamic变量外层typeId相同是合法共享；真实值类型的
		// 三参版则与dynamic分流。
		var get1 = (ToLongFunction<Bean>)b -> 101L;
		var create1 = (LongFunction<Bean>)id -> id == 101L ? new B1() : null;
		var get2 = (ToLongFunction<Bean>)b -> 102L;
		var create2 = (LongFunction<Bean>)id -> id == 102L ? new B2() : null;
		var d1 = GTable2.getFactory(String.class, Long.class, get1, create1);
		var d2 = GTable2.getFactory(String.class, Long.class, get2, create2);
		Assertions.assertEquals(d1.getPmapMeta().logTypeId, d2.getPmapMeta().logTypeId,
				"同(row,col)的dynamic变量外层typeId相同（值身份固定DynamicBean的等价契约）");

		var real = GTable2.getFactory(String.class, Long.class, B1.class);
		Assertions.assertNotEquals(real.getPmapMeta().logTypeId, d1.getPmapMeta().logTypeId,
				"dynamic与真实值bean的外层typeId必须分流");
		// dynamic值的行roundtrip属FND8-33钉板范围（TestFnd833GTable2Dynamic），此处只钉外层typeId契约。
	}
}
