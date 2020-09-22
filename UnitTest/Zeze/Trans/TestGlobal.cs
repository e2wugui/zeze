using System;
using System.Collections.Generic;
using System.Text;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using System.Threading.Tasks;
using Zeze.Transaction;
using System.Threading;

namespace UnitTest.Zeze.Trans
{
    [TestClass]
    public class TestGlobal
    {
        public class PrintLog : Log<demo.Module1.Value, demo.Module1.Value>
        {
            private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

            static volatile int lastInt = -1;
            int oldInt;
            int appId;
            bool eq = false;
            public PrintLog(Bean bean, demo.Module1.Value value, int appId) : base(bean, value)
            {
                int last = lastInt;
                oldInt = Value.Int1;
                eq = lastInt == oldInt;
                this.appId = appId;
            }

            public override long LogKey => this.Bean.ObjectId + 100;

            public override void Commit()
            {
                if (eq)
                {
                    logger.Debug("xxxeq " + oldInt + " " + appId);
                }
                else
                {
                    logger.Debug("xxx " + oldInt + " " + appId);
                }

                lastInt = oldInt;
            }
        }

        [TestMethod]
        public void Test2App()
        {
            demo.App app1 = demo.App.Instance;
            demo.App app2 = new demo.App();

            app1.Start();
            app2.Start();
            try
            {
                // 只删除一个app里面的记录就够了。
                Assert.IsTrue(Procedure.Success == app1.Zeze.NewProcedure(() =>
                {
                    app1.demo_Module1_Module1.Table1.Remove(6785);
                    return Procedure.Success;
                }).Call());
                
                Task[] task2 = new Task[2];
                int count = 2000;
                task2[0] = Task.Run(() => ConcurrentAdd(app1, count, 1));
                task2[1] = Task.Run(() => ConcurrentAdd(app2, count, 2));
                Task.WaitAll(task2);
                int countall = count * 2;
                Assert.IsTrue(Procedure.Success == app1.Zeze.NewProcedure(() =>
                {
                    int last1 = app1.demo_Module1_Module1.Table1.Get(6785).Int1;
                    //Assert.AreEqual(countall, last1);
                    Console.WriteLine("app1 " + last1);
                    return Procedure.Success;
                }).Call());
                Assert.IsTrue(Procedure.Success == app2.Zeze.NewProcedure(() =>
                {
                    int last2 = app2.demo_Module1_Module1.Table1.Get(6785).Int1;
                    //Assert.AreEqual(countall, last2);
                    Console.WriteLine("app1 " + last2);
                    return Procedure.Success;
                }).Call());
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
                tasks[i] = Task.Run(app.Zeze.NewProcedure(()=>
                {
                    demo.Module1.Value b = app.demo_Module1_Module1.Table1.GetOrAdd(6785);
                    b.Int1 += 1;
                    PrintLog log = new PrintLog(b, b, appId);
                    Transaction.Current.PutLog(log); return Procedure.Success;
                }).Call);
            }
            Task.WaitAll(tasks);
        }
    }
}
