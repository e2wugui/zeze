package Infinite;

import Zeze.Transaction.DatabaseMemory;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class Tasks {
    static ConcurrentHashMap<String, ConcurrentHashMap<Long, AtomicLong>> CounterRun = new ConcurrentHashMap<>();
    static ConcurrentHashMap<String, ConcurrentHashMap<Long, AtomicLong>> CounterSuccess = new ConcurrentHashMap<>();

    static ConcurrentHashMap<Long, AtomicLong> getSuccessCounters(String name) {
        return CounterSuccess.computeIfAbsent(name, (_key) -> new ConcurrentHashMap());
    }
    static AtomicLong getSuccessCounter(String name, long key) {
        return getSuccessCounters(name).computeIfAbsent(key, (_key) -> new AtomicLong());
    }

    static ConcurrentHashMap<Long, AtomicLong> getRunCounters(String name) {
        return CounterRun.computeIfAbsent(name, (_key) -> new ConcurrentHashMap());
    }

    static AtomicLong getRunCounter(String name, long key) {
        return getRunCounters(name).computeIfAbsent(key, (_key) -> new AtomicLong());
    }

    // 所有以long为key的记录访问可以使用这个基类。
    // 其他类型的key需要再定义新的基类。
    public static abstract class Task implements Zeze.Util.Func0<Long> {
        Set<Long> Keys = new HashSet<>();
        demo.App App;

        @Override
        public Long call() {
            var result = process();
            if (0L == result) {
                Transaction.getCurrent().RunWhileCommit(() -> {
                    for (var key : Keys)
                        getSuccessCounter(this.getClass().getName(), key).incrementAndGet();
                });
            }
            return result;
        }

        public int getKeyNumber() {
            return 1;
        }

        public int getKeyBound() {
            return Simulate.AccessKeyBound;
        }

        public abstract long process();

        public boolean IsProcedure() {
            return true;
        }
    }

    public static class TaskFactory {
        public Class<?> Class;
        public Zeze.Util.Factory<Task> Factory;
        public int Weight;

        public TaskFactory(Class<?> cls, Zeze.Util.Factory<Task> factory, int weight) {
            Class = cls;
            Factory = factory;
            Weight = weight;
        }
    }

    static ArrayList<TaskFactory> taskFactorys = new ArrayList<>();
    static int TotalWeight = 0;
    static {
        // 新的操作数据的测试任务在这里注册。weith是权重，see randCreateTask();
        taskFactorys.add(new TaskFactory(Table1Long2Add1.class, Table1Long2Add1::new, 100));
        taskFactorys.add(new TaskFactory(Table1List9AddOrRemove.class, Table1List9AddOrRemove::new, 100));
        taskFactorys.add(new TaskFactory(Table1Int1Trade.class, Table1Int1Trade::new, 100));
        //taskFactorys.add(new TaskFactory(Table1Int1TradeConcurrentVerify.class, Table1Int1TradeConcurrentVerify::new, 100));

        taskFactorys.sort(Comparator.comparingInt(a -> a.Weight));
        for (var task : taskFactorys) {
            TotalWeight += task.Weight;
        }
    }

    static Task randCreateTask() {
        var rand = Zeze.Util.Random.getInstance().nextInt(TotalWeight);
        for (var task : taskFactorys) {
            if (rand < task.Weight)
                return task.Factory.create();
            rand -= task.Weight;
        }
        throw new RuntimeException("impossible");
    }

    static void prepare() throws Throwable {
        for (var tf : taskFactorys) {
            try {
                var prepare = tf.Class.getMethod("prepare");
                prepare.invoke(null);
            } catch (NoSuchMethodException skip) {
            } catch (Throwable ex) {
                throw ex;
            }
        }
    }

    static void verifyBatch() throws Throwable {
        for (var tf : taskFactorys) {
            try {
                var verify = tf.Class.getMethod("verify");
                verify.invoke(null);
            } catch (NoSuchMethodException skip) {
                // verify default.
                var name = tf.Factory.create().getClass().getName();
                var runs = getRunCounters(name);
                var success = getSuccessCounters(name);
                assert runs.size() == success.size();
                for (var r : runs.entrySet()) {
                    var s = success.get(r.getKey());
                    assert null != s;
                    // ignore toomanytrys error
                    var toomanytrys = Zeze.Transaction.ProcedureStatistics.getInstance().GetOrAdd(tf.Class.getName()).GetOrAdd(Procedure.TooManyTry).get();
                    assert r.getValue().get() == s.get() + toomanytrys;
                    if (toomanytrys != 0)
                        Infinite.App.logger.fatal("TOOMANYTRS=" + toomanytrys + " " + tf.Class.getName());
                }
            } catch (Throwable ex) {
                throw ex;
            }
        }
    }

    public static class Table1Long2Add1 extends Task {
        @Override
        public long process() {
            var value = App.demo_Module1.getTable1().getOrAdd(Keys.iterator().next());
            value.setLong2(value.getLong2() + 1);
            return 0L;
        }

        public static void verify() {

            // verify 时，所有任务都执行完毕，不需要考虑并发。
            var name = Table1Long2Add1.class.getName();
            var app = Simulate.randApp().app; // 任何一个app都能查到相同的结果。
            var success = getSuccessCounters(name);
            for (var e : getRunCounters(name).entrySet()) {
                assert app.demo_Module1.getTable1().selectDirty(e.getKey()).getLong2() == success.get(e.getKey()).get();
            }
            Infinite.App.logger.debug("Table1Long2Add1.verify Ok.");
        }

        public static void prepare() throws Throwable {
            // 所有使用 Table1 的测试都可以依赖这个 prepare，不需要单独写了。
            var app = Simulate.randApp().app;
            app.Zeze.NewProcedure(() -> {
                for (long key = 0; key < Simulate.AccessKeyBound; ++key) {
                    app.demo_Module1.getTable1().remove(key);
                }
                return 0L;
            }, "Table1Long2Add1.prepare").Call();
        }
    }

    public static class Table1List9AddOrRemove extends Task {
        @Override
        public long process() {
            var value = App.demo_Module1.getTable1().getOrAdd(Keys.iterator().next());
            // 使用 bool4 变量：用来决定添加或者删除。
            if (value.isBool4()) {
                //Infinite.App.logger.error("list9.size()=" + value.getList9().size());
                if (!value.getList9().isEmpty())
                    value.getList9().remove(value.getList9().size() - 1);

                value.setBool4(!value.getList9().isEmpty());
            } else {
                value.getList9().add(new demo.Bean1());
                if (value.getList9().size() > 50)
                    value.setBool4(true); // 改成删除模式。
            }
            return 0L;
        }
    }

    // 在随机两个记录内进行交易。
    public static class Table1Int1Trade extends Task {
        @Override
        public int getKeyNumber() {
            return 2;
        }

        public final static int KeyBoundTrade = Simulate.AccessKeyBound;

        @Override
        public int getKeyBound() {
            return KeyBoundTrade;
        }

        @Override
        public long process() {
            final var keys = Keys.toArray();
            final var k1 = (long)keys[0];
            final var k2 = (long)keys[1];
            var v1 = App.demo_Module1.getTable1().getOrAdd(k1);
            var v2 = App.demo_Module1.getTable1().getOrAdd(k2);
            final var money = Zeze.Util.Random.getInstance().nextInt(1000);
            if (Zeze.Util.Random.getInstance().nextBoolean()) {
                // random swap
                var tmp = v1;
                v1 = v2;
                v2 = tmp;
            }
            v1.setInt1(v1.getInt1() - money);
            v2.setInt1(v2.getInt1() + money);
            return 0L;
        }

        public static void verify() {
            var app = Simulate.randApp().app; // 任何一个app都能查到相同的结果。
            int sum = 0;
            for (int key = 0; key < KeyBoundTrade; ++key) {
                var value = app.demo_Module1.getTable1().selectDirty((long)key);
                if (null != value)
                    sum += value.getInt1();
            }
            assert sum == 0;
        }
    }

    public static class Table1Int1TradeConcurrentVerify extends Task {
        @Override
        public int getKeyNumber() {
            return 0;
        }
        public boolean IsProcedure() {
            return false;
        }

        @Override
        public long process() {
            final var table1 = App.demo_Module1.getTable1();
            var keys = new HashSet<Zeze.Serialize.ByteBuffer>();
            for (int key = 0; key < Table1Int1Trade.KeyBoundTrade; ++key) {
                keys.add(table1.EncodeKey((long)key));
            }
            try (var t = table1.getDatabase().BeginTransaction()) {
                if (t instanceof DatabaseMemory.MemTrans) {
                    final var mt = (DatabaseMemory.MemTrans)t;
                    final var all = mt.Finds(table1.getName(), keys);
                    final var values = new ArrayList<demo.Module1.Value>();
                    for (var valueBytes : all.values()) {
                        if (valueBytes != null)
                            values.add(table1.DecodeValue(valueBytes));
                    }
                    int sum = 0;
                    for (var value : values) {
                        sum += value.getInt1();
                    }
                    assert sum == 0;
                }
            } catch (Exception e) {
                Infinite.App.logger.error(e);
                assert false;
            }
            return 0L;
        }
    }
}
