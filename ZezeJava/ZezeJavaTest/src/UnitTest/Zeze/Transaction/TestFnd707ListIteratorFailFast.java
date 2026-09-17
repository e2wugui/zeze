package UnitTest.Zeze.Transaction;

import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.ListIterator;

import Zeze.Transaction.Collections.PList1;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND7-07：PList.listIterator迭代期间发生外部结构性修改后，remove()/set()仅凭lastRet
 * 下标操作，未越界的错位场景静默删错/改错元素——未对齐iterator()的身份fail-fast惯例。
 * 修复：next()/previous()记录返回引用，remove()/set()前校验当前列表lastRet位仍是该引用，
 * 不符抛CME（身份相等是按下标操作不会错位的充分条件，与iterator()语义一致）。
 */
@Fast
public class TestFnd707ListIteratorFailFast {
	// 纯单元：非受管PList1直接操作底层pcollections数据，不需要应用环境。

	private static PList1<Integer> newList(Integer... items) {
		var list = new PList1<>(Integer.class);
		list.addAll(List.of(items));
		return list;
	}

	@Test
	public void testRemoveAfterExternalShiftFailFast() {
		// [a,b,c]：next()==1后外部删除更前的元素导致左移，remove()按下标会删掉2而非1。
		var list = newList(1, 2, 3);
		ListIterator<Integer> li = list.listIterator();
		Assertions.assertEquals(1, li.next()); // lastReturned=1，lastRet=0
		list.remove(Integer.valueOf(1)); // 外部结构性修改：[2,3]，下标0已是2
		Assertions.assertThrows(ConcurrentModificationException.class, li::remove,
				"错位场景必须fail-fast而不是静默删错元素");
		Assertions.assertEquals(List.of(2, 3), list.getList(), "fail-fast不得改动列表");
	}

	@Test
	public void testSetAfterExternalShiftFailFast() {
		var list = newList(1, 2, 3);
		ListIterator<Integer> li = list.listIterator();
		Assertions.assertEquals(1, li.next());
		Assertions.assertEquals(2, li.next()); // lastReturned=2，lastRet=1
		list.remove(Integer.valueOf(1)); // [2,3]，下标1已是3
		Assertions.assertThrows(ConcurrentModificationException.class, () -> li.set(9),
				"set()同型错改必须fail-fast");
		Assertions.assertEquals(List.of(2, 3), list.getList());
	}

	@Test
	public void testRemoveOutOfBoundConvertsToCme() {
		// next()后外部把lastRet位置及之后全删：越界仍按原约定转CME（保留原分支语义）。
		var list = newList(1, 2, 3);
		ListIterator<Integer> li = list.listIterator();
		Assertions.assertEquals(1, li.next());
		Assertions.assertEquals(2, li.next());
		Assertions.assertEquals(3, li.next()); // lastRet=2
		list.remove(Integer.valueOf(1));
		list.remove(Integer.valueOf(2));
		list.remove(Integer.valueOf(3)); // []
		Assertions.assertThrows(ConcurrentModificationException.class, li::remove);
	}

	@Test
	public void testNoModificationBehaviorUnchanged() {
		// 无结构性修改时行为不变：正向遍历remove清空、set替换、IllegalState语义。
		var list = newList(1, 2, 3, 4);
		ListIterator<Integer> li = list.listIterator();
		Assertions.assertThrows(IllegalStateException.class, li::remove); // 未next先remove
		Assertions.assertEquals(1, li.next());
		li.remove();
		Assertions.assertThrows(IllegalStateException.class, li::remove); // 同一元素连续remove
		Assertions.assertEquals(3, list.size());

		var list2 = newList(1, 2, 3);
		ListIterator<Integer> li2 = list2.listIterator();
		li2.next();
		li2.set(9);
		li2.set(8); // 连续set是JDK允许的：校验基准随set更新，不得误抛CME
		Assertions.assertEquals(List.of(8, 2, 3), list2.getList());
		li2.remove(); // set后remove仍按当前引用校验通过
		Assertions.assertEquals(List.of(2, 3), list2.getList());
	}

	@Test
	public void testPreviousAndSafeExternalModify() {
		// previous()同样记录返回引用参与校验；不引起lastRet错位的外部修改（删更后的元素）
		// 按下标操作仍恰好正确，身份比较通过放行——与iterator()设计语义一致。
		var list = newList(1, 2, 3, 4);
		ListIterator<Integer> li = list.listIterator(2);
		Assertions.assertEquals(2, li.previous()); // lastReturned=2，lastRet=1
		list.remove(Integer.valueOf(4)); // [1,2,3]：下标1仍是2
		li.set(9);
		Assertions.assertEquals(List.of(1, 9, 3), list.getList());
		li.remove();
		Assertions.assertEquals(List.of(1, 3), list.getList());

		// previous()后错位同样fail-fast
		var list2 = newList(1, 2, 3);
		ListIterator<Integer> li2 = list2.listIterator(2);
		Assertions.assertEquals(2, li2.previous()); // lastRet=1
		list2.remove(Integer.valueOf(1)); // [2,3]：下标1已是3
		Assertions.assertThrows(ConcurrentModificationException.class, li2::remove);
	}

	@Test
	public void testManagedPathFailFast(@org.junit.jupiter.api.io.TempDir java.nio.file.Path tempDir) throws Exception {
		// 托管路径（事务内）：getList()经事务日志读取当前值，身份校验同样生效。
		var config = new Zeze.Config();
		config.setServerId(7070); // 缓存目录zeze_cache_<serverId>按serverId命名：默认0会与同JVM其他默认App互撞（start先删后开，LOCK被持即删失败）
		config.setServiceManager("disable");
		config.setDefaultTableConf(new Zeze.Config.TableConf());
		var dbConf = new Zeze.Config.DatabaseConf();
		dbConf.setDatabaseType(Zeze.Config.DbType.RocksDb);
		dbConf.setDatabaseUrl(tempDir.resolve("dbhome").toString());
		config.getDatabaseConfMap().put("", dbConf);
		var app = new Zeze.Application("TestFnd707ListIteratorFailFast", config);
		var table = new demo.Module1.tflush();
		app.addTable("", table);
		app.start();
		try {
			var result = app.newProcedure(() -> {
				var v = table.getOrAdd(7071L);
				var list = v.getList30();
				list.clear();
				list.addAll(List.of(1, 2, 3));
				ListIterator<Integer> li = list.listIterator();
				Assertions.assertEquals(1, li.next()); // lastReturned=1，lastRet=0
				list.remove(Integer.valueOf(1)); // 同事务内外部结构性修改：[2,3]，下标0已是2
				Assertions.assertThrows(ConcurrentModificationException.class, li::remove,
						"托管路径错位场景同样必须fail-fast");
				Assertions.assertEquals(List.of(2, 3), list.getList());
				return 0L;
			}, "TestFnd707Managed").call();
			Assertions.assertEquals(Zeze.Transaction.Procedure.Success, result, "事务必须成功");
		} finally {
			app.stop();
		}
	}
}
