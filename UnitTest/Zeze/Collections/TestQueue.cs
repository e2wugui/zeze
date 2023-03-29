using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Transaction;
using Zeze.Util;

namespace UnitTest.Zeze.Collections
{
	[TestClass]
	public class TestQueue
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

		[TestMethod]

		public void Test1_QueueAdd()
		{
			Assert.AreEqual(ResultCode.Success, demo.App.Instance.Zeze.NewProcedure(async () =>
			{
				var queueModule = demo.App.Instance.Zeze.Queues;
				var queue = queueModule.Open<demo.Module1.Value>("test1");
				for (int i = 0; i < 10; i++) {
					var v = new demo.Module1.Value();
					v.Int_1 = i;
					await queue.AddAsync(v);
				}
				var bean = await queue.PeekAsync();
				Assert.AreEqual(0, bean.Int_1);
				return ResultCode.Success;
			}, "test1_QueueAdd").CallSynchronously());
		}

		[TestMethod]
		public void Test2_QueueWalk()
		{
			var queueModule = demo.App.Instance.Zeze.Queues;
			var queue = queueModule.Open<demo.Module1.Value>("test1");
			var i = new AtomicInteger();
			int[] arr = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
			queue.WalkAsync((key, value) =>
			{
				Assert.IsTrue(i.Get() < 10);
				Assert.IsTrue(value.Int_1 == arr[i.IncrementAndGet()-1]);
				return true;
			}).Wait();
			Assert.IsTrue(i.Get() == 10);
		}

		[TestMethod]
		public void Test3_QueuePop()
		{
			Assert.AreEqual(ResultCode.Success, demo.App.Instance.Zeze.NewProcedure(async () =>
			{
				var queueModule = demo.App.Instance.Zeze.Queues;
				var queue = queueModule.Open<demo.Module1.Value>("test1");
				for (int i = 0; i < 10; i++)
				{
					var bean = await queue.PopAsync();
					Assert.IsTrue(bean.Int_1 == i);
				}
				Assert.IsTrue(await queue.IsEmptyAsync());
				return ResultCode.Success;
			}, "test2_QueuePop").CallSynchronously());
		}

		[TestMethod]
		public void Test4_QueuePush()
		{
			Assert.AreEqual(ResultCode.Success, demo.App.Instance.Zeze.NewProcedure(async () =>
			{
				var queueModule = demo.App.Instance.Zeze.Queues;
				var queue = queueModule.Open<demo.Module1.Value>("test1");
				for (int i = 0; i < 10; i++)
				{
					var v = new demo.Module1.Value();
					v.Int_1 = i;
					await queue.PushAsync(v);
				}
				var bean = await queue.PeekAsync();
				Assert.IsTrue(bean.Int_1 == 9);
				return ResultCode.Success;
			}, "test3_QueuePush").CallSynchronously());
		}

		[TestMethod]
		public void Test5_QueueWalk()
		{
			var queueModule = demo.App.Instance.Zeze.Queues;
			var queue = queueModule.Open<demo.Module1.Value>("test1");
			var i = new AtomicInteger(0);
			int[] arr = { 9, 8, 7, 6, 5, 4, 3, 2, 1, 0 };
			queue.WalkAsync((key, value) =>
			{
				Assert.IsTrue(i.Get() < 10);
				Assert.IsTrue(value.Int_1 == arr[i.IncrementAndGet()-1]);
				return true;
			}).Wait();
			Assert.IsTrue(i.Get() == 10);
		}

		[TestMethod]
		public void Test6_QueuePop()
		{
			Assert.AreEqual(ResultCode.Success, demo.App.Instance.Zeze.NewProcedure(async () =>
			{
				var queueModule = demo.App.Instance.Zeze.Queues;
				var queue = queueModule.Open<demo.Module1.Value>("test1");
				for (int i = 9; i >= 0; i--)
				{
					var bean = await queue.PopAsync();
					Assert.IsTrue(bean.Int_1 == i);
				}
				Assert.IsTrue(await queue.IsEmptyAsync());
				return ResultCode.Success;
			}, "test4_QueuePop").CallSynchronously());
		}
	}
}
