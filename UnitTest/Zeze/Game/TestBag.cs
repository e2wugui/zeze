using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Game;
using Zeze.Transaction;
using Zeze.Util;

namespace UnitTest.Zeze.Game
{
    [TestClass()]
    public class TestBag
    {
        [TestInitialize]
        public void TestInit()
        {
            demo.App.Instance.Start();
            Bag.FuncItemPileMax = (itemId) => 99;
        }

        [TestCleanup]
        public void TestCleanup()
        {
            demo.App.Instance.Stop();
        }

        [TestMethod()]
        public void Test1_All()
        {
            var result = demo.App.Instance.Zeze.NewProcedure(async () =>
            {
                var bag = await demo.App.Instance.BagModule.OpenAsync("test1");
                bag.Capacity = 100;
                for (int i = 100; i < 100 + 10; i++)
                {
                    var code = await bag.AddAsync(i, 100);
                    Assert.IsTrue(code == 0);
                }
                Assert.IsTrue(bag.Bean.Items.Count == 10 * 2);

                Assert.IsTrue(bag.Bean.Items.Count == 20);

                for (int i = 0; i < 20; i += 2)
                {
                    Assert.IsTrue(bag.Bean.Items[i].Number >= 49);
                    var code = bag.Move(i, i + 1, 49);
                    Assert.IsTrue(code == 0);
                    Assert.IsTrue(bag.Bean.Items[i].Number == 50);
                    Assert.IsTrue(bag.Bean.Items[i + 1].Number == 50);
                }

                Assert.IsTrue(bag.Bean.Items.Count == 20);
                for (int i = 100; i < 100 + 10; i++)
                {
                    var code = bag.Remove(i, 50);
                    Assert.IsTrue(code == true);
                }

                Assert.IsTrue(bag.Bean.Items.Count == 10);
                for (int i = 100; i < 110; i++)
                {
                    var code = bag.Remove(i, 10);
                    Assert.IsTrue(code == true);
                }

                for (int i = 1; i < 20; i += 2)
                {
                    Assert.IsTrue(bag.Bean.Items[i].Number == 40);
                }

                Assert.IsTrue(bag.Bean.Items.Count == 10);
                for (int i = 1; i < 20; i += 2)
                {
                    var code = bag.Move(i, i - 1, 20);
                    Assert.IsTrue(code == 0);
                    Assert.IsTrue(bag.Bean.Items[i - 1].Number == 20);
                    Assert.IsTrue(bag.Bean.Items[i].Number == 20);
                }
                Assert.IsTrue(bag.Bean.Items.Count == 20);

                // test move swap
                var fromId = bag.Bean.Items[0].Id;
                var fromNum = bag.Bean.Items[0].Number;
                var toId = bag.Bean.Items[2].Id;
                var toNum = bag.Bean.Items[2].Number;
                var rst = bag.Move(0, 2, -1);
                Assert.IsTrue(bag.Bean.Items[0].Id == toId && bag.Bean.Items[0].Number == toNum);
                Assert.IsTrue(bag.Bean.Items[2].Id == fromId && bag.Bean.Items[2].Number == fromNum);
                return ResultCode.Success;
            }, "Test1_Add").CallSynchronously();
            Assert.IsTrue(result == ResultCode.Success);
        }
    }
}