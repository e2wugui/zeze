package Zeze.Services.Log4jQuery;

import java.io.IOException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import Zeze.Builtin.LogService.BResult;
import Zeze.Builtin.LogService.Search;
import Zeze.Transaction.Procedure;
import Zeze.Util.TaskCompletionSource;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND20 GE-C03（含盲审案外#1）：Session.search/browse 的 TCS resultCode 检查，纯逻辑直测。
 * 机制链：Zeze RPC 对非零 resultCode 的应答正常完成 future（Rpc.dispatch/handle 无条件
 * future.setResult，resultCode 只是 rpc 字段）——死会话（服务端闲置回收后 getLogSession==null
 * 返回 Procedure.LogicError）的空 Result（remain=false）会被当"查完无匹配"静默消费，
 * SessionAll 还把该台永久标记 finished。修复后 {@link Session#checkResultCode} 包装的 TCS
 * 在 get 时对非零码抛异常（对齐 Session.close 既有形态），单台 HTTP handle 的 catch 返回
 * 可见 system error、SessionAll.operate 的既有 catch 走 failedServers。
 * 本测直构 Search rpc（生成类构造不依赖网络）+ 已完成的底层 TCS，验证四种通道：
 * 非零码异常（get/get(timeout) 两形态）、零码透传、RPC 自身失败（超时/发送失败）的异常
 * 语义不被包装改变。SessionAll.operate 的 failedServers 分流是既有 catch 路径（GE-C03 不变式
 * 见 SessionAll 注释），其消费面（LogAgent 网络）不在纯逻辑直测面。
 */
@Fast
public class TestSessionResultCodeCheck {

	/** 死会话通道：服务端 LogicError 到达时，get 必须抛而非返回空 Result。 */
	@Test
	public void testNonZeroCodeThrowsOnGet() {
		var rpc = new Search();
		rpc.setResultCode(Procedure.LogicError);
		var inner = new TaskCompletionSource<BResult.Data>();
		inner.setResult(new BResult.Data()); // 未解码的空 Result（logs 空、remain=false）

		var wrapped = Session.checkResultCode(rpc, inner);
		var ex = assertThrows(RuntimeException.class, wrapped::get);
		assertTrue(ex.getMessage().contains(Long.toString(Procedure.LogicError)),
				"异常应携带错误码便于诊断: " + ex.getMessage());
	}

	/** 同上，get(timeout) 形态（单台 HTTP handle 的调用形态）。 */
	@Test
	public void testNonZeroCodeThrowsOnGetTimeout() {
		var rpc = new Search();
		rpc.setResultCode(Procedure.LogicError);
		var inner = new TaskCompletionSource<BResult.Data>();
		inner.setResult(new BResult.Data());

		var wrapped = Session.checkResultCode(rpc, inner);
		assertThrows(RuntimeException.class, () -> wrapped.get(1, TimeUnit.SECONDS));
	}

	/** 零码通道：正常结果原样透传（不过度拦截）。 */
	@Test
	public void testZeroCodePassesThrough() {
		var rpc = new Search();
		rpc.setResultCode(0);
		var data = new BResult.Data();
		var inner = new TaskCompletionSource<BResult.Data>();
		inner.setResult(data);

		var wrapped = Session.checkResultCode(rpc, inner);
		assertSame(data, wrapped.get());
		assertSame(data, wrapped.get(1, TimeUnit.SECONDS));
	}

	/** RPC 自身失败（发送失败/超时走 setException）：异常语义透传，不被包装吞掉或改形。 */
	@Test
	public void testRpcFailureSemanticsPreserved() {
		var rpc = new Search();
		var inner = new TaskCompletionSource<BResult.Data>();
		inner.setException(new IOException("rpc fail"));

		var wrapped = Session.checkResultCode(rpc, inner);
		assertThrows(CompletionException.class, wrapped::get);
		assertEquals(0, rpc.getResultCode(), "RPC 失败通道不设错误码，不应伪造非零码");
	}
}
