package Zeze.Util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class PersistentAtomicLong {
    private final AtomicLong currentId = new AtomicLong();
    private volatile long allocated;

    private final String FileName;
    private final int AllocateSize;

    private static ConcurrentHashMap<String, PersistentAtomicLong> pals = new ConcurrentHashMap<>();

    /**
     * 【小优化】请保存返回值，重复使用。
     *
     * @param ProgramInstanceName
     * 程序实例名字。
     * 当进程只有一份实例时，可以直接使用程序名。
     * 有多个实例时，需要另一个Id区分，这里不能使用进程id（pid），需要稳定的。
     * 对于网络程序，可以使用"进程名+Main.Acceptor.Name"
     */
    public static PersistentAtomicLong getOrAdd(final String ProgramInstanceName, final int allocateSize) {
        var name = ProgramInstanceName.replace(':', '.');
        // 这样写，不小心重名也能工作。
        return pals.computeIfAbsent(name, (k) -> new PersistentAtomicLong(k, allocateSize));
    }

    public static PersistentAtomicLong getOrAdd(final String ProgramInstanceName) {
        return getOrAdd(ProgramInstanceName, 5000);
    }

    private PersistentAtomicLong(String ProgramInstanceName, int allocateSize) {
        if (allocateSize <= 0)
            throw new IllegalArgumentException();

        FileName = ProgramInstanceName + ".PersistentAtomicLong.txt";
        AllocateSize = allocateSize;

        if (Files.exists(Path.of(FileName))) {
            try (var r = new BufferedReader(new FileReader(FileName))) {
                var line = r.readLine();
                if (null != line) {
                    allocated = Long.parseLong(line);
                    currentId.set(allocated);
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        // 初始化的时候不allocate，如果程序启动，没有分配就退出，保持原来的值。
    }

    public long next() {
        for (;;) {
            long current = currentId.get();
            if (current >= allocated) {
                allocate();
                continue;
            }
            if (currentId.compareAndSet(current, current + 1))
                return current + 1;
        }
    }

    private void allocate() {
        synchronized (this) {
            if (currentId.get() < allocated)
                return; // has allocated. concurrent.

            allocated += AllocateSize;
            // 应该尽量减少allocate的次数，所以这里文件就不保持打开了。
            try (var pw = new FileWriter(FileName)) {
                pw.write(String.valueOf(allocated));
            }
            catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
