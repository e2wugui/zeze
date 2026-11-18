package Zeze.Transaction;

import harness.Fast;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.Connection;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Fast
public class TestRedisTransactionResources {
	@Test
	void failedTransactionConstructionReturnsTheConnection() {
		var failure = new IllegalStateException("transaction construction failed");
		var closeFailure = new IllegalStateException("return to pool failed");
		var connection = new Jedis() {
			@Override
			public Connection getConnection() {
				throw failure;
			}

			@Override
			public void close() {
				throw closeFailure;
			}
		};
		try (var pool = new JedisPool() {
			@Override
			public Jedis getResource() {
				return connection;
			}
		}) {
			var thrown = assertThrows(IllegalStateException.class, () -> new DatabaseRedis.RedisTransaction(pool));
			assertSame(failure, thrown);
			assertEquals(1, thrown.getSuppressed().length);
			assertSame(closeFailure, thrown.getSuppressed()[0]);
		}
	}
}
