package Zeze.Arch;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import Zeze.Builtin.Online.Login;
import Zeze.Builtin.Online.SReliableNotify;
import Zeze.Builtin.ProviderDirect.BLoginKey;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

/**
 * Arch.OnlineSpec / OnlineTarget 的单元测试（不组网）。
 *
 * 组网 harness（TestOnlineSpec）拿不到 Arch.Online 实例：Game 服务器挂的是
 * Zeze.Game.Online（与 Zeze.Arch.Online 是两棵独立继承树），ZezexJava 也没有
 * 任何 App 实例化 Zeze.Arch.Online。因此这里只覆盖不依赖 Online 实例的层：
 * 空目标短路、构造时快照/去重、describe、Rpc fail-fast 守卫。
 * Account/Accounts/Logins/Reliable 的实际投递路径仍由代码走查保证。
 */
public class TestArchOnlineSpec {
	private static final long ANY_TYPE_ID = 1L;

	/**
	 * 未初始化 Online 探针：不调用构造器分配实例，字段全为默认值，任何真实发送路径都会触碰其内部状态而 NPE——
	 * 语义等价于最初的 null 探针（空目标/守卫必须提前返回），但引用非 null，
	 * 满足工厂方法 @NotNull 契约，兼容 IDEA "Add runtime assertions"。
	 */
	private static final Online NoOnline = newUninitializedOnline();

	@SuppressWarnings("restriction")
	private static Online newUninitializedOnline() {
		try {
			@SuppressWarnings("unchecked")
			var ctor = (Constructor<Online>)sun.reflect.ReflectionFactory.getReflectionFactory()
					.newConstructorForSerialization(Online.class, Object.class.getDeclaredConstructor());
			return ctor.newInstance();
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	@Test
	public void testEmptyTargetShortCircuit() {
		var bomb = new SReliableNotify() {
			@Override
			public void encode(@NotNull ByteBuffer bb) {
				throw new AssertionError("empty target must not encode");
			}
		};
		// 空目标在编码与发送之前短路（未初始化 online 探针，见 NoOnline 注释）
		OnlineSpec.ofLogins(NoOnline, List.of()).send(bomb);
		OnlineSpec.ofLogins(NoOnline, List.of()).sendNow(bomb);
		OnlineSpec.ofLogins(NoOnline, List.of()).sendWhileRollback(bomb);
		OnlineSpec.ofAccounts(NoOnline, List.of()).send(bomb);
		OnlineSpec.ofAccounts(NoOnline, List.of()).sendNow(bomb);
		OnlineSpec.ofAccounts(NoOnline, List.of()).sendWhileRollback(bomb);
		OnlineSpec.ofLogins(NoOnline, List.of()).send(ANY_TYPE_ID, Binary.Empty);
		OnlineSpec.ofAccounts(NoOnline, List.of()).sendNow(ANY_TYPE_ID, Binary.Empty);
		// dispatch 空集合路径同样不触碰 online
		Assert.assertEquals(0, OnlineTarget.dispatchLogins(NoOnline, Set.of(), ANY_TYPE_ID, Binary.Empty, false));
		Assert.assertEquals(0, OnlineTarget.dispatchAccounts(NoOnline, Set.of(), ANY_TYPE_ID, Binary.Empty, false));
	}

	@Test
	public void testSnapshotAndDedup() {
		var key1 = new BLoginKey("account", "client1");
		var key2 = new BLoginKey("account", "client2");

		// 构造后修改调用方的活集合，不影响快照
		var mutableLogins = new HashSet<BLoginKey>();
		mutableLogins.add(key1);
		var logins = new OnlineTarget.Logins(mutableLogins);
		mutableLogins.add(key2);
		Assert.assertEquals(Set.of(key1), logins.logins());

		var mutableAccounts = new ArrayList<String>(List.of("a"));
		var accounts = new OnlineTarget.Accounts(mutableAccounts);
		mutableAccounts.add("b");
		Assert.assertEquals(Set.of("a"), accounts.accounts());

		// 去重
		Assert.assertEquals(Set.of(key1), new OnlineTarget.Logins(List.of(key1, key1)).logins());
		Assert.assertEquals(Set.of("a", "b"), new OnlineTarget.Accounts(List.of("a", "b", "a")).accounts());

		// isEmpty：仅集合目标可为空，单目标永不为空
		Assert.assertTrue(new OnlineTarget.Logins(Set.of()).isEmpty());
		Assert.assertTrue(new OnlineTarget.Accounts(Set.of()).isEmpty());
		Assert.assertFalse(new OnlineTarget.Logins(Set.of(key1)).isEmpty());
		Assert.assertFalse(new OnlineTarget.Accounts(Set.of("a")).isEmpty());
		Assert.assertFalse(new OnlineTarget.Login("a", "c").isEmpty());
		Assert.assertFalse(new OnlineTarget.Account("a").isEmpty());
		Assert.assertFalse(new OnlineTarget.Reliable("a", "c", "l").isEmpty());
	}

	@Test
	public void testDescribe() {
		var key1 = new BLoginKey("account", "client1");
		Assert.assertEquals("account,client1", new OnlineTarget.Logins(Set.of(key1)).describe());
		Assert.assertEquals("a", new OnlineTarget.Accounts(Set.of("a")).describe());
		Assert.assertEquals("account,client", new OnlineTarget.Login("account", "client").describe());
		Assert.assertEquals("account", new OnlineTarget.Account("account").describe());
		Assert.assertEquals("account,client:listener", new OnlineTarget.Reliable("account", "client", "listener").describe());
	}

	@Test
	public void testRpcFailFast() {
		// Rpc request 直接 send/sendNow 抛 IllegalArgumentException（提示走 sendResponse）；守卫先于 online 使用
		Assert.assertThrows(IllegalArgumentException.class,
				() -> OnlineSpec.ofLogin(NoOnline, "a", "c").send(new Login()));
		Assert.assertThrows(IllegalArgumentException.class,
				() -> OnlineSpec.ofLogin(NoOnline, "a", "c").sendNow(new Login()));
		Assert.assertThrows(IllegalArgumentException.class,
				() -> OnlineSpec.ofLogins(NoOnline, List.of(new BLoginKey("a", "c"))).send(new Login()));
		Assert.assertThrows(IllegalArgumentException.class,
				() -> OnlineSpec.ofAccounts(NoOnline, List.of("a")).send(new Login()));
	}
}
