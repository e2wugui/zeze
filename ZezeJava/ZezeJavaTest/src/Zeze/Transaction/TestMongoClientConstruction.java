package Zeze.Transaction;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import harness.Fast;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import Zeze.Config;

@Fast
public class TestMongoClientConstruction {
	private static <T> T proxy(Class<T> type, InvocationHandler handler) {
		return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
	}

	private static MongoClient client(AtomicInteger closes, RuntimeException failure,
			boolean failInSchema, @Nullable RuntimeException closeFailure) {
		var database = proxy(MongoDatabase.class, (__, method, ___) -> {
			if (method.getName().equals("createCollection"))
				throw failure;
			throw new AssertionError("unexpected database call: " + method.getName());
		});
		return proxy(MongoClient.class, (__, method, ___) -> {
			return switch (method.getName()) {
				case "getDatabase" -> {
					if (!failInSchema)
						throw failure;
					yield database;
				}
				case "close" -> {
					closes.incrementAndGet();
					if (closeFailure != null)
						throw closeFailure;
					yield null;
				}
				default -> throw new AssertionError("unexpected client call: " + method.getName());
			};
		});
	}

	private static void verifyFailure(boolean inSchema, boolean failClose) {
		var closes = new AtomicInteger();
		var failure = new IllegalStateException("initialization failed");
		var closeFailure = failClose ? new IllegalStateException("close failed") : null;
		var client = client(closes, failure, inSchema, closeFailure);
		var thrown = Assertions.assertThrows(IllegalStateException.class,
				() -> new DatabaseMongoDb(null, new Config.DatabaseConf(), client));
		Assertions.assertSame(failure, thrown);
		Assertions.assertEquals(1, closes.get(), "unpublished client must be released by the constructor");
		if (closeFailure != null)
			Assertions.assertArrayEquals(new Throwable[]{closeFailure}, thrown.getSuppressed());
	}

	@Test
	public void databaseSelectionFailureClosesTheClient() {
		verifyFailure(false, false);
	}

	@Test
	public void metadataInitializationFailureClosesTheClient() {
		verifyFailure(true, false);
	}

	@Test
	public void cleanupFailureDoesNotReplaceTheInitializationFailure() {
		verifyFailure(true, true);
	}
}
