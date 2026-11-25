package Zeze.Arch;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Application;
import Zeze.Builtin.ProviderDirect.ModuleRedirect;
import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TransactionLevel;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

@Fast
public class TestProviderDirectResultEncodeFailure {

	@Test
	void asyncResultEncodeFailureRepliesExceptionImmediately() throws Exception {
		var config = new Config();
		config.setNoDatabase(true);
		config.setServiceManager("disable");
		var app = new Application("ProviderDirectAsyncEncodeFailure", config);
		try {
			var providerApp = new ProviderApp(app);
			var direct = new ProviderDirect();
			direct.providerApp = providerApp;

			var future = new RedirectFuture<Object>();
			var method = "TestProviderDirectResultEncodeFailure:asyncEncode";
			requireNonNull(app.redirect).handles.put(method, new RedirectHandle(
					TransactionLevel.None,
					(hash, params) -> future,
					r -> {
						throw new IllegalStateException("result encoder failure");
					}));

			var replyCount = new AtomicInteger();
			var replyCode = new AtomicLong(Long.MIN_VALUE);
			var replied = new CountDownLatch(1);
			var rpc = new ModuleRedirect() {
				@Override
				protected boolean sendResultCode(long code, @Nullable Binary result) {
					replyCode.set(code);
					replyCount.incrementAndGet();
					replied.countDown();
					return true;
				}
			};
			rpc.Argument.setMethodFullName(method);
			rpc.Argument.setParams(Binary.Empty);

			assertEquals(Procedure.Success, direct.ProcessModuleRedirectRequest(rpc));
			assertEquals(0, replyCount.get(), "async future must not reply before completion");

			var workerError = new AtomicReference<@Nullable Throwable>();
			var worker = new Thread(() -> {
				try {
					future.setResult(new Object());
				} catch (Throwable e) {
					workerError.set(e);
				}
			}, "redirect-async-result");
			worker.start();
			worker.join(5_000);

			assertFalse(worker.isAlive());
			assertNull(workerError.get(), "encode failure inside onSuccess must be caught and converted to an RPC error");
			assertTrue(replied.await(5, TimeUnit.SECONDS), "RPC must receive an immediate error reply without timing out");
			assertEquals(1, replyCount.get());
			assertEquals(Procedure.Exception, replyCode.get());
		} finally {
			app.stop();
		}
	}

	@Test
	void syncResultEncodeFailureRepliesExceptionOnce() throws Exception {
		var config = new Config();
		config.setNoDatabase(true);
		config.setServiceManager("disable");
		var app = new Application("ProviderDirectSyncEncodeFailure", config);
		try {
			var providerApp = new ProviderApp(app);
			var direct = new ProviderDirect();
			direct.providerApp = providerApp;

			var method = "TestProviderDirectResultEncodeFailure:syncEncode";
			requireNonNull(app.redirect).handles.put(method, new RedirectHandle(
					TransactionLevel.None,
					(hash, params) -> RedirectFuture.finish(new Object()),
					r -> {
						throw new IllegalStateException("sync result encoder failure");
					}));

			var replyCount = new AtomicInteger();
			var replyCode = new AtomicLong(Long.MIN_VALUE);
			var rpc = new ModuleRedirect() {
				@Override
				protected boolean sendResultCode(long code, @Nullable Binary result) {
					replyCode.set(code);
					replyCount.incrementAndGet();
					return true;
				}
			};
			rpc.Argument.setMethodFullName(method);
			rpc.Argument.setParams(Binary.Empty);

			assertEquals(Procedure.Success, direct.ProcessModuleRedirectRequest(rpc));
			assertEquals(1, replyCount.get());
			assertEquals(Procedure.Exception, replyCode.get());
		} finally {
			app.stop();
		}
	}
}
