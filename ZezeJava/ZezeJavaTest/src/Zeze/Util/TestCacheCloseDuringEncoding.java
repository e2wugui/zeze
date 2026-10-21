package Zeze.Util;

import java.io.File;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.rocksdb.RocksDB;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestCacheCloseDuringEncoding {
    // A closed native RocksDB handle can terminate the JVM before Java catches anything.
    // Run this regression in a child, so the original implementation only fails this test.
    @Test
    void closeDuringEncodingRejectsTheWrite(@TempDir Path dir) throws Exception {
        runChild(dir, "concurrent");
    }

    @Test
    void encoderMayCloseItsCacheWithoutDeadlock(@TempDir Path dir) throws Exception {
        runChild(dir, "reentrant");
    }

    private static void runChild(Path dir, String scenario) throws Exception {
        var entries = new LinkedHashSet<String>();
        for (var loader = TestCacheCloseDuringEncoding.class.getClassLoader(); loader != null;
             loader = loader.getParent()) {
            if (loader instanceof URLClassLoader urls) {
                for (var url : urls.getURLs())
                    entries.add(Path.of(url.toURI()).toString());
            }
        }
        entries.addAll(java.util.List.of(System.getProperty("java.class.path").split(File.pathSeparator)));
        var output = dir.resolve("child.log");
        var process = new ProcessBuilder(ProcessHandle.current().info().command().orElseThrow(),
                "-ea", "-XX:-CreateCoredumpOnCrash", "-XX:ErrorFile=" + dir.resolve("hs_err_pid%p.log"),
                "-cp", String.join(File.pathSeparator, entries),
                TestCacheCloseDuringEncoding.class.getName(), dir.toString(), scenario)
                .redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try {
            assertTrue(process.waitFor(20, TimeUnit.SECONDS), "cache close/encoder must finish");
            assertEquals(0, process.exitValue(), () -> {
                try { return Files.readString(output); }
                catch (Exception e) { return e.toString(); }
            });
            assertTrue(Files.readString(output).contains("CLOSED_WRITE_REJECTED"));
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                assertTrue(process.waitFor(5, TimeUnit.SECONDS));
            }
        }
    }

    public static void main(String[] args) throws Exception {
        RocksDB.loadLibrary();
        Task.tryInitThreadPool();
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var cacheRef = new AtomicReference<@Nullable Cache>();
        var thrown = new AtomicReference<@Nullable Throwable>();
        boolean reentrant = args[1].equals("reentrant");
        var cache = new Cache(Path.of(args[0], "db").toString(), 4, id -> new CacheObject() {
            @Override public String cacheId() { return id; }
            @Override public void encode(ByteBuffer bb) {
                entered.countDown();
                try {
                    if (reentrant) {
                        var current = cacheRef.get();
                        assert current != null; // Published before the getter starts.
                        current.close();
                    } else if (!release.await(10, TimeUnit.SECONDS))
                        throw new AssertionError("encoder release timed out");
                } catch (Exception e) { throw Task.forceThrow(e); }
                bb.WriteString("value");
            }
            @Override public void decode(IByteBuffer bb) { throw new UnsupportedOperationException(); }
        }, (id, bb) -> { throw new AssertionError("first miss must use loader"); });
        cacheRef.set(cache);
        var getter = new Thread(() -> {
            try { cache.get("key"); }
            catch (Throwable e) { thrown.set(e); }
        }, "cache-encoder");
        getter.setDaemon(true);
        getter.start();
        try {
            if (!entered.await(10, TimeUnit.SECONDS))
                throw new AssertionError("encoder never entered");
            if (!reentrant)
                cache.close();
        } finally {
            release.countDown();
            getter.join(10_000);
            cache.close();
        }
        if (getter.isAlive())
            throw new AssertionError("encoder/getter did not finish");
        if (!(thrown.get() instanceof IllegalStateException ex) || !ex.getMessage().contains("cache is closed"))
            throw new AssertionError("expected clean closed failure", thrown.get());
        System.out.println("CLOSED_WRITE_REJECTED");
    }
}
