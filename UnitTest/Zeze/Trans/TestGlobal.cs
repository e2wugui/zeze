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
                task2[0] = Task.Run(() => ConcurrentAdd(app1));
                task2[1] = Task.Run(() => ConcurrentAdd(app2));
                Task.WaitAll(task2);
                Assert.IsTrue(Procedure.Success == app2.Zeze.NewProcedure(() =>
                {
                    int last2 = app2.demo_Module1_Module1.Table1.Get(6785).Int1;
                    Console.WriteLine("Assert Failed. app2 " + last2);
                    return Procedure.Success;
                }).Call());
                Assert.IsTrue(Procedure.Success == app1.Zeze.NewProcedure(() =>
                {
                    int last1 = app1.demo_Module1_Module1.Table1.Get(6785).Int1;
                    Console.WriteLine("Assert Failed. app1 " + last1);
                    return Procedure.Success;
                }).Call());
                //Thread.Sleep(100000);
            }
            finally
            {
                app1.Stop();
                app2.Stop();
            }
        }

        void ConcurrentAdd(demo.App app)
        {
            Task[] tasks = new Task[2000];
            for (int i = 0; i < tasks.Length; ++i)
            {
                tasks[i] = Task.Run(app.Zeze.NewProcedure(()=>
                {
                    app.demo_Module1_Module1.Table1.GetOrAdd(6785).Int1 += 1;
                    return Procedure.Success;
                }).Call);
            }
            Task.WaitAll(tasks);
        }
    }
}
