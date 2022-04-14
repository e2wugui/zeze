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

        private async Task Check(int expect)
        {
            Assert.IsTrue(Procedure.Success == await demo.App.Instance.Zz.NewProcedure(
                async () =>
                {
                    var value = await demo.App.Instance.demo_Module1.TableImportant.GetOrAddAsync(1);
                    return value.Int1 == expect ? Procedure.Success : Procedure.LogicError;
                },
                "TestCheckpointModeTable.Check").CallAsync());
        }

        [TestMethod]
        public async Task Test1()
        {
            Assert.IsTrue(Procedure.Success == await demo.App.Instance.Zz.NewProcedure(
                async () =>
                {
                    var value = await demo.App.Instance.demo_Module1.TableImportant.GetOrAddAsync(1);
                    value.Int1 = 0;
                    return Procedure.Success;
                },
                "TestCheckpointModeTable.Init").CallAsync());
            await Check(0);

            int sum = 0;
            {
                Task[] tasks = new Task[1000];
                for (int i = 0; i < tasks.Length; ++i)
                {
                    tasks[i] = demo.App.Instance.Zz.NewProcedure(Add, "TestCheckpointModeTable.Add").CallAsync();
                }
                await Task.WhenAll(tasks);
                sum += tasks.Length;
                await Check(sum);
            }

            {
                Task[] tasks = new Task[1000];
                for (int i = 0; i < tasks.Length; ++i)
                {
                    tasks[i] = demo.App.Instance.Zz.NewProcedure(Add2, "TestCheckpointModeTable.Add2").CallAsync();
                }
                await Task.WhenAll(tasks);
                sum += tasks.Length;
                await Check(sum);
            }
        }

        private async Task<long> Add()
        {
            var value = await demo.App.Instance.demo_Module1.TableImportant.GetOrAddAsync(1);
            value.Int1++;
            return Procedure.Success;
        }

        private async Task<long> Add2()
        {
            var value = await demo.App.Instance.demo_Module1.TableImportant.GetOrAddAsync(1);
            value.Int1++;
            var value2 = await demo.App.Instance.demo_Module1.Table1.GetOrAddAsync(1);
            value2.Int1++;
            return Procedure.Success;
        }
    }
}
