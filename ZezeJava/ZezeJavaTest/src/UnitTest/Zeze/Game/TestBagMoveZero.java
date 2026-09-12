package UnitTest.Zeze.Game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import Zeze.Application;
import Zeze.Config;
import Zeze.Game.Bag;
import Zeze.Transaction.Procedure;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * FND4-78 回归：move(number=0) 旧代码0穿透数量钳制（钳制只处理&lt;0与&gt;from），
 * 目标格为空时走拆分路径写入number=0物品——空格被占据且0数量条目污染持久化，
 * 恶意客户端可逐格发0移动占满自己背包。修复：入口拒绝number==0；
 * 注意-1=移动全部是协议契约（solution.zeze.xml BMove注释），不得一并拒绝。
 * 表注册须在app.start()前，故各测试自建Module与Application（顺序：new Application→new Module→start）。
 */
@Fast
public class TestBagMoveZero {
	// 770段：避开200/400/500/700/730/750/760（@Fast类并行，独占RocksCache与Memory库url）。
	private static final int ServerId = 770;

	private Application app;
	private Bag.Module module;

	@AfterEach
	public void tearDown() throws Exception {
		if (module != null) {
			module.UnRegisterZezeTables(app);
			module = null;
		}
		if (app != null) {
			app.stop();
			app = null;
		}
	}

	private static Application newApp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(ServerId);
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("bag_move_zero_test_" + ServerId);
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		return new Application("TestBagMoveZero", conf);
	}

	@Test
	public void testMoveZeroRejected() throws Exception {
		app = newApp();
		module = new Bag.Module(app);
		module.funcItemPileMax = itemId -> 99;
		app.start();

		var ret = app.newProcedure(() -> {
			var bag = module.open("zero");
			bag.setCapacity(10);
			assertEquals(0, bag.add(100, 50), "50个可堆叠物品应全部进入格子0");

			assertEquals(Bag.Module.ResultCodeNumberInvalid, bag.move(0, 5, 0),
					"number=0必须在入口拒绝");
			assertNull(bag.getBean().getItems().get(5), "空格不得被0数量物品占据");
			assertEquals(50, bag.getBean().getItems().get(0).getNumber(), "源格数量不变");

			// -1=移动全部是文档化契约，守卫不得误伤。
			assertEquals(0, bag.move(0, 5, -1), "-1应移动全部并成功");
			assertNull(bag.getBean().getItems().get(0), "移动全部后源格清空");
			assertEquals(50, bag.getBean().getItems().get(5).getNumber(), "全部数量到达目标格");
			return Procedure.Success;
		}, "TestBagMoveZero").call();
		assertEquals(Procedure.Success, ret);
	}
}
