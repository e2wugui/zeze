package UnitTest.Zeze.Game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import Zeze.Application;
import Zeze.Arch.ProviderUserSession;
import Zeze.Builtin.Game.Bag.BBagKey;
import Zeze.Builtin.Game.Bag.Destroy;
import Zeze.Builtin.Game.Bag.Move;
import Zeze.Builtin.Provider.Dispatch;
import Zeze.Config;
import Zeze.Game.Bag;
import Zeze.Transaction.Procedure;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * FND3-50 收尾回归：BBagKey(roleId, bagName) 改造后 roleId 取自会话进表键，
 * bagName（客户端载荷）只能在本角色分区内寻址——跨角色越权在键结构上不可能，
 * 归属钩子 checkBagAccess 随之移除。本测试锁死剩余两个不变量：
 * 1) 他人 bagName 只会落进自己分区的不存在行（BagNotExist），他人行原封不动；
 * 2) 内建协议非建行访问：对不存在的 bagName 返回 BagNotExist 而非建空行——
 *   旧行为 getOrAdd+destroy 恒成功，任意 bagName 的空行会随事务提交落盘。
 * 协议成功路径（respond 需真实 link）不在此测，由 TestBag 覆盖。
 * 表注册须在app.start()前，故各测试自建Module与Application（顺序：new Application→new Module→start）。
 */
@Fast
public class TestBagPartition {
	// 760段：避开200/400/500/700/730/750/770（@Fast类并行，独占RocksCache与Memory库url）。
	private static final int ServerId = 760;

	private static final long OwnerId = 456; // 受害者：真实持有 "bag#456"
	private static final long AttackerId = 123; // 攻击者：拿着别人的 bagName 发协议
	private static final String BagName = "bag#" + OwnerId;

	// 暴露protected处理器给测试直调（不经网络派发）。
	static class ExposedModule extends Bag.Module {
		ExposedModule(Application zeze) {
			super(zeze);
		}

		long moveRpc(Move r) throws Exception {
			return ProcessMoveRequest(r);
		}

		long destroyRpc(Destroy r) throws Exception {
			return ProcessDestroyRequest(r);
		}
	}

	private Application app;
	private ExposedModule module;

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
		dbConf.setDatabaseUrl("bag_partition_test_" + ServerId);
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		return new Application("TestBagPartition", conf);
	}

	private static ProviderUserSession session(long roleId) {
		var dispatch = new Dispatch();
		dispatch.Argument.setContext(Long.toString(roleId));
		return new ProviderUserSession(dispatch);
	}

	private static Move moveRpc(String bagName, long userRoleId, int from, int to) {
		var move = new Move();
		move.Argument.setBagName(bagName);
		move.Argument.setPositionFrom(from);
		move.Argument.setPositionTo(to);
		move.setUserState(session(userRoleId));
		return move;
	}

	private static Destroy destroyRpc(String bagName, long userRoleId) {
		var destroy = new Destroy();
		destroy.Argument.setBagName(bagName);
		destroy.Argument.setPosition(0);
		destroy.setUserState(session(userRoleId));
		return destroy;
	}

	@Test
	public void testForeignBagNameStaysInOwnPartition() throws Exception {
		app = newApp();
		module = new ExposedModule(app);
		module.funcItemPileMax = itemId -> 99;
		app.start();

		// 受害者的包裹：容量10，格子0放50个物品100。
		assertEquals(Procedure.Success, app.newProcedure(() -> {
			var bag = module.open(OwnerId, BagName);
			bag.setCapacity(10);
			assertEquals(0, bag.add(100, 50), "setup添加应全部成功");
			return Procedure.Success;
		}, "TestBagPartitionSetup").call());

		// 攻击者拿受害者的bagName发Move/Destroy：命中的是(AttackerId, BagName)——
		// 自己分区里的不存在行，而不是受害者的行。
		var moveResult = app.newProcedure(
				() -> module.moveRpc(moveRpc(BagName, AttackerId, 0, 1)), "TestBagForeignMove").call();
		assertEquals(module.errorCode(Bag.Module.ResultCodeBagNotExist), moveResult, "他人bagName不得触达他人分区");

		var destroyResult = app.newProcedure(
				() -> module.destroyRpc(destroyRpc(BagName, AttackerId)), "TestBagForeignDestroy").call();
		assertEquals(module.errorCode(Bag.Module.ResultCodeBagNotExist), destroyResult, "Destroy同样只在自己分区寻址");

		// 受害者的行原封不动；攻击者分区内没有落任何行（非建行）。
		assertEquals(Procedure.Success, app.newProcedure(() -> {
			var victim = module.openOrNull(OwnerId, BagName);
			assertNotNull(victim, "受害者的行必须还在");
			assertEquals(50, victim.getBean().getItems().get(0).getNumber(), "受害者格子0数量不变");
			assertNull(module.getTable().get(new BBagKey(AttackerId, BagName)), "攻击者分区内不得落行");
			return Procedure.Success;
		}, "TestBagPartitionVerify").call());
	}

	@Test
	public void testNonExistBagRejectedWithoutCreatingRow() throws Exception {
		app = newApp();
		module = new ExposedModule(app);
		app.start();

		// 对自己分区内不存在的bagName：BagNotExist，且不得建空行落盘（旧行为destroy恒成功提交空行）。
		var moveResult = app.newProcedure(
				() -> module.moveRpc(moveRpc("fresh", AttackerId, 0, 1)), "TestBagFreshMove").call();
		assertEquals(module.errorCode(Bag.Module.ResultCodeBagNotExist), moveResult, "Move对不存在行必须拒绝");

		var destroyResult = app.newProcedure(
				() -> module.destroyRpc(destroyRpc("fresh", AttackerId)), "TestBagFreshDestroy").call();
		assertEquals(module.errorCode(Bag.Module.ResultCodeBagNotExist), destroyResult, "Destroy对不存在行必须拒绝而非建行成功");

		assertEquals(Procedure.Success, app.newProcedure(() -> {
			assertNull(module.getTable().get(new BBagKey(AttackerId, "fresh")), "失败请求不得留下空行");
			return Procedure.Success;
		}, "TestBagFreshVerify").call());
	}

	@Test
	public void testOwnedExistingBagReachesMoveLogic() throws Exception {
		app = newApp();
		module = new ExposedModule(app);
		module.funcItemPileMax = itemId -> 99;
		app.start();

		assertEquals(Procedure.Success, app.newProcedure(() -> {
			var bag = module.open(AttackerId, "bag#own");
			bag.setCapacity(10);
			assertEquals(0, bag.add(100, 50));
			return Procedure.Success;
		}, "TestBagOwnedSetup").call());

		// 本人对自己已存在的包裹发Move：应穿过非建行访问、到达move的参数校验
		// （from=5在容量内但为空格→FromNotExist），证明openOrNull命中的是真实行而非一律BagNotExist。
		var result = app.newProcedure(
				() -> module.moveRpc(moveRpc("bag#own", AttackerId, 5, 6)), "TestBagOwnedMove").call();
		assertEquals(module.errorCode(Bag.Module.ResultCodeFromNotExist), result, "已存在行应进入move逻辑");
	}
}
