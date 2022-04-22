using Microsoft.VisualStudio.TestTools.UnitTesting;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Transaction;
using Zeze.Game;

namespace UnitTest.Zeze.Game
{
    [TestClass()]
    public class TestBag
    {
        public const int ADD_NUM = 100;          // add item num
        public const int ADD_PILE_NUM = 10;      // 添加的格子数量
        public const int MIN_ITEM_ID = 100;      // item编号起始值
        public const int MAX_GRID_CAPACITY = 99; // 每个格子堆叠上限
        public const int SECOND_REMOVE_NUM = 10; // 第二次删除的item数量 应小于ADD_NUM/2
        public const int MAX_BAG_CAPACITY = 100; // 背包容量

        [TestInitialize]
        public void TestInit()
        {
            demo.App.Instance.Start();
            Bag.FuncItemPileMax(99);
        }

        [TestCleanup]
        public void TestCleanup()
        {
            demo.App.Instance.Stop();
        }

        [TestMethod()]
        public void Test1_Add()
        {
            var result = demo.App.Instance.Zeze.NewProcedure(async () =>
            {
                var bag = demo.App.Instance.BagModule.OpenAsync("test1").Result;
                bag.Capacity = MAX_BAG_CAPACITY;
                for (int i = MIN_ITEM_ID; i < MIN_ITEM_ID + ADD_PILE_NUM; i++)
                {
                    var code = await bag.AddAsync(i, ADD_NUM);
                    Assert.IsTrue(code == 0);
                }
                Assert.IsTrue(bag.Bean.Items.Count == ADD_PILE_NUM * 2);
                return Procedure.Success;
            }, "Test1_Add").CallSynchronously();
            Assert.IsTrue(result == Procedure.Success);
        }

        [TestMethod()]
        public void Test2_Move()
        {
            var result = demo.App.Instance.Zeze.NewProcedure(() =>
            {
                var bag = demo.App.Instance.BagModule.OpenAsync("test1").Result;
                Assert.IsTrue(bag.Bean.Items.Count == ADD_PILE_NUM * 2);
                int moveNum = MAX_GRID_CAPACITY - (ADD_NUM / 2);
                for (int i = 0; i < ADD_PILE_NUM * 2; i += 2)
                {
                    Assert.IsTrue(bag.Bean.Items[i].Number >= moveNum);
                    var code = bag.Move(i, i + 1, moveNum);
                    Assert.IsTrue(code == 0);
                    Assert.IsTrue(bag.Bean.Items[i].Number == ADD_NUM / 2);
                    Assert.IsTrue(bag.Bean.Items[i + 1].Number == ADD_NUM / 2);
                }
                return Task.FromResult(Procedure.Success);
            }, "Test2_Move").CallSynchronously();
            Assert.IsTrue(result == Procedure.Success);
        }

        [TestMethod()]
        public void Test3_Remove()
        { 
            var result = demo.App.Instance.Zeze.NewProcedure(() =>
            {
                var bag = demo.App.Instance.BagModule.OpenAsync("test1").Result;
                Assert.IsTrue(bag.Bean.Items.Count == ADD_PILE_NUM * 2);
                for (int i = MIN_ITEM_ID; i < MIN_ITEM_ID + ADD_PILE_NUM; i++)
                {
                    var code = bag.Remove(i, ADD_NUM / 2);
                    Assert.IsTrue(code == true);
                }

                Assert.IsTrue(bag.Bean.Items.Count == ADD_PILE_NUM);
                for (int i = MIN_ITEM_ID; i < MIN_ITEM_ID + ADD_PILE_NUM; i++)
                {
                    var code = bag.Remove(i, SECOND_REMOVE_NUM);
                    Assert.IsTrue(code == true);
                }

                for (int i = 1; i < ADD_PILE_NUM * 2; i += 2)
                {
                    Assert.IsTrue(bag.Bean.Items[i].Number == ADD_NUM / 2 - SECOND_REMOVE_NUM);
                }
                return Task.FromResult(Procedure.Success);
            }, "Test3_Remove").CallSynchronously();
            Assert.IsTrue(result == Procedure.Success);
        }

        [TestMethod()]
        public void Test4_Move()
        {
            var result = demo.App.Instance.Zeze.NewProcedure(() =>
            {
                var bag = demo.App.Instance.BagModule.OpenAsync("test1").Result;
                Assert.IsTrue(bag.Bean.Items.Count == ADD_PILE_NUM);
                int moveNum = (ADD_NUM / 2 - SECOND_REMOVE_NUM) / 2;
                for (int i = 1; i < ADD_PILE_NUM * 2; i += 2)
                {
                    var code = bag.Move(i, i - 1, moveNum);
                    Assert.IsTrue(code == 0);
                    Assert.IsTrue(bag.Bean.Items[i - 1].Number == moveNum);
                    Assert.IsTrue(bag.Bean.Items[i].Number == ADD_NUM / 2 - SECOND_REMOVE_NUM - moveNum);
                }
                Assert.IsTrue(bag.Bean.Items.Count == ADD_PILE_NUM * 2);
                return Task.FromResult(Procedure.Success);
            }, "Test4_Move").CallSynchronously();
            Assert.IsTrue(result == Procedure.Success);
        }
    }
}