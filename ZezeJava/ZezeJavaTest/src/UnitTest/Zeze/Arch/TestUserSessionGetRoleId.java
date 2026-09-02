package UnitTest.Zeze.Arch;

import Zeze.Arch.LinkdUserSession;
import Zeze.Arch.ProviderUserSession;
import Zeze.Builtin.Provider.Dispatch;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

// N-26回归：BUserState.context是登录身份槽——Game角色模式存roleId数字串，
// Arch账号模式存clientId（任意字符串，见Online.java:1783 setContext(getClientId())）。
// getRoleId()无条件Long.parseLong，账号模式下抛NumberFormatException；
// 开协议日志时ProcessDispatch的Recv/Send日志分支在try内触发NFE，
// 被当成协议异常sendKick(ErrorProtocolException)，玩家发正常请求即被踢。
// 约定：账号模式没有roleId概念，getRoleId()应返回null（调用点已有null兜底：
// 日志用-linkSid，LinkdProvider返回错误码21）。
@Fast
public class TestUserSessionGetRoleId {
	@Test
	public void testProviderSessionContextVariants() {
		// 账号在线模式：context=clientId非数字字符串，不得抛异常，返回null
		var accountMode = new Dispatch();
		accountMode.Argument.setContext("device-abc-123");
		Assertions.assertNull(new ProviderUserSession(accountMode).getRoleId());

		// 未登录：空context返回null（既有行为不变）
		var loggedOut = new Dispatch();
		loggedOut.Argument.setContext("");
		Assertions.assertNull(new ProviderUserSession(loggedOut).getRoleId());

		// Game角色模式：数字roleId照常解析（既有行为不变）
		var roleMode = new Dispatch();
		roleMode.Argument.setContext("123456");
		Assertions.assertEquals(Long.valueOf(123456), new ProviderUserSession(roleMode).getRoleId());
	}

	@Test
	public void testLinkdSessionContextVariants() {
		var session = new LinkdUserSession(1);

		// 账号在线模式：clientId非数字字符串，不得抛异常，返回null
		session.getUserState().setContext("device-abc-123");
		Assertions.assertNull(session.getRoleId());

		// 未登录
		session.getUserState().setContext("");
		Assertions.assertNull(session.getRoleId());

		// Game角色模式
		session.getUserState().setContext("123456");
		Assertions.assertEquals(Long.valueOf(123456), session.getRoleId());
	}
}
