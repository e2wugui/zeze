package UnitTest.Zeze.Transaction;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.GTable.GTable1;
import Zeze.Transaction.GTable.GTable2;
import Zeze.Util.Json;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND7-10：GTable1/GTable2 Json解析的Factory.fm1/fm2惰性初始化只校验fm1且两写分离，
 * 并发首次解析共享同一Factory时可观察到"fm1已写、fm2未写"的部分状态，parseMap0解引用
 * null直接NPE中断解析。修复：fm1/fm2在Factory构造期一次性构建并声明final，经factories
 * 的ConcurrentHashMap安全发布，根除惰性双写竞态。
 */
@Fast
public class TestFnd710GTableJsonFactoryInit {
	// 纯单元：Json解析不依赖应用环境。标记bean保证每轮的(R,C,V)三元组全局唯一，
	// 每轮拿到独立的Factory（未初始化/全新构造，前置断言保证）。

	public static class M1 extends Marker {
	}

	public static class M2 extends Marker {
	}

	public static class M3 extends Marker {
	}

	public static abstract class Marker extends Bean {
		public long v;
		@SuppressWarnings("FieldNameHidesFieldInSuperclass")
		private transient Object mapKey;

		public @NotNull Marker set(long x) {
			v = x;
			return this;
		}

		@Override
		public Object mapKey() {
			return mapKey;
		}

		@Override
		public void mapKey(@NotNull Object mapKey) {
			this.mapKey = mapKey;
		}

		@Override
		public void encode(@NotNull ByteBuffer bb) {
			bb.WriteLong(v);
		}

		@Override
		public void decode(@NotNull IByteBuffer bb) {
			v = bb.ReadLong();
		}
	}

	public static class H1 {
		final GTable2<Integer, Long, M1, M1> g = new GTable2<>(Integer.class, Long.class, M1.class);
	}

	public static class H2 {
		final GTable2<Integer, Long, M2, M2> g = new GTable2<>(Integer.class, Long.class, M2.class);
	}

	public static class H3 {
		final GTable2<Integer, Long, M3, M3> g = new GTable2<>(Integer.class, Long.class, M3.class);
	}

	public static class H4 {
		// V用float走MAP+FLOAT定型路径：GTable1的fm2对Bean值klass为Object.class
		// （原实现即如此，Bean值GTable1由GTable2承担），不与该历史行为耦合。
		final GTable1<Integer, Long, Float> g = new GTable1<>(Integer.class, Long.class, Float.class);
	}

	private static final int POLLERS = 4;

	/** 发起者解析触发（修复前的）惰性初始化；观察者轮询fm1可见后立即解析。 */
	private static void raceRound(Field fm1Field, Object factory, String json, Runnable parse) throws Exception {
		if (Modifier.isFinal(fm1Field.getModifiers()))
			Assertions.assertNotNull(fm1Field.get(factory), "final形态：构造期必须已完成初始化");
		else
			Assertions.assertNull(fm1Field.get(factory), "惰性形态前置：本轮Factory必须未初始化");
		var start = new CountDownLatch(1);
		var executor = (ExecutorService)Executors.newFixedThreadPool(1 + POLLERS);
		try {
			var futures = new ArrayList<Future<?>>();
			futures.add(executor.submit(() -> {
				start.await();
				parse.run();
				return null;
			}));
			for (int i = 0; i < POLLERS; i++)
				futures.add(executor.submit(() -> {
					start.await();
					while (fm1Field.get(factory) == null)
						Thread.onSpinWait();
					parse.run(); // 修复前可携fm2==null进入parseMap0（NPE）
					return null;
				}));
			start.countDown();
			for (var f : futures)
				f.get(30, TimeUnit.SECONDS); // 任何线程的NPE/断言失败都在此抛出
		} finally {
			executor.shutdownNow();
		}
	}

	private static Field fm1Field(Class<?> factoryClass) throws NoSuchFieldException {
		var f = factoryClass.getDeclaredField("fm1");
		f.setAccessible(true);
		return f;
	}

