package Zeze.Arch;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash;
import Zeze.Builtin.ProviderDirect.ModuleRedirectAllResult;
import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Net.Service;
import Zeze.Transaction.Procedure;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestRedirectAllResultCallbackFailureIsolation {

	@Test
	public void lateCallbackFailureDoesNotDiscardOtherSavedResults() {
		Task.tryInitThreadPool();
		var service = new Service("RedirectAllLateCallbackFailureIsolation", (Zeze.Application)null, new Config());
		var context = new RedirectAllContext<RedirectResult>(3, binary -> new RedirectResult());
		long sessionId = service.addManualContextWithTimeout(context, 30_000);
		var response = new ModuleRedirectAllResult();
		for (int hash = 0; hash < 3; hash++)
			response.Argument.getHashes().put(hash, new BModuleRedirectAllHash.Data(Procedure.Success, Binary.Empty));
		try {
			context.processResult(response); // 回调迟注册：先保存同一报文的全部结果。
			assertEquals(3, context.getAllResults().size());
			var callbackCounts = new HashMap<Integer, Integer>();
			var calls = new AtomicInteger();
			assertDoesNotThrow(() -> context.getFuture().OnResult(result -> {
				callbackCounts.merge(result.getHash(), 1, Integer::sum);
				if (calls.incrementAndGet() == 1)
					throw new IllegalStateException("first saved result callback failed");
			}), "迟注册扫描的首个回调失败不得逃逸或阻止其余已保存结果回调");
			assertEquals(3, calls.get());
			for (int hash = 0; hash < 3; hash++)
				assertEquals(1, callbackCounts.get(hash), "每个已保存结果都应独立回调恰好一次");
			assertEquals(3, context.getAllResults().size(), "回调失败不能改变已经收集的结果");
		} finally {
			service.tryRemoveManualContext(sessionId);
		}
	}

	@Test
	public void failedCallbackDoesNotDiscardOtherHashesInTheSameResponse() {
		Task.tryInitThreadPool();
		var service = new Service("RedirectAllResultCallbackFailureIsolation", (Zeze.Application)null, new Config());
		var context = new RedirectAllContext<RedirectResult>(3, binary -> new RedirectResult());
		long sessionId = service.addManualContextWithTimeout(context, 30_000);
		var callbackCalls = new AtomicInteger();
		var completionCalls = new AtomicInteger();
		context.getFuture().OnResult(result -> {
			if (callbackCalls.incrementAndGet() == 1)
				throw new IllegalStateException("first result callback failed");
		});
		context.getFuture().OnAllDone(done -> completionCalls.incrementAndGet());

		var response = new ModuleRedirectAllResult();
		for (int hash = 0; hash < 3; hash++)
			response.Argument.getHashes().put(hash, new BModuleRedirectAllHash.Data(Procedure.Success, Binary.Empty));
		try {
			try {
				context.processResult(response);
			} catch (IllegalStateException e) {
				// 用最终状态检验批处理隔离；修复前首个回调异常会逃逸并截断报文。
				assertEquals("first result callback failed", e.getMessage());
			}
			assertEquals(3, context.getAllResults().size(), "一条报文中后续hash的结果不能因首个回调异常丢失");
			assertEquals(3, callbackCalls.get(), "每个hash都应独立回调一次");
			assertEquals(1, completionCalls.get(), "收齐全部hash后必须正常触发完成回调");
			assertNull(service.tryGetManualContext(sessionId), "必须立即收尾，不应依赖上下文超时");
		} finally {
			service.tryRemoveManualContext(sessionId);
		}
	}
}
