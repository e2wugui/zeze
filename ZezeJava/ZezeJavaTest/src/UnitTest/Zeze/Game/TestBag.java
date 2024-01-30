package UnitTest.Zeze.Game;

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
	public static final int ADD_NUM = 100;          // add item num
	public static final int ADD_PILE_NUM = 10;      // 添加的格子数量
	public static final int MIN_ITEM_ID = 100;      // item编号起始值
	public static final int MAX_GRID_CAPACITY = 99; // 每个格子堆叠上限
	public static final int SECOND_REMOVE_NUM = 10; // 第二次删除的item数量 应小于ADD_NUM/2
	public static final int MAX_BAG_CAPACITY = 100; // 背包容量

	@Before
	public final void testInit() throws Exception {
		demo.App.getInstance().Stop();
		demo.App.getInstance().Start();
		// 设置下可堆叠个数
		App.Instance.BagModule.funcItemPileMax = itemId -> MAX_GRID_CAPACITY;
	}

	@After
	public final void testCleanup() throws Exception {
	}

	@Test
	public final void test1_Add() throws Exception {
		Assert.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(TestBag::preRemove, "BagPreRemove").call());
		var ret = demo.App.getInstance().Zeze.newProcedure(() -> {
			var bag = App.getInstance().BagModule.open("test1");
			bag.setCapacity(MAX_BAG_CAPACITY);
			for (int i = MIN_ITEM_ID; i < MIN_ITEM_ID + ADD_PILE_NUM; i++) {
				// bag.GetItemPileMax() TODO阶段，默认99
				// 占用两个格子，第1个99个，第二个1个
				var code = bag.add(i, ADD_NUM);
				Assert.assertEquals(code, 0);
			}
			// 总共占用20个格子
			Assert.assertEquals(ADD_PILE_NUM * 2, bag.getBean().getItems().size());
			return Procedure.Success;
		}, "test1_Add").call();
		Assert.assertEquals(ret, Procedure.Success);
	}

	@Test
	public final void test2_Move() throws Exception {
		var ret = demo.App.getInstance().Zeze.newProcedure(() -> {
			var bag = demo.App.getInstance().BagModule.open("test1");
			Assert.assertEquals(bag.getBean().getItems().size(), ADD_PILE_NUM * 2);
			int moveNum = MAX_GRID_CAPACITY - (ADD_NUM / 2);
			for (int i = 0; i < ADD_PILE_NUM * 2; i += 2) {
				int code = bag.move(i, i + 1, moveNum);
				Assert.assertEquals(code, 0);
				Assert.assertEquals(bag.getBean().getItems().get(i).getNumber(), ADD_NUM / 2);
				Assert.assertEquals(bag.getBean().getItems().get(i + 1).getNumber(), ADD_NUM / 2);
			}
			return Procedure.Success;
		}, "test2_Move").call();
		Assert.assertEquals(ret, Procedure.Success);
	}

	@Test
	public final void test3_Remove() throws Exception {
		var ret = demo.App.getInstance().Zeze.newProcedure(() -> {
			var bag = demo.App.getInstance().BagModule.open("test1");
			Assert.assertEquals(bag.getBean().getItems().size(), ADD_PILE_NUM * 2);
			for (int i = MIN_ITEM_ID; i < MIN_ITEM_ID + ADD_PILE_NUM; i++) {
				var code = bag.remove(i, ADD_NUM / 2);
				Assert.assertTrue(code);
			}
			Assert.assertEquals(bag.getBean().getItems().size(), ADD_PILE_NUM);
			for (int i = MIN_ITEM_ID; i < MIN_ITEM_ID + ADD_PILE_NUM; i++) {
				var code = bag.remove(i, SECOND_REMOVE_NUM);
				Assert.assertTrue(code);
			}
			for (int i = 1; i < ADD_PILE_NUM * 2; i += 2) {
				Assert.assertEquals(bag.getBean().getItems().get(i).getNumber(), ADD_NUM / 2 - SECOND_REMOVE_NUM);
			}
			return Procedure.Success;
		}, "test3_Remove").call();
		Assert.assertEquals(ret, Procedure.Success);
	}

	@Test
	public final void test4_Move() throws Exception {
		var ret = demo.App.getInstance().Zeze.newProcedure(() -> {
			var bag = demo.App.getInstance().BagModule.open("test1");
			Assert.assertEquals(bag.getBean().getItems().size(), ADD_PILE_NUM);
			// 移动物品到空格子
			int moveNum = (ADD_NUM / 2 - SECOND_REMOVE_NUM) / 2;
			for (int i = 1; i < ADD_PILE_NUM * 2; i += 2) {
				var code = bag.move(i, i - 1, moveNum);
				Assert.assertEquals(code, 0);
				Assert.assertEquals(bag.getBean().getItems().get(i - 1).getNumber(), moveNum);
				Assert.assertEquals(bag.getBean().getItems().get(i).getNumber(), ADD_NUM / 2 - SECOND_REMOVE_NUM - moveNum);
			}
			Assert.assertEquals(bag.getBean().getItems().size(), ADD_PILE_NUM * 2);
			return Procedure.Success;
		}, "test4_Move").call();
		Assert.assertEquals(ret, Procedure.Success);
	}

	private static long preRemove() {
		var table = App.getInstance().BagModule.getTable();
		table.remove("test1");
		System.out.printf("delete table %s%n", table.getName());
		return Procedure.Success;
	}
}
