package UnitTest.Zeze.Game;

import Zeze.Game.Bag;
import Zeze.Transaction.Procedure;
import demo.App;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestBag {
	@Before
	public final void testInit() throws Throwable {
		demo.App.getInstance().Start();
		// 设置下可堆叠个数
		Bag.funcItemPileMax = itemId -> 99;
	}

	@After
	public final void testCleanup() throws Throwable {
		demo.App.getInstance().Stop();
	}

	@Test
	public final void test1_Add() throws Throwable {
		var ret = demo.App.getInstance().Zeze.NewProcedure(() -> {
			var bag = App.getInstance().BagModule.open("test1");
			bag.setCapacity(100);
			for (int i = 100; i < 110; i++) {
				// bag.GetItemPileMax() TODO阶段，默认99
				// 占用两个格子，第1个99个，第二个1个
				bag.add(i, 100);
			}
			var items = bag.getBean().getItems();
			// 总共占用20个格子
			Assert.assertEquals(20, items.size());
			return Procedure.Success;
		}, "test1_Add").Call();
		Assert.assertEquals(ret, Procedure.Success);
	}

	@Test
	public final void test2_Move() throws Throwable {
		var ret = demo.App.getInstance().Zeze.NewProcedure(() -> {
			var bag = demo.App.getInstance().BagModule.open("test1");
			var items = bag.getBean().getItems();
			Assert.assertEquals(items.size(), 20);
			for (int i = 0; i < 20; i += 2) {
				bag.move(i, i + 1, 49);
				Assert.assertEquals(items.get(i).getNumber(), 50);
				Assert.assertEquals(items.get(i + 1).getNumber(), 50);
			}
			return Procedure.Success;
		}, "test2_Move").Call();
		Assert.assertEquals(ret, Procedure.Success);
	}

	@Test
	public final void test3_Remove() throws Throwable {
		var ret = demo.App.getInstance().Zeze.NewProcedure(() -> {
			var bag = demo.App.getInstance().BagModule.open("test1");
			var items = bag.getBean().getItems();
			Assert.assertEquals(items.size(), 20);
			for (int i = 100; i < 110; i++) {
				bag.remove(i, 50);
			}
			Assert.assertEquals(items.size(), 10);
			for (int i = 100; i < 110; i++) {
				bag.remove(i, 10);
			}
			for (int i = 1; i < 20; i += 2) {
				Assert.assertEquals(items.get(i).getNumber(), 40);
			}
			return Procedure.Success;
		}, "test3_Remove").Call();
		Assert.assertEquals(ret, Procedure.Success);
	}

	@Test
	public final void test4_Move() throws Throwable {
		var ret = demo.App.getInstance().Zeze.NewProcedure(() -> {
			var bag = demo.App.getInstance().BagModule.open("test1");
			var items = bag.getBean().getItems();
			Assert.assertEquals(items.size(), 10);
			// 移动物品到空格子
			for (int i = 0; i < 20; i += 2) {
				bag.move(i + 1, i, 20);
				Assert.assertEquals(items.get(i).getNumber(), 20);
				Assert.assertEquals(items.get(i + 1).getNumber(), 20);
			}
			Assert.assertEquals(items.size(), 20);
			return Procedure.Success;
		}, "test4_Move").Call();
		Assert.assertEquals(ret, Procedure.Success);
	}
}