	@Test
	public void testConcurrentFirstParseGTable2() throws Exception {
		var fm1 = fm1Field(GTable2.Factory.class);

		var h1 = new H1();
		h1.g.put(1, 2L, (M1)new M1().set(3));
		var json1 = Json.toCompactString(h1);
		raceRound(fm1, GTable2.getFactory(Integer.class, Long.class, M1.class), json1, () -> {
			var parsed = (H1)Json.parse(json1, new H1());
			var m = parsed.g.get(1, 2L);
			Assertions.assertNotNull(m, "并发首解析后数据必须恢复");
			Assertions.assertEquals(3L, m.v);
		});

		var h2 = new H2();
		h2.g.put(1, 2L, (M2)new M2().set(4));
		var json2 = Json.toCompactString(h2);
		raceRound(fm1, GTable2.getFactory(Integer.class, Long.class, M2.class), json2, () -> {
			var parsed = (H2)Json.parse(json2, new H2());
			var m = parsed.g.get(1, 2L);
			Assertions.assertNotNull(m);
			Assertions.assertEquals(4L, m.v);
		});

		var h3 = new H3();
		h3.g.put(1, 2L, (M3)new M3().set(5));
		var json3 = Json.toCompactString(h3);
		raceRound(fm1, GTable2.getFactory(Integer.class, Long.class, M3.class), json3, () -> {
			var parsed = (H3)Json.parse(json3, new H3());
			var m = parsed.g.get(1, 2L);
			Assertions.assertNotNull(m);
			Assertions.assertEquals(5L, m.v);
		});
	}

	@Test
	public void testConcurrentFirstParseGTable1() throws Exception {
		var fm1 = fm1Field(GTable1.Factory.class);
		var h4 = new H4();
		h4.g.put(1, 2L, 3.5f);
		var json4 = Json.toCompactString(h4);
		raceRound(fm1, GTable1.getFactory(Integer.class, Long.class, Float.class), json4, () -> {
			var parsed = (H4)Json.parse(json4, new H4());
			var m = parsed.g.get(1, 2L);
			Assertions.assertNotNull(m);
			Assertions.assertEquals(3.5f, m);
		});
	}

	@Test
	public void testFieldsAreFinalLockingInSafePublication() throws Exception {
		// 机制锁定：fm1/fm2为final（构造期赋值+CHM安全发布=观察者不可能见到部分初始化）。
		for (var c : List.of(GTable1.Factory.class, GTable2.Factory.class))
			for (var name : List.of("fm1", "fm2"))
				Assertions.assertTrue(Modifier.isFinal(c.getDeclaredField(name).getModifiers()),
						c.getSimpleName() + '.' + name + "必须为final（构造期构建）");
	}

	@Test
	public void testPartialInitStateMustNotBreakParse() throws Exception {
		// 并发首解析缺陷机制的直接复现：观察者见到"fm1已写、fm2未写"的中间态时，
		// 解析在parseMap0解引用null直接NPE。修复前（非final字段）用反射强制构造出该
		// 中间态并要求解析不崩溃（红：NPE）；修复后fm1/fm2为final构造期赋值，该中间态
		// 无法构造也无从出现，跳过强制段（绿）。
		var fm1Field = GTable2.Factory.class.getDeclaredField("fm1");
		var fm2Field = GTable2.Factory.class.getDeclaredField("fm2");
		fm1Field.setAccessible(true);
		fm2Field.setAccessible(true);
		if (Modifier.isFinal(fm1Field.getModifiers()))
			return; // 修复后形态：部分初始化状态不可构造。

		var factory = GTable2.getFactory(Integer.class, Long.class, M1.class);
		var savedFm1 = fm1Field.get(factory);
		var savedFm2 = fm2Field.get(factory);
		var h = new H1();
		h.g.put(1, 2L, (M1)new M1().set(3));
		var json = Json.toCompactString(h);
		try {
			// 与生产初始化块同款fm1；fm2保持null=并发窗口内观察者所见。
			var dummyField = GTable2.class.getDeclaredField("pMap2");
			fm1Field.set(factory, new Json.FieldMeta(0x3c, 0, "PMap2", Zeze.Transaction.GTable.BeanMap2.class,
					factory::get, Json.ClassMeta.getKeyReader(Integer.class), dummyField));
			fm2Field.set(factory, null);
			Assertions.assertDoesNotThrow(() -> Json.parse(json, new H1()),
					"fm1可见而fm2未写时解析不得崩溃（修复前NPE于parseMap0）");
		} finally {
			fm1Field.set(factory, savedFm1);
			fm2Field.set(factory, savedFm2);
		}
	}
}
