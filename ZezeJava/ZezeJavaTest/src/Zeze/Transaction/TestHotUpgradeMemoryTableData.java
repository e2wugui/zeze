package Zeze.Transaction;

import static java.util.Objects.requireNonNull;

import org.jspecify.annotations.NullMarked;

import harness.FastServerIds;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.tools.ToolProvider;

import Zeze.Application;
import Zeze.Config;
import Zeze.Hot.HotUpgradeMemoryTable;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.SQLStatement;
import Zeze.Services.GlobalCacheManagerConst;
import Zeze.Transaction.Record1;
import Zeze.Util.TaskSpec;
import demo.Bean1;
import demo.Module1.tMemorySize;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND-T2-5 回归：内存表热更迁移 __direct_put_cache__ 的落盘语义。
 * 迁移记录不在任何rrs中，置脏后checkpoint永远flush不到它：数据永不落本地Rocks
 * （受限容量walkMemory为空），且cleanNow因脏记录清不掉而无限循环。
 * 迁移必须直接写入本地Rocks镜像且不置脏。
 */
@Fast
@NullMarked
public class TestHotUpgradeMemoryTableData {
	// 独立serverId隔离本地zeze_cache目录与其他@Fast测试（T2组：300/400/500/600起）。
	private static final AtomicInteger nextServerId = new AtomicInteger(FastServerIds.TEST_HOT_UPGRADE_MEMORY_TABLE_DATA);

