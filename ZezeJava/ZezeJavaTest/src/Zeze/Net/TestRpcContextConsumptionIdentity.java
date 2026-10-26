package Zeze.Net;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestRpcContextConsumptionIdentity {
	@Test
	void responseConsumesOnlyTheContextThatPassedValidation() throws Exception {
		var entered = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		var original = new TestRpcNoReuse.TestRpc() {
			@Override public long getTypeId() {
				entered.countDown();
				try {
					assertTrue(release.await(5, TimeUnit.SECONDS));
				} catch (InterruptedException e) {
					throw new AssertionError(e);
				}
				return super.getTypeId();
			}
		};
		var service = new Service("TestRpcContextConsumptionIdentity");
		service.setSessionIdGenerator(() -> 17); // 仅本 Service 的测试发号器，可复用已腾出的编号。
		long sid = service.addRpcContext(original);
		var replacement = new TestRpcNoReuse.TestRpc();
		var consumed = new AtomicReference<@Nullable Protocol<?>>();
		var failure = new AtomicReference<@Nullable Throwable>();
		var response = new TestRpcNoReuse.TestRpc();
		var worker = new Thread(() -> {
			try {
				consumed.set(Rpc.removeRpcContextChecked(service, sid, response));
			} catch (Throwable e) {
				failure.set(e);
			}
		});
		worker.start();
		try {
			assertTrue(entered.await(5, TimeUnit.SECONDS));
			assertSame(original, service.removeRpcContext(sid)); // 原上下文已由超时/另一应答消费。
			assertEquals(sid, service.addRpcContext(replacement));
		} finally {
			release.countDown();
			worker.join(5000);
		}
		try {
			assertFalse(worker.isAlive());
			assertNull(failure.get());
			assertNull(consumed.get(), "原上下文已被消费，当前应答应按丢失处理");
			assertSame(replacement, service.getRpcContext(sid), "不得删除未经过当前校验的新上下文");
		} finally {
			service.removeRpcContext(sid);
			service.stop();
		}
	}
}
