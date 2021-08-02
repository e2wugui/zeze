using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Transaction;

namespace UnitTest.Zeze.Trans
{
    [TestClass]
    public class TestCheckpointModeTable
    {
        [TestInitialize]
        public void TestInit()
        {
            demo.App.Instance.Start();
        }

        [TestCleanup]
        public void TestCleanup()
        {
            demo.App.Instance.Stop();
        }

        private void Check(int expect)
        {
            Assert.IsTrue(Procedure.Success == demo.App.Instance.Zeze.NewProcedure(
                () =>
                {
                    var value = demo.App.Instance.demo_Module1.TableImportant.GetOrAdd(1);
                    return value.Int1 == expect ? Procedure.Success : Procedure.LogicError;
                },
                "TestCheckpointModeTable.Check").Call());
        }

        [TestMethod]
        public void Test1()
        {
            Assert.IsTrue(Procedure.Success == demo.App.Instance.Zeze.NewProcedure(
                () =>
                {
                    var value = demo.App.Instance.demo_Module1.TableImportant.GetOrAdd(1);
                    value.Int1 = 0;
                    return Procedure.Success;
                },
                "TestCheckpointModeTable.Init").Call());
            Check(0);

            int sum = 0;
            {
                Task[] tasks = new Task[1000];
                for (int i = 0; i < tasks.Length; ++i)
                {
                    tasks[i] = global::Zeze.Util.Task.Run(
                        demo.App.Instance.Zeze.NewProcedure(Add, "TestCheckpointModeTable.Add"));
                }
                Task.WaitAll(tasks);
                sum += tasks.Length;
                Check(sum);
            }

            {
                Task[] tasks = new Task[1000];
                for (int i = 0; i < tasks.Length; ++i)
                {
                    tasks[i] = global::Zeze.Util.Task.Run(
                        demo.App.Instance.Zeze.NewProcedure(Add2, "TestCheckpointModeTable.Add2"));
                }
                Task.WaitAll(tasks);
                sum += tasks.Length;
                Check(sum);
            }
        }

        private int Add()
        {
            var value = demo.App.Instance.demo_Module1.TableImportant.GetOrAdd(1);
            value.Int1++;
            return Procedure.Success;
        }

        private int Add2()
        {
            var value = demo.App.Instance.demo_Module1.TableImportant.GetOrAdd(1);
            value.Int1++;
            var value2 = demo.App.Instance.demo_Module1.Table1.GetOrAdd(1);
            value2.Int1++;
            return Procedure.Success;
        }
    }
}
