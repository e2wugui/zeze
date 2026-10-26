package Zeze.Transaction;

import com.mongodb.client.ClientSession;
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
public class TestMongoSessionCreation {
	private static <T> T proxy(Class<T> type, InvocationHandler handler) {
		return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
	}

	private static void verifyFailure(@Nullable RuntimeException closeFailure) {
		var closes = new AtomicInteger();
		var failure = new IllegalStateException("start transaction failed");
		var session = proxy(ClientSession.class, (__, method, ___) -> {
			return switch (method.getName()) {
				case "startTransaction" -> throw failure;
				case "close" -> {
					closes.incrementAndGet();
					if (closeFailure != null)
						throw closeFailure;
					yield null;
				}
				default -> throw new AssertionError("unexpected session call: " + method.getName());
			};
		});
		var database = proxy(MongoDatabase.class, (__, method, ___) -> {
			throw new AssertionError("unexpected database call: " + method.getName());
		});
		var client = proxy(MongoClient.class, (__, method, ___) -> {
			return switch (method.getName()) {
				case "getDatabase" -> database;
				case "startSession" -> session;
				case "close" -> null;
				default -> throw new AssertionError("unexpected client call: " + method.getName());
			};
		});
		var config = new Config.DatabaseConf();
		config.setDisableOperates(true);
		var db = new DatabaseMongoDb(null, config, client);
		try {
			var thrown = Assertions.assertThrows(IllegalStateException.class, db::beginTransaction);
			Assertions.assertSame(failure, thrown);
			Assertions.assertEquals(1, closes.get(), "session must be released before ownership can reach the caller");
			if (closeFailure != null)
				Assertions.assertArrayEquals(new Throwable[]{closeFailure}, thrown.getSuppressed());
		} finally {
			db.close();
		}
	}

	@Test
	public void failedTransactionStartReleasesTheSession() {
		verifyFailure(null);
	}

	@Test
	public void failedSessionCloseIsSuppressedByTheStartFailure() {
		verifyFailure(new IllegalStateException("close failed"));
	}
}
