package Infinite;

import Zeze.Transaction.Transaction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class Tasks {
    static ConcurrentHashMap<String, AtomicLong> TaskCounterRun = new ConcurrentHashMap<>();
    static ConcurrentHashMap<String, AtomicLong> TaskCounterSuccess = new ConcurrentHashMap<>();
    static void increace(Task task) {
        getSuccessTaskCounter(task.getClass().getName()).incrementAndGet();
    }
    static AtomicLong getSuccessTaskCounter(String name) {
        return TaskCounterSuccess.computeIfAbsent(name, (key) -> new AtomicLong());
    }
    static AtomicLong getRunTaskCounter(String name) {
        return TaskCounterRun.computeIfAbsent(name, (key) -> new AtomicLong());
    }

    // 所有以long为key的记录访问可以使用这个基类。
    // 其他类型的key需要再定义新的基类。
    public static abstract class Task implements Runnable {
        long Key;
        demo.App App;

        @Override
        public void run() {
            getRunTaskCounter(this.getClass().getName()).incrementAndGet();
            if (0L == process()) {
                Transaction.getCurrent().RunWhileCommit(() -> increace(this));
            }
        }

        public abstract long process();
    }

    public static class TaskFactory {
        public Zeze.Util.Factory<Task> Factory;
        public int Weight;
        public Zeze.Util.Action0 Verify = null;

        public TaskFactory(Zeze.Util.Factory<Task> factory, int weight) {
            Factory = factory;
            Weight = weight;
        }

        public TaskFactory(Zeze.Util.Factory<Task> factory, int weight, Zeze.Util.Action0 verify) {
            Factory = factory;
            Weight = weight;
            Verify = verify;
        }
    }

    static ArrayList<TaskFactory> taskFactorys = new ArrayList<>();
    static int TotalWeight = 0;
    static {
        // 新的操作数据的测试任务在这里注册。weith是权重，see randCreateTask();
        taskFactorys.add(new TaskFactory(Table1Long2Add1::new, 100, Table1Long2Add1::verify));
        taskFactorys.add(new TaskFactory(Table1List9AddOrRemove::new, 100, Table1List9AddOrRemove::verify));

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

    static void verifyBatch() {
        for (var tf : taskFactorys) {
            if (null != tf.Verify) {
                try {
                    tf.Verify.run();
                } catch (Throwable e) {
                    App.logger.error(e);
                }
            }
        }
    }

    public static class Table1Long2Add1 extends Task {
        @Override
        public long process() {
            var value = App.demo_Module1.getTable1().getOrAdd(Key);
            value.setLong2(value.getLong2() + 1);
            return 0L;
        }

        public static void verify() {
            // verify 时，所有任务都执行完毕，不需要考虑并发。
            long total = 0L;
            for (long key = 0; key < Infinite.App.AccessKeyBound; ++key) {
                for (var app : Simulate.Apps) {
                    total += app.app.demo_Module1.getTable1().selectDirty(key).getLong2();
                }
            }
            if (total != Simulate.BatchTaskCount * Simulate.BatchNumber) {
                Infinite.App.logger.error(Table1List9AddOrRemove.class.getName());
                System.exit(123);
            }
        }
    }

    public static class Table1List9AddOrRemove extends Task {
        @Override
        public long process() {
            var value = App.demo_Module1.getTable1().getOrAdd(Key);
            // 使用 bool4 变量：用来决定添加或者删除。
            if (value.isBool4()) {
                Infinite.App.logger.error("list9.size()=" + value.getList9().size());
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

        public static void verify() {
            var name= Table1List9AddOrRemove.class.getName();
            if (getSuccessTaskCounter(name).get() != getRunTaskCounter(name).get()) {
                Infinite.App.logger.error(name);
                System.exit(123);
            }
        }
    }
}
