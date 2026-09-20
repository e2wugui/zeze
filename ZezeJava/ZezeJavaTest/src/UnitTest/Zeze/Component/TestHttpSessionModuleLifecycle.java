package UnitTest.Zeze.Component;

import Zeze.Arch.ProviderApp;
import Zeze.Application;
import Zeze.Netty.HttpSession;
import Zeze.Netty.HttpServer;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * NY2-F1回归：ExpiredTimer按全名（ModuleFullName）查模块表，登记却用短名（AppBase.addModule
 * 按getName()=ModuleName登记）——过期会话清理永不执行。修复：查询键改为ModuleName。
 * NY2-F2回归：stop()移除模块后start()不再登记，close→start同实例重启后会话模块从注册表
 * 消失（F1修复后过期清理立即失效）。修复：start()首行幂等addModule（保留stop"停止即注销"）。
 * NY2-F7附带：accessTable无事务路径成功/失败行为不回归（异常cause捕获分支无可注入点，见台账）。
 */
@Fast
public class TestHttpSessionModuleLifecycle {

	private static Application newApp() throws Exception {
		var conf = TakeoverTestEnv.newConf("dryrun", 600_000, 600_000);
		var zeze = new Application("TestHttpSessionModuleLifecycle", conf);
		new ProviderApp(zeze); // fake ProviderApp：建立redirect，initialize创建Timer
		zeze.initialize(new TakeoverTestEnv.TestAppBase(zeze));
		return zeze;
	}

	@Test
	public void testModuleRegisteredUnderShortNameAndReaddedAfterRestart() throws Exception {
		Task.tryInitThreadPool();
		var zeze = newApp();
		try {
			zeze.start(); // 表打开（enableHttpSession的openDynamicTable前提）
			var httpServer = new HttpServer(zeze);
			httpServer.enableHttpSession();
			var session = httpServer.getHttpSession();
			Assertions.assertNotNull(session);

			// NY2-F1：ExpiredTimer的查询键（ModuleName=短名）必须命中登记条目
			Assertions.assertSame(session, zeze.getAppBase().getModules().get(HttpSession.ModuleName),
					"模块表必须以短名（getName/ModuleName）登记——ExpiredTimer按此键查询");
			Assertions.assertNull(zeze.getAppBase().getModules().get(HttpSession.ModuleFullName),
					"全名键下无条目（登记侧从不使用全名）");

			// NY2-F2：stop注销（停止即注销语义保留），start幂等补登记（重启恢复）
			session.stop();
			Assertions.assertNull(zeze.getAppBase().getModules().get(HttpSession.ModuleName),
					"stop后模块必须注销（getModules查询方不再看到已停模块）");
			session.start();
			Assertions.assertSame(session, zeze.getAppBase().getModules().get(HttpSession.ModuleName),
					"close→start重启后模块必须回到注册表（否则过期清理失效）");

			// NY2-F7附带：无事务路径accessTable行为不回归（CookieSession经短Procedure访问表）
			var cs = session.new CookieSession("wt4-lifecycle-sid");
			Assertions.assertThrows(IllegalStateException.class, () -> cs.getProperty("k"),
					"未知会话id在无事务路径按既有契约抛IllegalStateException（accessTable正常工作）");
		} finally {
			zeze.stop();
		}
	}
}
