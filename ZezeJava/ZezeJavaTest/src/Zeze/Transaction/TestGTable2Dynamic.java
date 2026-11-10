package Zeze.Transaction;

import static java.util.Objects.requireNonNull;

import java.util.LinkedHashMap;
import java.util.Map;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.DynamicBean;
import Zeze.Transaction.GTable.GTable2;
import Zeze.Util.Json;
import dynamicred.Demo.Amount;
import dynamicred.Demo.Order;
import dynamicred.Demo.Quantity;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证手写 GTable2 的动态值工厂和 JSON 往返。 */
@Fast
public class TestGTable2Dynamic {

	private static final long TYPE_MYBEAN = 101L;
	private abstract static class Fields extends Bean {
	}

	private static final DynamicField FIELD = DynamicField.declare(Fields.class, Bean.hash64(Fields.class.getName()),
			1, b -> TYPE_MYBEAN, id -> id == TYPE_MYBEAN ? new MDynBean() : null, MDynBean.class);
	private static final GTable2.Factory<String, Long, DynamicBean, DynamicBean> FACTORY =
			GTable2.getFactory(String.class, Long.class, FIELD);

	/** 纯字段bean（无事务逻辑，未管理环境直接读写）。 */
	public static final class MDynBean extends Bean {
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

	public static class HDyn {
		final GTable2<String, Long, DynamicBean, DynamicBean> g = new GTable2<>(FACTORY);
	}

	// A1：带工厂的重载可构造，put/get正常。
	@Test
	public void testDynamicFactoryOverload() {
		var h = new HDyn();
		var dyn = new DynamicBean(FIELD);
		var bean = new MDynBean();
		bean.v = 42;
		dyn.setBean(bean);
		h.g.put("r1", 2L, dyn);

		var v = h.g.get("r1", 2L);
		assertNotNull(v, "工厂路径put/get正常");
		assertEquals(42L, ((MDynBean)v.getBean()).v);
	}

	// 三参版对DynamicBean指名拒绝（原实现深抛不带dynamic信息的NoSuchMethodException）。
	@Test
	public void testThreeArgFactoryNamedRejection() {
		var e = assertThrows(IllegalArgumentException.class,
				() -> GTable2.getFactory(String.class, Long.class, DynamicBean.class),
				"三参版对DynamicBean必须指名拒绝");
		assertTrue(e.getMessage().contains("DynamicBean") && e.getMessage().contains("dynamicField"),
				"报错必须指名dynamic与工厂重载，实际: " + e.getMessage());
	}

	// A4：dynamic值的JSON往返（parser补BeanMap2 parent适配，原实现null value崩溃）。
	@Test
	public void testDynamicValueJsonRoundTrip() {
		var h = new HDyn();
		var dyn = new DynamicBean(FIELD);
		var bean = new MDynBean();
		bean.v = 7;
		dyn.setBean(bean);
		h.g.put("r1", 2L, dyn);

		var json = Json.toCompactString(h);
		var parsed = (HDyn)Json.parse(json, new HDyn());
		var v = requireNonNull(parsed).g.get("r1", 2L);
		assertNotNull(v, "dynamic值必须经JSON往返恢复（原实现put(null)崩溃）");
		assertNotNull(v.getBean(), "内部bean必须恢复");
		assertEquals(7L, ((MDynBean)v.getBean()).v);
	}

	@Test
	public void rejectsForeignRowMetaBeforeChangingTheOuterMap() {
		var order = new Order();
		var amounts = order.getAmountGrid().getPMap2();
		var quantities = order.getQuantityGrid().getPMap2();
		var foreign = amounts.createValue();
		var amount = Order.newDynamicBean_AmountGrid();
		amount.setBean(new Amount(43));
		foreign.put(1, amount);
		assertThrows(IllegalArgumentException.class, () -> quantities.put(1, foreign));
		assertNull(foreign.mapKey());

		var valid = quantities.createValue();
		var quantity = Order.newDynamicBean_QuantityGrid();
		quantity.setBean(new Quantity(44));
		valid.put(1, quantity);
		var batch = new LinkedHashMap<>(Map.of(2, valid));
		batch.put(3, foreign);
		assertThrows(IllegalArgumentException.class, () -> quantities.putAll(batch));
		assertTrue(quantities.isEmpty());
		assertNull(valid.mapKey());
		assertNull(foreign.mapKey());

		quantities.put(2, valid);
		amounts.put(3, foreign);
		assertThrows(IllegalArgumentException.class, () -> quantities.assign(amounts));
		assertEquals(1, quantities.size());
		assertSame(valid, quantities.get(2));
	}
}
