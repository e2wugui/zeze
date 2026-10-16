package Zeze.Util;

import java.util.concurrent.CompletionException;
import java.util.concurrent.CancellationException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.function.LongFunction;
import Zeze.Config;
import Zeze.Net.Service;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestNonNullResultAccess {
	@Test
	public void pairNullableAccessRemainsNullable() {
		KV<String, @Nullable String> pair = KV.create("key", null);
		assertNull(pair.getValue());
		assertEquals(KV.create("key", null), pair);
	}

	@Test
	public void pairNonNullAccessPreservesIdentity() {
		var value = new Object();
		var pair = KVNotNull.create("key", value);
		assertSame(value, pair.getValue());
		assertEquals("key", pair.getKey());
		assertEquals(KVNotNull.create("key", value), pair);
		assertEquals(KV.create("key", value).hashCode(), pair.hashCode());
		assertEquals(KV.create("key", value).toString(), pair.toString());
	}

	@Test
	public void nonNullPairRejectsNullAtConstruction() throws Exception {
		var constructor = KVNotNull.class.getConstructor(Object.class, Object.class);
		assertInstanceOf(NullPointerException.class,
				assertThrows(InvocationTargetException.class, () -> constructor.newInstance("key", null)).getCause());
		assertInstanceOf(NullPointerException.class,
				assertThrows(InvocationTargetException.class, () -> constructor.newInstance(null, "value")).getCause());
	}

	@Test
	public void nonNullPairRetainsValueEqualityAndNullableFactoryResults() {
		var pairs = new HashSet<KVNotNull<String, String>>();
		pairs.add(KVNotNull.create("key", "value"));
		pairs.add(KVNotNull.create("key", "value"));
		assertEquals(1, pairs.size());
		KVNotNull<String, LongFunction<@Nullable String>> factory = KVNotNull.create("factory", index -> null);
		assertNull(factory.getValue().apply(1));
	}

	@Test
	public void addressAccessReturnsNonNullPairsAndRetainsOverrides() throws Exception {
		var service = new Service("NonNullAddressAccess", new Config()) {
			@Override
			public KVNotNull<String, Integer> getOneAcceptorAddress() {
				return KVNotNull.create("127.0.0.1", 0);
			}
		};
		try {
			var expected = KVNotNull.create("127.0.0.1", 0);
			assertEquals(expected, service.getOneAcceptorAddress());
			assertEquals(expected, service.getOnePassiveAddress());
			assertEquals(KVNotNull.class, Service.class.getMethod("getOneAcceptorAddress").getReturnType());
			assertEquals(KVNotNull.class, Service.class.getMethod("getOnePassiveAddress").getReturnType());
		} finally {
			service.stop();
		}
	}

	@Test
	public void completedNullFutureRemainsAValidResult() {
		var future = new TaskCompletionSource<@Nullable String>();
		assertTrue(future.setResult(null));
		assertNull(future.get());
		assertNull(future.get(0, TimeUnit.MILLISECONDS));
		assertNull(future.getNow());
		assertNull(future.join());
		assertSame(future, future.await());
		assertThrows(IllegalStateException.class, future::getNonNull);
		assertThrows(IllegalStateException.class, () -> future.getNonNull(0, TimeUnit.MILLISECONDS));
		assertNull(future.get());
	}

	@Test
	public void nonNullFutureAccessPreservesResultIdentity() {
		var value = new Object();
		var future = new TaskCompletionSource<>();
		assertTrue(future.setResult(value));
		assertSame(value, future.getNonNull());
		assertSame(value, future.getNonNull(0, TimeUnit.MILLISECONDS));
	}

	@Test
	public void nonNullFutureAccessPreservesFailureAndTimeout() {
		var future = new TaskCompletionSource<String>();
		assertThrows(TimeoutException.class, () -> future.getNonNull(0, TimeUnit.MILLISECONDS));
		assertFalse(future.isDone());
		var failure = new IllegalArgumentException("failure");
		assertTrue(future.setException(failure));
		assertSame(failure, assertThrows(CompletionException.class, future::getNonNull).getCause());
		assertSame(failure, assertThrows(CompletionException.class,
				() -> future.getNonNull(0, TimeUnit.MILLISECONDS)).getCause());
	}

	@Test
	public void nonNullFutureAccessPreservesCancellation() {
		var future = new TaskCompletionSource<String>();
		assertTrue(future.cancel(false));
		assertThrows(CancellationException.class, future::getNonNull);
		assertThrows(CancellationException.class, () -> future.getNonNull(0, TimeUnit.MILLISECONDS));
	}
}
