package Zeze.Arch;

import java.lang.reflect.Field;

import Zeze.Net.Binary;
import Zeze.Builtin.LoginQueueServer.BSecret;
import Zeze.Services.LoginQueueAgent;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND8-89回归：choiceProvider的可空点判错对象——linkdLoad（未配置LoginQueueAgent
 * 服务节时LinkdApp不构造LinkdLoad）才是真实可空点，FND5-26把判空加在内层
 * loginQueueAgent上（LinkdLoad构造即ensureLoginQueueAgent，可达世界恒非空，防护是
 * 死代码），getLinkdLoad()裸解引用照旧NPE，被认证处理吞成无差别断连，掩盖配置缺失
 * 根因。孪生：secret仅在连上LoginQueueServer收到AnnounceSecret后写入，冷启动/重启
 * 后依赖未就绪时为null（无需配置错误即可达），decodeToken(null,...)在decrypt内NPE
 * 走同一断连路径。
 * 修复：判空上移到linkdLoad，未配置返回4（保留既有契约码）；secret未就绪判空返回5
 * （与"未配置"区分）。测试用Unsafe分配跳过重型构造器，精确构造三种字段形态。
 */
@Fast
public class TestFnd889ChoiceProviderNullGuards {

	private static sun.misc.Unsafe unsafe() throws Exception {
		var field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
		field.setAccessible(true);
		return (sun.misc.Unsafe)field.get(null);
	}

	private static void setField(Class<?> declaring, Object target, String name, Object value) throws Exception {
		Field field = declaring.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	/** 未配置LoginQueueAgent（linkdLoad=null）：返回错误码4，不得NPE。 */
	@Test
	public void testNotConfiguredReturns4() throws Exception {
		var provider = new LinkdProvider();
		provider.linkdApp = (LinkdApp)unsafe().allocateInstance(LinkdApp.class); // linkdLoad字段默认null
		var rc = provider.choiceProvider(null, Binary.Empty);
		Assertions.assertEquals(4, rc, "未配置LoginQueueAgent必须返回4（FND8-89：原先getLinkdLoad()裸解引用NPE）");
	}

	/** 已配置但secret未就绪（agent已建、AnnounceSecret未达）：返回错误码5，不得NPE。 */
	@Test
	public void testSecretNotReadyReturns5() throws Exception {
		var unsafe = unsafe();
		var provider = new LinkdProvider();
		var app = (LinkdApp)unsafe.allocateInstance(LinkdApp.class);
		var load = (LinkdLoad)unsafe.allocateInstance(LinkdLoad.class);
		var agent = (LoginQueueAgent)unsafe.allocateInstance(LoginQueueAgent.class); // secret字段默认null
		setField(LoadBase.class, load, "loginQueueAgent", agent);
		setField(LinkdApp.class, app, "linkdLoad", load);
		provider.linkdApp = app;
		var rc = provider.choiceProvider(null, Binary.Empty);
		Assertions.assertEquals(5, rc, "secret未就绪必须返回5（孪生：decodeToken(null,...)在decrypt内NPE）");
	}

	/** 护栏：secret就绪时越过两道守卫进入令牌解码（空令牌解不开，抛异常即已通过守卫）。 */
	@Test
	public void testReadyPassesGuards() throws Exception {
		var unsafe = unsafe();
		var provider = new LinkdProvider();
		var app = (LinkdApp)unsafe.allocateInstance(LinkdApp.class);
		var load = (LinkdLoad)unsafe.allocateInstance(LinkdLoad.class);
		var agent = (LoginQueueAgent)unsafe.allocateInstance(LoginQueueAgent.class);
		setField(LoadBase.class, load, "loginQueueAgent", agent);
		setField(LinkdApp.class, app, "linkdLoad", load);
		var secretData = new BSecret.Data();
		secretData.setSecretKey(new Binary(new byte[16]));
		secretData.setSecretIv(new Binary(new byte[16]));
		setField(LoginQueueAgent.class, agent, "secret", secretData);
		provider.linkdApp = app;
		// 空令牌对合法密钥必然解密失败（密码学异常），关键是越过4/5守卫且非NPE
		var ex = Assertions.assertThrows(Exception.class, () -> provider.choiceProvider(null, Binary.Empty));
		Assertions.assertFalse(ex instanceof NullPointerException, "守卫通过后不得再出现空指针: " + ex);
	}
}
