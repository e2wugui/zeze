using System;
using System.Collections.Generic;
using System.Text;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using System.Threading.Tasks;
using Zeze.Transaction;
using System.Threading;
using Zeze.Util;

namespace UnitTest.Zeze.Trans
{
    [TestClass]
    public class TestGlobal
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();
        public class PrintLog : Log<demo.Module1.Value>
        {
            static volatile int lastInt = -1;
            int oldInt;
            int appId;
            bool eq = false;
            public PrintLog(Bean bean, demo.Module1.Value value, int appId)
            {
                Belong = bean;
                Value = value;
                VariableId = 100;
 
                int last = lastInt;
                oldInt = Value.Int_1;
                eq = lastInt == oldInt;
                this.appId = appId;
            }

            public override void Commit()
            {
                if (eq)
                {
                    logger.Fatal("xxxeq " + oldInt + " " + appId);
                }
                else
                {
                    //logger.Debug("xxx " + oldInt + " " + appId);
                }

                lastInt = oldInt;
            }
        }

        //[TestMethod]
        public void Test2AppSameLocalId()
        {
            demo.App app1 = demo.App.Instance;
            demo.App app2 = new demo.App();
            var config1 = global::Zeze.Config.Load();
            var config2 = global::Zeze.Config.Load();
            try
            {
                app1.Start(config1);
                app2.Start(config2);
            }
            finally
            {
                app1.Stop();
                app2.Stop();
            }
        }

        [TestMethod]
        public void Test2App()
        {
            demo.App app1 = demo.App.Instance;
            demo.App app2 = new demo.App();
            var config1 = global::Zeze.Config.Load();
            var config2 = global::Zeze.Config.Load();
            config2.ServerId = config1.ServerId + 1;

            app1.Start(config1);
            app2.Start(config2);
            try
            {
                // 只删除一个app里面的记录就够了。
                Assert.AreEqual(ResultCode.Success, app1.Zeze.NewProcedure(async () =>
                {
                    await app1.demo_Module1.Table1.RemoveAsync(6785);
                    return ResultCode.Success;
                }, "RemoveClean").CallSynchronously());
                
                Task[] task2 = new Task[2];
                int count = 2000;
                task2[0] = Task.Run(() => ConcurrentAdd(app1, count, 1));
                task2[1] = Task.Run(() => ConcurrentAdd(app2, count, 2));
                Task.WaitAll(task2);
                int countall = count * 2;

                var result1 = app1.Zeze.NewProcedure(async () =>
                {
                    int last1 = (await app1.demo_Module1.Table1.GetAsync(6785)).Int_1;
                    Assert.AreEqual(countall, last1);
                    //Console.WriteLine("app1 " + last1);
                    return ResultCode.Success;
                }, "CheckResult1").CallSynchronously();
                logger.Warn("result1=" + result1);
                Assert.IsTrue(ResultCode.Success == result1);

                var result2 = app2.Zeze.NewProcedure(async () =>
                {
                    var value = await app2.demo_Module1.Table1.GetAsync(6785);
                    int last2 = value.Int_1;
                    Assert.AreEqual(countall, last2);
                    //Console.WriteLine("app1 " + last2);
                    return ResultCode.Success;
                }, "CheckResult2").CallSynchronously();
                logger.Warn("result2=" + result2);
                Assert.IsTrue(ResultCode.Success == result2);
            }
            finally
            {
                app1.Stop();
                app2.Stop();
            }
        }

        void ConcurrentAdd(demo.App app, int count, int appId)
        {
            Task[] tasks = new Task[count];
            for (int i = 0; i < tasks.Length; ++i)
            {
                tasks[i] = app.Zeze.NewProcedure(async ()=>
                {
                    demo.Module1.Value b = await app.demo_Module1.Table1.GetOrAddAsync(6785);
                    b.Int_1 += 1;
                    PrintLog log = new PrintLog(b, b, appId);
                    Transaction.Current.PutLog(log);
                    return ResultCode.Success;
                }, "ConcurrentAdd" + appId).CallAsync();
            }
            Task.WaitAll(tasks);
        }
    }
}
