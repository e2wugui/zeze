using demo;
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Diagnostics;
using System.Threading.Tasks;
using Zeze.Transaction;
using Zeze.Util;

namespace Infinite
{
    public class Tasks
    {
        static readonly ConcurrentDictionary<string, ConcurrentDictionary<long, AtomicLong>> CounterRun = new();
        static readonly ConcurrentDictionary<string, ConcurrentDictionary<long, AtomicLong>> CounterSuccess = new();

        internal static ConcurrentDictionary<long, AtomicLong> GetSuccessCounters(string name)
        {
            return CounterSuccess.GetOrAdd(name, _ => new());
        }

        internal static AtomicLong GetSuccessCounter(string name, long key)
        {
            return GetSuccessCounters(name).GetOrAdd(key, _ => new());
        }

        internal static ConcurrentDictionary<long, AtomicLong> GetRunCounters(string name)
        {
            return CounterRun.GetOrAdd(name, _ => new());
        }

        internal static AtomicLong GetRunCounter(string name, long key)
        {
            return GetRunCounters(name).GetOrAdd(key, _ => new());
        }

        // 所有以long为key的记录访问可以使用这个基类。
        // 其他类型的key需要再定义新的基类。
        public abstract class Task
        {
            internal HashSet<long> Keys = new();
            internal demo.App App;

            public virtual void Run()
            {
                Simulate.randApp().Run(this);
            }

            public async Task<long> Call()
            {
                var result = await Process();
                if (0L == result)
                {
                    var txn = Transaction.Current;
                    if (null != txn)
                    {
                        txn.RunWhileCommit(() =>
                        {
                            foreach (var key in Keys)
                                GetSuccessCounter(GetType().FullName, key).IncrementAndGet();
                        });
                    }
                    else
                    {
                        foreach (var key in Keys)
                            GetSuccessCounter(GetType().FullName, key).IncrementAndGet();
                    }
                }
                return result;
            }

            public virtual int GetKeyNumber()
            {
                return 1;
            }

            public virtual int GetKeyBound()
            {
                return Simulate.AccessKeyBound;
            }

            public abstract Task<long> Process();

            public virtual bool IsProcedure()
            {
                return true;
            }
        }

        public class TaskFactory
        {
            public Type Class;
            public Func<Task> Factory;
            public int Weight;

            public TaskFactory(Type cls, Func<Task> factory, int weight)
            {
                Class = cls;
                Factory = factory;
                Weight = weight;
            }
        }

        static readonly List<TaskFactory> taskFactorys = new();
        static readonly int TotalWeight = 0;
        static Tasks()
        {
            // 新的操作数据的测试任务在这里注册。weith是权重，see randCreateTask();
            taskFactorys.Add(new TaskFactory(typeof(Table1Long2Add1), () => new Table1Long2Add1(), 100));
            taskFactorys.Add(new TaskFactory(typeof(Table1List9AddOrRemove), () => new Table1List9AddOrRemove(), 100));
            taskFactorys.Add(new TaskFactory(typeof(TflushInt1Trade), () => new TflushInt1Trade(), 100));
            taskFactorys.Add(new TaskFactory(typeof(TflushInt1TradeConcurrentVerify), () => new TflushInt1TradeConcurrentVerify(), 100));

            taskFactorys.Sort((a, b) => a.Weight - b.Weight);
            foreach (var task in taskFactorys)
                TotalWeight += task.Weight;
        }

        internal static Task RandCreateTask()
        {
            var rand = Zeze.Util.Random.Instance.Next(TotalWeight);
            foreach (var task in taskFactorys)
            {
                if (rand < task.Weight)
                    return task.Factory.Invoke();
                rand -= task.Weight;
            }
            throw new Exception("impossible");
        }

        internal static void Prepare()
        {
            foreach (var tf in taskFactorys)
            {
                var prepare = tf.Class.GetMethod("prepare");
                if (prepare != null)
                    prepare.Invoke(null, null);
            }
        }

        internal static void VerifyBatch()
        {
            foreach (var tf in taskFactorys)
            {
                var verify = tf.Class.GetMethod("verify");
                if (verify != null)
                    verify.Invoke(null, null);
                else
                {
                    // verify default.
                    var name = tf.Factory.Invoke().GetType().FullName;
                    var runs = GetRunCounters(name);
                    var success = GetSuccessCounters(name);
                    Debug.Assert(runs.Count == success.Count);
                    foreach (var r in runs)
                    {
                        success.TryGetValue(r.Key, out var s);
                        Debug.Assert(null != s);
                        // ignore toomanytrys error
                        var toomanytrys = ProcedureStatistics.Instance.GetOrAdd(tf.Class.FullName).GetOrAdd(Procedure.TooManyTry).Get();
                        Debug.Assert(r.Value.Get() == s.Get() + toomanytrys);
                        if (toomanytrys != 0)
                            App.logger.Fatal("TOOMANYTRS=" + toomanytrys + " " + tf.Class.FullName);
                    }
                }
            }
        }

