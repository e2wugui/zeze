package UnitTest.Zeze.Arch;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import Zeze.Arch.ProviderUserSession;
import Zeze.Builtin.Provider.Dispatch;

@Fast
public class TestProviderUserSessionIsLogin {
	@Test
	public void testIsLogin() {
		// 纯单元：全仓语义为 context 非空 == 已登录（getRoleId、LinkdProvider 广播、
		// ProviderWithOnline.LinkBroken、sendOnline 均按此约定），isLogin 必须同向。
		var loggedOut = new Dispatch();
		loggedOut.Argument.setContext("");
		assertFalse(new ProviderUserSession(loggedOut).isLogin());

		var loggedIn = new Dispatch();
		loggedIn.Argument.setContext("123456");
		assertTrue(new ProviderUserSession(loggedIn).isLogin());
	}
}
