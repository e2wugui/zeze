package Game.AutoKey;

import Zeze.Transaction.Transaction;
import java.util.concurrent.ConcurrentHashMap;

// ZEZE_FILE_CHUNK {{{ IMPORT GEN
// ZEZE_FILE_CHUNK }}} IMPORT GEN

public class ModuleAutoKey extends AbstractModule {
    private static final int AllocateCount = 500;

    public void Start(Game.App app) throws Throwable {
    }

    public void Stop(Game.App app) throws Throwable {
    }

    private static class Range {
        private long nextId;
        private long max;

        public synchronized Long tryNextId() {
            if (nextId >= max) {
                return null;
            }
            return ++nextId;
        }

        public Range(long start, long end) {
            nextId = start;
            max = end;
        }
    }

    public class AutoKey {
        private final String name;
        private volatile Range range;
        private long logKey;

        public AutoKey(String name) {
            this.name = name;

            // 详细参考Bean的Log的用法。这里只有一个variable。
            logKey = Zeze.Transaction.Bean.getNextObjectId();
        }

        public long nextId() {
            if (null != range) {
                var next = range.tryNextId();
                if (null != next)
                    return next; // allocate in range success
            }

            var txn = Transaction.getCurrent();
            var log = (RangeLog)txn.GetLog(logKey);
            while (true) {
                if (null == log) {
                    // allocate 多线程，多事务，多服务器（缓存同步）由zeze保证。
                    var key = _tautokeys.getOrAdd(name);
                    var start = key.getNextId();
                    var end = start + AllocateCount; // AllocateCount == 0 会死循环。
                    key.setNextId(end);
                    // create log，本事务可见，
                    log = new RangeLog();
                    log.range = new Range(start, end);
                    txn.PutLog(log);
                }
                var trynext = log.range.tryNextId();
                if (null != trynext)
                    return trynext;

                // 事务内分配了超出Range范围的id，再次allocate。
                // 覆盖RangeLog是可以的。就像事务内多次改变变量。最后面的Log里面的数据是最新的。
                // 已分配的范围保存在_autokeys表内，事务内可以继续分配。
                log = null;
            }
        }

        private class RangeLog extends Zeze.Transaction.Log {
            private Range range;

            public RangeLog() {
                super(null); // null: 特殊日志，不关联Bean。
            }

            @Override
            public void Commit() {
                // 这里直接修改拥有者的引用，开放出去，以后其他事务就能看到新的Range了。
                // 并发：多线程实际上由 _autokeys 表的锁来达到互斥，commit的时候，是互斥锁。
                AutoKey.this.range = range;
            }

            @Override
            public long getLogKey() {
                return AutoKey.this.logKey;
            }
        }
    }

    private final ConcurrentHashMap<String, AutoKey> map = new ConcurrentHashMap<>();

    /**
     * 这个返回值，可以在自己模块内保存下来，效率高一些。
     * @param name
     * @return
     */
    public static AutoKey getAutoKey(String name) {
        return Game.App.Instance.Game_AutoKey._getAutoKey(name);
    }

    private AutoKey _getAutoKey(String name) {
        return map.computeIfAbsent(name, AutoKey::new);
    }

    // ZEZE_FILE_CHUNK {{{ GEN MODULE
    public static final int ModuleId = 10;

    private tautokeys _tautokeys = new tautokeys();

    public Game.App App;

    public ModuleAutoKey(Game.App app) {
        App = app;
        // register protocol factory and handles
        var _reflect = new Zeze.Util.Reflect(this.getClass());
        // register table
        App.Zeze.AddTable(App.Zeze.getConfig().GetTableConf(_tautokeys.getName()).getDatabaseName(), _tautokeys);
    }

    public void UnRegister() {
        App.Zeze.RemoveTable(App.Zeze.getConfig().GetTableConf(_tautokeys.getName()).getDatabaseName(), _tautokeys);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE

}
