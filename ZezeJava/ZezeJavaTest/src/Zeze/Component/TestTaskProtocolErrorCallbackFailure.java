package Zeze.Component;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Application;
import Zeze.Builtin.ProviderDirect.ModuleRedirect;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolErrorHandle;
import Zeze.Transaction.Procedure;
import Zeze.Util.OutObject;
import Zeze.Util.TaskSpec;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Fast
public class TestTaskProtocolErrorCallbackFailure {
	@Test
	public void functionErrorCallbackFailureKeepsOriginalResultAndRunsOnce() {
		var request = new ModuleRedirect();
		var callbacks = new AtomicInteger();
		var result = TaskSpec.ofFunc(() -> Procedure.LogicError, request,
				failingCallback(request, callbacks)).call();
		assertEquals(1, callbacks.get());
		assertEquals(Procedure.LogicError, result);
	}

	@Test
	public void procedureErrorCallbackFailureKeepsOriginalResultAndRunsOnce() throws Exception {
		var app = newApplication();
		try {
			var request = new ModuleRedirect();
			var callbacks = new AtomicInteger();
			var procedure = app.newProcedure(() -> Procedure.LogicError, "protocol-callback");
			var result = TaskSpec.ofProcedure(procedure, request, failingCallback(request, callbacks)).call();
			assertEquals(1, callbacks.get());
			assertEquals(Procedure.LogicError, result);
		} finally {
			app.stop();
		}
	}

	@Test
	public void decodedProcedureErrorCallbackFailureKeepsOriginalResultAndRunsOnce() throws Exception {
		var app = newApplication();
		try {
			var request = new ModuleRedirect();
			var out = new OutObject<Protocol<?>>();
			var callbacks = new AtomicInteger();
			var procedure = app.newProcedure(() -> {
				out.value = request;
				return Procedure.LogicError;
			}, "decoded-protocol-callback");
			var result = TaskSpec.ofProcedureOut(procedure, out, failingCallback(request, callbacks)).call();
			assertEquals(1, callbacks.get());
			assertEquals(Procedure.LogicError, result);
		} finally {
			app.stop();
		}
	}

	@Test
	public void businessExceptionStillNotifiesOnceWhenCallbackAlsoFails() {
		var request = new ModuleRedirect();
		var callbacks = new AtomicInteger();
		var result = TaskSpec.ofFunc(() -> {
			throw new IOException("business failed");
		}, request, (protocol, code) -> {
			assertSame(request, protocol);
			assertEquals(Procedure.Exception, code);
			callbacks.incrementAndGet();
			throw new IOException("callback failed");
		}).call();
		assertEquals(1, callbacks.get());
		assertEquals(Procedure.Exception, result);
	}

	@Test
	public void errorFromProtocolCallbackStillPropagates() {
		var callbacks = new AtomicInteger();
		var failure = new AssertionError("callback error");
		var thrown = assertThrows(AssertionError.class, () ->
				TaskSpec.ofFunc(() -> Procedure.LogicError, new ModuleRedirect(), (protocol, code) -> {
					callbacks.incrementAndGet();
					throw failure;
				}).call());
		assertSame(failure, thrown);
		assertEquals(1, callbacks.get());
	}

	private static ProtocolErrorHandle failingCallback(Protocol<?> expected, AtomicInteger callbacks) {
		return (protocol, code) -> {
			assertSame(expected, protocol);
			callbacks.incrementAndGet();
			throw new IOException("reply failed");
		};
	}

	private static Application newApplication() throws Exception {
		var config = TakeoverTestEnv.newConf("off", 60_000, 60_000);
		var app = new Application("TestTaskProtocolErrorCallbackFailure@" + config.getServerId(), config);
		app.start();
		return app;
	}
}