	private static Application newApp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(nextServerId.getAndIncrement());
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("t2_memupgrade_test_" + conf.getServerId());
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		return new Application("TestHotUpgradeMemoryTableData@" + conf.getServerId(), conf);
	}

	private static Object getField(Object obj, String name) throws ReflectiveOperationException {
		for (Class<?> c = obj.getClass(); c != null; c = c.getSuperclass()) {
			try {
				Field f = c.getDeclaredField(name);
				f.setAccessible(true);
				return f.get(obj);
			} catch (NoSuchFieldException ignored) {
			}
		}
		throw new NoSuchFieldException(name);
	}

	private static Object invokeOn(Object obj, String name, Object... args) throws ReflectiveOperationException {
		for (Class<?> c = obj.getClass(); c != null; c = c.getSuperclass()) {
			for (Method m : c.getDeclaredMethods()) {
				if (m.getName().equals(name) && m.getParameterCount() == args.length) {
					m.setAccessible(true);
					return m.invoke(obj, args);
				}
			}
		}
		throw new NoSuchMethodException(name);
	}

	@SuppressWarnings("unchecked")
	private static Record1<Long, Bean1> getRecord(tMemorySize table, long key) throws ReflectiveOperationException {
		return (Record1<Long, Bean1>)invokeOn(requireNonNull(table.getCache()), "get", key);
	}

	@Test
	@SuppressWarnings("ConstantConditions") // walkMemory键契约非空，仍保留历史防御检查。
	public void testDirectPutCacheWritesLocalRocks() throws Exception {
		var app = newApp();
		var table = new tMemorySize();
		app.addTable("", table);
		app.start();
		try {
			var key = 7777L;
			var value = new Bean1();
			value.setV1(321);
			table.__direct_put_cache__(key, value, GlobalCacheManagerConst.StateModify);

			// 1. 核心：本地Rocks镜像有数据（受限容量walkMemory的数据源、软引用回收后的恢复源）
			var fromLocalRocks = table.getLocalRocksCacheTable().find(table, key);
			Assertions.assertNotNull(fromLocalRocks, "__direct_put_cache__必须写入本地Rocks镜像");
			Assertions.assertEquals(321, fromLocalRocks.getV1());

			// 2. 记录不置脏（不在rrs中的脏记录cleanNow永远清不掉：无限循环）
			var r = getRecord(table, key);
			Assertions.assertFalse((Boolean)invokeOn(r, "getDirty"), "迁移记录不得置脏");

			// 3. 受限容量内存表的walkMemory（走localRocks）可见
			Assertions.assertTrue(table.getTableConf().getRealCacheCapacity() >= 0); // sanity:默认受限
			var found = new boolean[] {false};
			table.walkMemory((k, v) -> {
				if (k != null && k == key && v.getV1() == 321)
					found[0] = true;
				return true;
			});
			Assertions.assertTrue(found[0], "受限容量walkMemory必须能遍历到迁移数据");

			// 4. cleanNow能清出dataMap（修复前dirty记录卡住，块永不为空循环清理），
			//    且清出后事务重新装载仍能得到数据（localRocks恢复源生效）。
			var key2 = 7778L;
			var value2 = new Bean1();
			value2.setV1(654);
			table.__direct_put_cache__(key2, value2, GlobalCacheManagerConst.StateModify);
			@SuppressWarnings("unchecked")
			var dataMap = (ConcurrentHashMap<Long, Record1<Long, Bean1>>)getField(requireNonNull(table.getCache()), "dataMap");
			Assertions.assertEquals(2, dataMap.size());

			table.getTableConf().setCacheCapacity(1);
			table.getTableConf().setCacheFactor(1); // realCacheCapacity=1 < 2 触发清理
			invokeOn(requireNonNull(table.getCache()), "newLruHot"); // 当前记录所在块退出热块
			requireNonNull(table.getCache()).cleanNow();

			Assertions.assertEquals(0, dataMap.size(), "非脏的迁移记录必须能被cleanNow清理");

			var got = new int[] {-1};
			var rc = TaskSpec.ofProcedure(app.newProcedure(() -> {
				var v = table.get(key);
				got[0] = v != null ? v.getV1() : -1;
				return 0L;
			}, "TestHotUpgradeMemoryTableData.reload")).call();
			Assertions.assertEquals(0L, rc);
			Assertions.assertEquals(321, got[0], "清出缓存后必须能从本地Rocks重新装载");
		} finally {
			app.stop();
		}
	}

	private static final class ChildFirstUrlClassLoader extends URLClassLoader {
		private final String prefix;

		ChildFirstUrlClassLoader(URL[] urls, ClassLoader parent, String prefix) {
			super(urls, parent);
			this.prefix = prefix;
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			if (name.startsWith(prefix)) {
				synchronized (getClassLoadingLock(name)) {
					var c = findLoadedClass(name);
					if (c == null)
						c = findClass(name);
					if (resolve)
						resolveClass(c);
					return c;
				}
			}
			return super.loadClass(name, resolve);
		}
	}

	private static Path compileHotBeanKeyTable(Path dir) throws Exception {
		var srcDir = dir.resolve("src");
		var outDir = dir.resolve("classes");
		Files.createDirectories(srcDir);
		Files.createDirectories(outDir);
		var keySrc = srcDir.resolve("BHotKey.java");
		var tableSrc = srcDir.resolve("tHotKeyMem.java");
		Files.writeString(keySrc, """
				package hot.memkey;
				import Zeze.Serialize.ByteBuffer;
				import Zeze.Serialize.IByteBuffer;
				import Zeze.Transaction.BeanKey;
				public final class BHotKey implements BeanKey, Comparable<BHotKey> {
					private final int a;
					private final String b;
					public BHotKey() { this(0, ""); }
					public BHotKey(int a, String b) { this.a = a; this.b = b; }
					public int getA() { return a; }
					public String getB() { return b; }
					@Override
					public void encode(ByteBuffer o) {
						int i = 0;
						if (a != 0) { i = o.WriteTag(i, 1, ByteBuffer.INTEGER); o.WriteInt(a); }
						if (!b.isEmpty()) { i = o.WriteTag(i, 2, ByteBuffer.BYTES); o.WriteString(b); }
						o.WriteByte(0);
					}
					public static BHotKey readFrom(IByteBuffer o) {
						int a = 0; String b = "";
						int t = o.ReadByte();
						int i = o.ReadTagSize(t);
						if (i == 1) { a = o.ReadInt(t); i += o.ReadTagSize(t = o.ReadByte()); }
						if (i == 2) { b = o.ReadString(t); i += o.ReadTagSize(t = o.ReadByte()); }
						while (t != 0) { o.SkipUnknownField(t); i += o.ReadTagSize(t = o.ReadByte()); }
						return new BHotKey(a, b);
					}
					@Override
					public int compareTo(BHotKey o) {
						int c = Integer.compare(a, o.a);
						return c != 0 ? c : b.compareTo(o.b);
					}
					@Override
					public boolean equals(Object o) {
						return o instanceof BHotKey k && a == k.a && b.equals(k.b);
					}
					@Override
					public int hashCode() { return 31 * a + b.hashCode(); }
				}
				""");
		Files.writeString(tableSrc, """
				package hot.memkey;
				import java.sql.ResultSet;
				import Zeze.Serialize.ByteBuffer;
				import Zeze.Serialize.SQLStatement;
				import Zeze.Transaction.TableX;
				import demo.Bean1;
				public final class tHotKeyMem extends TableX<BHotKey, Bean1> {
					public tHotKeyMem() { super(1888812345, "hot_memkey_tHotKeyMem"); }
					@Override public Class<BHotKey> getKeyClass() { return BHotKey.class; }
					@Override public Class<Bean1> getValueClass() { return Bean1.class; }
					@Override public boolean isMemory() { return true; }
					@Override public BHotKey decodeKey(ByteBuffer os) { return BHotKey.readFrom(os); }
					@Override public ByteBuffer encodeKey(BHotKey v) { var os = ByteBuffer.Allocate(16); v.encode(os); return os; }
					@Override public BHotKey decodeKeyResultSet(ResultSet s) { throw new UnsupportedOperationException(); }
					@Override public void encodeKeySQLStatement(SQLStatement s, BHotKey v) { throw new UnsupportedOperationException(); }
					@Override public Bean1 newValue() { return new Bean1(); }
				}
				""");
		var compiler = requireNonNull(ToolProvider.getSystemJavaCompiler());
		var cp = System.getProperty("java.class.path");
		try (var fm = compiler.getStandardFileManager(null, null, null)) {
			var units = fm.getJavaFileObjects(keySrc, tableSrc);
			var ok = compiler.getTask(null, fm, null, List.of("-cp", cp, "-d", outDir.toString()), null, units).call();
			Assertions.assertTrue(Boolean.TRUE.equals(ok), "热更 BeanKey 表编译必须成功");
		}
		return outDir;
	}

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void testCrossClassLoaderBeanKeyUpgradeMigratesDataAndDisablesOld(@TempDir Path tempDir) throws Exception {
		var classesDir = compileHotBeanKeyTable(tempDir);
		var urls = new URL[]{classesDir.toUri().toURL()};
		var parent = TestHotUpgradeMemoryTableData.class.getClassLoader();
		try (var oldCl = new ChildFirstUrlClassLoader(urls, parent, "hot.memkey.");
			 var curCl = new ChildFirstUrlClassLoader(urls, parent, "hot.memkey.")) {
			var oldKeyCls = oldCl.loadClass("hot.memkey.BHotKey");
			var curKeyCls = curCl.loadClass("hot.memkey.BHotKey");
			Assertions.assertNotSame(oldKeyCls, curKeyCls, "新旧热更 BeanKey 必须来自不同 ClassLoader");
			Assertions.assertEquals(oldKeyCls.getName(), curKeyCls.getName());

			TableX oldTable = (TableX)oldCl.loadClass("hot.memkey.tHotKeyMem").getDeclaredConstructor().newInstance();
			TableX curTable = (TableX)curCl.loadClass("hot.memkey.tHotKeyMem").getDeclaredConstructor().newInstance();

			var app = newApp();
			app.addTable("", oldTable);
			app.start();
			try {
				var oldKey1 = oldKeyCls.getDeclaredConstructor(int.class, String.class).newInstance(10, "alpha");
				var oldKey2 = oldKeyCls.getDeclaredConstructor(int.class, String.class).newInstance(20, "beta");
				var rc = TaskSpec.ofProcedure(app.newProcedure(() -> {
					var v1 = new Bean1();
					v1.setV1(111);
					oldTable.put((Comparable)oldKey1, v1);
					var v2 = new Bean1();
					v2.setV1(222);
					oldTable.put((Comparable)oldKey2, v2);
					return 0L;
				}, "TestHotUpgradeMemoryTableData.seedBeanKey")).call();
				Assertions.assertEquals(0L, rc);
				app.checkpointRun();

				curTable.open(oldTable, app);
				new HotUpgradeMemoryTable(oldTable, curTable).upgrade();

				Assertions.assertNull(oldTable.getCache(), "迁移完成后旧表必须被禁用");
				var curKey1 = curKeyCls.getDeclaredConstructor(int.class, String.class).newInstance(10, "alpha");
				var curKey2 = curKeyCls.getDeclaredConstructor(int.class, String.class).newInstance(20, "beta");
				var values = new int[]{-1, -1};
				rc = TaskSpec.ofProcedure(app.newProcedure(() -> {
					var r1 = (Bean1)curTable.get((Comparable)curKey1);
					var r2 = (Bean1)curTable.get((Comparable)curKey2);
					values[0] = r1 != null ? r1.getV1() : -1;
					values[1] = r2 != null ? r2.getV1() : -1;
					return 0L;
				}, "TestHotUpgradeMemoryTableData.verifyBeanKey")).call();
				Assertions.assertEquals(0L, rc);
				Assertions.assertEquals(111, values[0], "跨 ClassLoader BeanKey 记录 1 必须完整迁移");
				Assertions.assertEquals(222, values[1], "跨 ClassLoader BeanKey 记录 2 必须完整迁移");
			} finally {
				app.stop();
			}
		}
	}

	@Test
	public void testIncompatibleKeyTypeFailsFastAndKeepsOldEnabled() throws Exception {
		var app = newApp();
		var oldTable = new tMemorySize(); // Long 键内存表
		app.addTable("", oldTable);
		app.start();
		try {
			var rc = TaskSpec.ofProcedure(app.newProcedure(() -> {
				var v = new Bean1();
				v.setV1(42);
				oldTable.put(1L, v);
				return 0L;
			}, "TestHotUpgradeMemoryTableData.seedIncompatible")).call();
			Assertions.assertEquals(0L, rc);
			app.checkpointRun();

			var stringKeyTable = new TableX<String, Bean1>(oldTable.getId(), oldTable.getName()) {
				@Override
				public Class<String> getKeyClass() {
					return String.class;
				}

				@Override
				public Class<Bean1> getValueClass() {
					return Bean1.class;
				}

				@Override
				public boolean isMemory() {
					return true;
				}

				@Override
				public String decodeKey(ByteBuffer bb) {
					return bb.ReadString();
				}

				@Override
				public ByteBuffer encodeKey(String key) {
					var bb = ByteBuffer.Allocate();
					bb.WriteString(key);
					return bb;
				}

				@Override
				public String decodeKeyResultSet(ResultSet rs) {
					throw new UnsupportedOperationException();
				}

				@Override
				public void encodeKeySQLStatement(SQLStatement st, String key) {
					throw new UnsupportedOperationException();
				}

				@Override
				public Bean1 newValue() {
					return new Bean1();
				}
			};

			Assertions.assertThrows(IllegalStateException.class,
					() -> new HotUpgradeMemoryTable(oldTable, stringKeyTable).upgrade(),
					"Long 键表升级到 String 键表必须 fail-fast 抛 IllegalStateException");
			Assertions.assertNotNull(oldTable.getCache(), "不兼容中止后旧表必须保持启用状态");

			var got = new int[]{-1};
			rc = TaskSpec.ofProcedure(app.newProcedure(() -> {
				var v = oldTable.get(1L);
				got[0] = v != null ? v.getV1() : -1;
				return 0L;
			}, "TestHotUpgradeMemoryTableData.verifyOldKept")).call();
			Assertions.assertEquals(0L, rc);
			Assertions.assertEquals(42, got[0], "不兼容中止后旧表数据仍可正常读取");
		} finally {
			app.stop();
		}
	}
}