        public class Table1Long2Add1 : Task
        {
            public override async Task<long> Process()
            {
                var keyEnum = Keys.GetEnumerator();
                keyEnum.MoveNext();
                var value = await App.demo_Module1.Table1.GetOrAddAsync(keyEnum.Current);
                value.Long2++;
                return 0L;
            }

            public static async void Verify()
            {
                // verify 时，所有任务都执行完毕，不需要考虑并发。
                var name = typeof(Table1Long2Add1).FullName;
                var app = Simulate.randApp().app; // 任何一个app都能查到相同的结果。
                var success = GetSuccessCounters(name);
                foreach (var e in GetRunCounters(name))
                    Debug.Assert((await app.demo_Module1.Table1.SelectDirtyAsync(e.Key)).Long2 == success[e.Key].Get());
                Infinite.App.logger.Debug("Table1Long2Add1.verify Ok.");
            }

            public static void Prepare()
            {
                // 所有使用 Table1 的测试都可以依赖这个 prepare，不需要单独写了。
                var app = Simulate.randApp().app;
                app.Zeze.NewProcedure(async () =>
                {
                    for (long key = 0; key < Simulate.AccessKeyBound; ++key)
                        await app.demo_Module1.Table1.RemoveAsync(key);
                    return 0L;
                }, "Table1Long2Add1.prepare").CallSynchronously();
            }
        }

        public class Table1List9AddOrRemove : Task
        {
            public override async Task<long> Process()
            {
                var keyEnum = Keys.GetEnumerator();
                keyEnum.MoveNext();
                var value = await App.demo_Module1.Table1.GetOrAddAsync(keyEnum.Current);
                // 使用 bool4 变量：用来决定添加或者删除。
                if (value.Bool4)
                {
                    //App.logger.error("list9.size()=" + value.getList9().size());
                    if (value.List9.Count > 0)
                        value.List9.RemoveAt(value.List9.Count - 1);

                    value.Bool4 = value.List9.Count > 0;
                }
                else
                {
                    value.List9.Add(new Bean1());
                    if (value.List9.Count > 50)
                        value.Bool4 = true; // 改成删除模式。
                }
                return 0L;
            }
        }

        // 在随机两个记录内进行交易。
        public class TflushInt1Trade : Task
        {
            public override int GetKeyNumber()
            {
                return 2;
            }

            public override void Run()
            {
                Simulate.randApp(2).Run(this);
            }

            public const int KeyBoundTrade = Simulate.AccessKeyBound / 2;
            public const int CacheCapacity = Simulate.CacheCapacity / 2;

            public override int GetKeyBound()
            {
                return KeyBoundTrade;
            }

            public override async Task<long> Process()
            {
                var keyEnum = Keys.GetEnumerator();
                keyEnum.MoveNext();
                var k1 = keyEnum.Current;
                keyEnum.MoveNext();
                var k2 = keyEnum.Current;
                var v1 = await App.demo_Module1.Tflush.GetOrAddAsync(k1);
                var v2 = await App.demo_Module1.Tflush.GetOrAddAsync(k2);
                var money = Zeze.Util.Random.Instance.Next(1000);
                if ((Zeze.Util.Random.Instance.Next() & 1) == 0)
                    (v2, v1) = (v1, v2); // random swap
                v1.Int1 -= money;
                v2.Int1 += money;
                return 0L;
            }

            public static async void Verify()
            {
                var app = Simulate.randApp().app; // 任何一个app都能查到相同的结果。
                int sum = 0;
                for (int key = 0; key < KeyBoundTrade; ++key)
                {
                    var value = await app.demo_Module1.Table1.SelectDirtyAsync((long)key);
                    if (null != value)
                        sum += value.Int1;
                }
                Debug.Assert(sum == 0);
            }
        }

        public class TflushInt1TradeConcurrentVerify : Task
        {
            public override int GetKeyNumber()
            {
                return 0;
            }

            public override bool IsProcedure()
            {
                return false;
            }

#pragma warning disable CS1998 // Async method lacks 'await' operators and will run synchronously
            public override async Task<long> Process()
#pragma warning restore CS1998 // Async method lacks 'await' operators and will run synchronously
            {
                var table1 = App.demo_Module1.Tflush;
                var keys = new HashSet<Zeze.Serialize.ByteBuffer>();
                for (int key = 0; key < TflushInt1Trade.KeyBoundTrade; ++key)
                    keys.Add(table1.EncodeKey(key));
                Database.TransactionAsync t = null;
                try
                {
                    t = table1.Database.BeginTransaction();
                    if (t.ITransaction is DatabaseMemory.MemTrans mt)
                    {
                        var all = mt.Finds(table1.Name, keys);
                        var values = new List<demo.Module1.Value>();
                        foreach (var valueBytes in all.Values)
                        {
                            if (valueBytes != null)
                                values.Add(table1.DecodeValue(valueBytes));
                        }
                        int sum = 0;
                        foreach (var value in values)
                            sum += value.Int1;
                        Debug.Assert(sum == 0);
                    }
                }
                catch (Exception e)
                {
                    Infinite.App.logger.Error(e, "");
                    Debug.Assert(false);
                }
                finally
                {
                    t.Dispose();
                }
                return 0L;
            }
        }
    }
}
