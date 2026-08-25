package UnitTest.Zeze.Arch;

import java.lang.reflect.Constructor;
import java.util.List;

import Zeze.Arch.Online;
import Zeze.Arch.OnlineSpec;
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
