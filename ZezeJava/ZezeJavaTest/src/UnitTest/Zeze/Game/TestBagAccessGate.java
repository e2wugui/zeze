package UnitTest.Zeze.Game;

import static org.junit.jupiter.api.Assertions.assertEquals;

import Zeze.Application;
import Zeze.Arch.ProviderUserSession;
import Zeze.Builtin.Game.Bag.Destroy;
import Zeze.Builtin.Game.Bag.Move;
import Zeze.Builtin.Provider.Dispatch;
import Zeze.Config;
import Zeze.Game.Bag;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * FND3-50 回归：内建协议(Move/Destroy)的bagName归属门卫。
 * bagName来自客户端载荷，旧代码唯一校验是会话存在——任意已认证连接可用他人bagName
 * 越权操作背包（成功操作会提交持久化）。修复：checkBagAccess钩子默认拒绝，
 * 应用必须覆写显式定义归属规则后这两个协议才可用。
 * 表注册须在app.start()前，故各测试自建Module与Application（顺序：new Application→new Module→start）。
 */
@Fast
public class TestBagAccessGate {
	// 760段：避开200/400/500/700/730/750（@Fast类并行，独占RocksCache与Memory库url）。
	private static final int ServerId = 760;

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

	// 标准覆写：bag#roleId 精确匹配放行，否则以自定义码77拒绝。
	static class OwnedBagModule extends ExposedModule {
		OwnedBagModule(Application zeze) {
			super(zeze);
		}

		@Override
		protected int checkBagAccess(@NotNull ProviderUserSession session, @NotNull String bagName) {
			return bagName.equals("bag#" + session.getRoleId()) ? 0 : 77;
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
		dbConf.setDatabaseUrl("bag_access_test_" + ServerId);
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		return new Application("TestBagAccessGate", conf);
	}

	private static ProviderUserSession session(long roleId) {
		var dispatch = new Dispatch();
		dispatch.Argument.setContext(Long.toString(roleId));
		return new ProviderUserSession(dispatch);
	}

	private static Move moveRpc(String bagName, long userRoleId) {
		var move = new Move();
		move.Argument.setBagName(bagName);
		move.Argument.setPositionFrom(0);
		move.Argument.setPositionTo(1);
		move.setUserState(session(userRoleId));
		return move;
	}

	@Test
	public void testDefaultDeny() throws Exception {
		app = newApp();
		module = new ExposedModule(app);
		app.start();

		var move = moveRpc("bag#123", 999); // 攻击者roleId=999，背包属于123
		var result = app.newProcedure(() -> module.moveRpc(move), "TestBagDefaultDenyMove").call();
		assertEquals(module.errorCode(Bag.Module.ResultCodeBagNameDenied), result, "未定义归属规则必须拒绝");

		var destroy = new Destroy();
		destroy.Argument.setBagName("bag#123");
		destroy.Argument.setPosition(0);
		destroy.setUserState(session(999));
		result = app.newProcedure(() -> module.destroyRpc(destroy), "TestBagDefaultDenyDestroy").call();
		assertEquals(module.errorCode(Bag.Module.ResultCodeBagNameDenied), result, "Destroy同样必须拒绝");
	}

	@Test
	public void testOverrideAllowOwnedAndDenyForeign() throws Exception {
		app = newApp();
		module = new OwnedBagModule(app);
		app.start();

		// 归属放行后进入真实move逻辑：空背包capacity=0，from=0越界→FromInvalid(1)，
		// 证明穿过了门卫、处理器正常执行（而非门卫放行即成功）。
		var owned = moveRpc("bag#123", 123); // 本人
		var result = app.newProcedure(() -> module.moveRpc(owned), "TestBagOwnedMove").call();
		assertEquals(module.errorCode(Bag.Module.ResultCodeFromInvalid), result, "放行后应走到move的参数校验");

		var foreign = moveRpc("bag#456", 123); // 他人背包
		result = app.newProcedure(() -> module.moveRpc(foreign), "TestBagForeignMove").call();
		assertEquals(module.errorCode(77), result, "他人背包按覆写规则拒绝");
	}
}
