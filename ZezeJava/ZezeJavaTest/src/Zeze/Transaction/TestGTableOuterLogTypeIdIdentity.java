package Zeze.Transaction;

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
import dynamicred.Demo.Order;
import harness.Fast;

/**
 * coll-01根治钉板：GTable外层logTypeId/name由(row,col,val)完整身份参与（GTable1/GTable2
 * 各自专用家族头）。修复前外层typeId=hashLog(家族头,rowClass,擦除BeanMap1/2.class)——同kind
 * 同rowClass不同列/值类型的两个GTable共享typeId，History注册先到先得+同名仅debug留痕，
 * 回放端LogRegistry.decode拿到第一个表的行工厂解码第二个表的整行日志（跨wire家族中断回放/
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
	}

	/** 构造整行替换日志（GTable外层PMap2的行写入形态）并编码。 */
	private static <R, C, V> ByteBuffer encodeReplacedRow(GTable1.Factory<R, C, V> f, R rowKey, C col, V val) {
		var row = new BeanMap1<>(f.getBmapMeta());
		row.put(col, val);
		var log = Map2TestSupport.emptyWirePayload(
			new LogMap2<R, BeanMap1<C, V>>(1, null, f.getPmapMeta(), null));
		log.getModified().put(rowKey, row);
		var bb = ByteBuffer.Allocate();
		log.encode(bb);
		return bb;
	}

	@Test
	public void testReplayOwnFactoryCrossWireFamily() {
		// audit触发序列反转：t2=GTable1<long,long,int>整行写入，回放端必须用t2自己的行工厂解码。
		// 修复前：t2.typeId==t1(列string).typeId，LogRegistry.decode拿到t1工厂→ReadString(tag=INTEGER)抛
		// IllegalStateException，回放批中断。
		var fStr = GTable1.getFactory(long.class, String.class, Integer.class);
		var fLong = GTable1.getFactory(long.class, Long.class, Integer.class);
		Helper.registerLogMap2Meta(fStr.getPmapMeta());
		Helper.registerLogMap2Meta(fLong.getPmapMeta());

		var bb = encodeReplacedRow(fLong, 1L, 7L, 42);
		var decoded = Assertions.assertDoesNotThrow(() -> Zeze.Transaction.LogRegistry.decode(fLong.getPmapMeta().logTypeId, 1, bb),
			"跨wire家族：long列的字节不得再被string行工厂解码（修复前抛IllegalStateException）");
		@SuppressWarnings("unchecked")
		var decodedRow = ((LogMap2<Long, BeanMap1<Long, Integer>>)decoded).getModified().get(1L);
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
		var decoded = Assertions.assertDoesNotThrow(() -> Zeze.Transaction.LogRegistry.decode(fLongVal.getPmapMeta().logTypeId, 1, bb));
		@SuppressWarnings("unchecked")
		var decodedRow = ((LogMap2<Long, BeanMap1<Long, Long>>)decoded).getModified().get(1L);
		Assertions.assertNotNull(decodedRow, "整行必须恢复");
		Assertions.assertEquals(4_294_967_296L, decodedRow.get(7L),
			"超int域的long值必须无损（修复前int行工厂静默截断为0）");
	}

	@Test
	public void testDynamicOuterTypeIdSharedByDesign() {
		// 同一行列类型的动态 GTable 共享日志类型号，回放按字段身份选择 Meta。
		var order = new Order();
		var d1 = order.getAmountGrid().getPMap2().getMeta();
		var d2 = order.getQuantityGrid().getPMap2().getMeta();
		Assertions.assertEquals(d1.logTypeId, d2.logTypeId);

		var real = GTable2.getFactory(Integer.class, Integer.class, B1.class);
		Assertions.assertNotEquals(real.getPmapMeta().logTypeId, d1.logTypeId,
			"dynamic与真实值bean的外层typeId必须分流");
	}
}
