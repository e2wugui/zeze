package Zeze.Net;

import Zeze.Config;
import Zeze.Services.ServiceManager.KeepAlive;
import Zeze.Services.ServiceManager.Subscribe;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * FND16 net-01 红绿钉板（typeId 层）：Rpc 应答会合先校验后消费——typeId 与上下文
 * 不一致的应答帧（伪造劫持的常见形态：异协议 Response 携邻居号）必须被拒绝且
 * 上下文留存（真实应答或超时仍可达）；同协议应答正常消费。
 * <p>
 * sender 连接绑定层（ctx.sender!=null 时应答须从原发送连接到达）在本钉板不可构造
 * （AsyncSocket 依赖真实通道），以编译+机制核查口径覆盖（Raft/SM/GCM 全家原连接
 * 应答、Online 经 linkd 的 sender==null 上下文不受该层约束）——对齐 FND15 net-01
 * 的部分层钉板先例。sessionId 全 JVM 顺序发号+明文入帧的枚举面由 A1-G1C01 证链
 * 留档。
 */
@Fast
public class TestRpcResponseHijack {
	private static Service service;

	@BeforeAll
	public static void setUp() {
		// 不 start：纯 rpcContexts 会合逻辑，不起任何网络。
		service = new Service("TestRpcHijack", new Config());
	}

	@Test
	public void testTypeMismatchRejected() throws Exception {
		var ctx = new KeepAlive(); // 上下文（sender=null：连接绑定层对 null 上下文不约束）
		var sid = service.addRpcContext(ctx);

		// 伪造应答：异协议（Subscribe）的 Response 帧携带目标号。
		var fake = new Subscribe();
		fake.setRequest(false);
		fake.setSessionId(sid);
		fake.dispatch(service, null);

		Assertions.assertSame(ctx, service.removeRpcContext(sid),
				"typeId不匹配的应答必须被拒绝且上下文留存（否则伪造帧劫持他人在飞Rpc）");
	}

	@Test
	public void testTypeMatchConsumes() throws Exception {
		var ctx = new KeepAlive();
		var sid = service.addRpcContext(ctx);

		// 真实应答：同协议 Response 帧（future/responseHandle 均 null，消费后无操作）。
		var ok = new KeepAlive();
		ok.setRequest(false);
		ok.setSessionId(sid);
		ok.dispatch(service, null);

		Assertions.assertNull(service.removeRpcContext(sid), "同协议应答必须正常会合消费");
	}
}
