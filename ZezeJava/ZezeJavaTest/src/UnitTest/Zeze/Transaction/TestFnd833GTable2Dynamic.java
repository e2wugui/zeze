package UnitTest.Zeze.Transaction;

import java.util.function.LongFunction;
import java.util.function.ToLongFunction;

import Zeze.Builtin.HotDistribute.BVariable;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.History.Helper;
import Zeze.Transaction.Bean;
import Zeze.Transaction.DynamicBean;
import Zeze.Transaction.GTable.GTable2;
import Zeze.Util.Json;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND8-33回归（A1/A3/A4）：GTable2对dynamic值无构造路径——getFactory对
 * DynamicBean深抛无信息的NoSuchMethodException（无无参构造器）；JSON解析的
 * DynamicBean parser不认BeanMap2容器（返回null撞PMap2.put的null value）；
 * History.dependsGTable的dynamic分支先调三参版getFactory必抛。
 * 修复：GTable2增加带get/create工厂的重载（对齐PMap2判例，不进按类缓存）；
 * 三参版对DynamicBean指名拒绝；Helper先取工厂再经重载构建；Json的DynamicBean
 * parser补BeanMap2 parent适配。
 * （A2——生成器侧gtable+dynamic的发射修复——需改C#生成器并再生产物，已记档上报。）
 * 纯单元：手写宿主直驱工厂/JSON往返/dependsGTable。
 */
@Fast
public class TestFnd833GTable2Dynamic {

	private static final long TYPE_MYBEAN = 101L;
	private static final ToLongFunction<Bean> GET = b -> TYPE_MYBEAN;
	private static final LongFunction<Bean> CREATE = id -> id == TYPE_MYBEAN ? new MDynBean() : null;

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
		final GTable2<String, Long, DynamicBean, DynamicBean> g =
				new GTable2<>(GTable2.getFactory(String.class, Long.class, GET, CREATE));
	}

	/** dependsGTable反射查找的宿主形态（生成newDynamicBean_Xxx同形态）。 */
	public static final class HostG {
		public static DynamicBean newDynamicBean_Gtable() {
			return new DynamicBean(1, GET, CREATE);
		}
	}

	// A1：带工厂的重载可构造，put/get正常。
	@Test
	public void testDynamicFactoryOverload() {
		var h = new HDyn();
		var dyn = new DynamicBean(1, GET, CREATE);
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
		assertTrue(e.getMessage().contains("DynamicBean") && e.getMessage().contains("get, create"),
				"报错必须指名dynamic与工厂重载，实际: " + e.getMessage());
	}

	// A4：dynamic值的JSON往返（parser补BeanMap2 parent适配，原实现null value崩溃）。
	@Test
	public void testDynamicValueJsonRoundTrip() {
		var h = new HDyn();
		var dyn = new DynamicBean(1, GET, CREATE);
		var bean = new MDynBean();
		bean.v = 7;
		dyn.setBean(bean);
		h.g.put("r1", 2L, dyn);

		var json = Json.toCompactString(h);
		var parsed = (HDyn)Json.parse(json, new HDyn());
		var v = parsed.g.get("r1", 2L);
		assertNotNull(v, "dynamic值必须经JSON往返恢复（原实现put(null)崩溃）");
		assertNotNull(v.getBean(), "内部bean必须恢复");
		assertEquals(7L, ((MDynBean)v.getBean()).v);
	}

	// A3：History.dependsGTable的dynamic分支先取工厂再经重载构建（原实现必抛）。
	@Test
	public void testDependsGTableDynamic() throws Exception {
		var v = new BVariable.Data();
		v.setId(1);
		v.setName("gtable");
		v.setType("gtable");
		v.setKey("string,long");
		v.setValue("dynamic");

		var result = new Helper.DependsResult();
		assertDoesNotThrow(() -> Helper.dependsGTable(HostG.class, v, "string", "long", "dynamic", result),
				"dependsGTable对dynamic值不得再抛（原实现三参版getFactory必抛）");
		assertTrue(result.map2Metas.size() >= 1, "pmapMeta必须注册");
		assertEquals(1, result.map2Dynamic.size(), "dynamic家族按(keyClass,DynamicBean)登记");
	}
}
