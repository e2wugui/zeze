package Zeze.Transaction;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import Zeze.Serialize.ByteBuffer;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 真实 Redis 集成测试：显式指定隔离服务的 -Dzeze.test.redis.port，未配置时由 JUnit 标记跳过。
 * 每例使用 UUID 字段，只清理自身字段；不执行 FLUSHDB，不连接默认 6379。
 */
@NullMarked
@EnabledIfSystemProperty(named = "zeze.test.redis.port", matches = "[1-9][0-9]*")
public class TestRedisSchemaVersionCas {
    private static final byte[] SCHEMA_HASH = "_ZezeDataWithVersion_".getBytes(StandardCharsets.UTF_8);

    @Test
    void firstInsertCanAdvanceThroughMultipleMatchingVersions() {
        try (var f = new Fixture()) {
            for (long version = 0; version < 8; version++) {
                var payload = bytes("schema-" + version);
                var result = f.ops.saveDataWithSameVersion(f.key, ByteBuffer.Wrap(payload), version);
                assertEquals(Boolean.TRUE, result.getValue(), "matching version " + version + " must advance");
                assertEquals(version + 1, result.getKey());
                assertStored(f, version + 1, payload);
            }
        }
    }

    @Test
    void staleVersionCannotChangeStoredData() {
        try (var f = new Fixture()) {
            var original = bytes("current");
            f.seed(64, original);
            var result = f.ops.saveDataWithSameVersion(f.key, ByteBuffer.Wrap(bytes("stale")), 63);
            assertEquals(Boolean.FALSE, result.getValue());
            assertEquals(63L, result.getKey());
            assertStored(f, 64, original);
        }
    }

    @Test
    void existingVariableLengthVersionsKeepTheirEncoding() {
        try (var f = new Fixture()) {
            for (long version : new long[] {0, 1, 63, 64, 8191, 8192, 1L << 40, Long.MAX_VALUE - 1}) {
                f.seed(version, bytes("previous-" + version));
                var payload = bytes("next-" + version);
                var result = f.ops.saveDataWithSameVersion(f.key, ByteBuffer.Wrap(payload), version);
                assertEquals(Boolean.TRUE, result.getValue(), "existing version " + version);
                assertEquals(version + 1, result.getKey());
                assertStored(f, version + 1, payload);
            }
        }
    }

    @Test
    void emptyDataCanStillAdvanceTheVersion() {
        try (var f = new Fixture()) {
            assertEquals(Boolean.TRUE, f.ops.saveDataWithSameVersion(f.key, ByteBuffer.Allocate(0), 0).getValue());
            assertEquals(Boolean.TRUE, f.ops.saveDataWithSameVersion(f.key, ByteBuffer.Allocate(0), 1).getValue());
            assertStored(f, 2, ByteBuffer.Empty);
        }
    }

    @Test
    void concurrentUpdatesOfOneExistingVersionHaveOnlyOneWinner() throws Exception {
        try (var f = new Fixture()) {
            f.seed(64, bytes("original"));
            assertSingleWinner(f, 64);
        }
    }

    @Test
    void concurrentFirstInsertsHaveOnlyOneWinner() throws Exception {
        try (var f = new Fixture()) {
            assertSingleWinner(f, 0);
        }
    }

    private static void assertSingleWinner(Fixture f, long version) throws Exception {
        // 暂停真实 HGET 返回，让两个调用拿到同一旧快照；EVAL 仍由真实 Redis 执行。
        // 这覆盖客户端读与服务端条件写之间的竞争窗口，不在测试中重写 Lua 行为。
        var reads = new CyclicBarrier(2);
        try (var pool = new BarrierPool(reads); var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var ops = new DatabaseRedis.OperatesRedis(pool);
            var left = executor.submit(() -> ops.saveDataWithSameVersion(f.key,
                    ByteBuffer.Wrap(bytes("left")), version));
            var right = executor.submit(() -> ops.saveDataWithSameVersion(f.key,
                    ByteBuffer.Wrap(bytes("right")), version));
            var leftResult = left.get(30, TimeUnit.SECONDS);
            var rightResult = right.get(30, TimeUnit.SECONDS);
            var leftWon = leftResult.getValue();
            var rightWon = rightResult.getValue();
            assertNotNull(leftWon);
            assertNotNull(rightWon);
            assertNotEquals(leftWon, rightWon, "exactly one CAS must succeed");
            assertStored(f, version + 1, bytes(leftWon ? "left" : "right"));
        }
    }

    private static void assertStored(Fixture f, long version, byte[] payload) {
        var actual = f.ops.getDataWithVersion(f.key);
        assertNotNull(actual);
        assertEquals(version, actual.version);
        assertNotNull(actual.data);
        assertArrayEquals(payload, actual.data.CopyIf());
        try (var jedis = f.pool.getResource()) {
            assertArrayEquals(encode(version, payload), jedis.hget(SCHEMA_HASH, f.key.CopyIf()),
                    "stored DataWithVersion format must stay byte-for-byte compatible");
        }
    }

    private static byte[] encode(long version, byte[] payload) {
        var value = new Database.DataWithVersion();
        value.data = ByteBuffer.Wrap(payload);
        value.version = version;
        var encoded = ByteBuffer.Allocate();
        value.encode(encoded);
        return encoded.CopyIf();
    }

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private static int port() {
        return Integer.parseInt(System.getProperty("zeze.test.redis.port"));
    }

    private static final class Fixture implements AutoCloseable {
        final JedisPool pool = new JedisPool(new JedisPoolConfig(), "127.0.0.1", port(), 10_000);
        final DatabaseRedis.OperatesRedis ops = new DatabaseRedis.OperatesRedis(pool);
        final ByteBuffer key = ByteBuffer.Wrap(bytes("schema-cas-" + UUID.randomUUID()));

        void seed(long version, byte[] payload) {
            try (var jedis = pool.getResource()) {
                jedis.hset(SCHEMA_HASH, key.CopyIf(), encode(version, payload));
            }
        }

        @Override
        public void close() {
            try (pool; var jedis = pool.getResource()) {
                jedis.hdel(SCHEMA_HASH, key.CopyIf());
            }
        }
    }

    private static final class BarrierPool extends JedisPool {
        private final CyclicBarrier reads;

        BarrierPool(CyclicBarrier reads) {
            super(new JedisPoolConfig(), "127.0.0.1", port(), 10_000);
            this.reads = reads;
        }

        @Override
        public Jedis getResource() {
            return new Jedis("127.0.0.1", port(), 10_000) {
                @Override
                public byte @Nullable [] hget(byte[] key, byte[] field) {
                    var result = super.hget(key, field);
                    try {
                        reads.await(20, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        throw new AssertionError("both real HGET reads must finish", e);
                    }
                    return result;
                }
            };
        }
    }
}
