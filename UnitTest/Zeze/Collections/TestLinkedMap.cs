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
	public class TestLinkedMap
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
		public void Test1_LinkedMapPut()
		{
			Assert.IsTrue(ResultCode.Success == demo.App.Instance.Zeze.NewProcedure(async () =>
			{
				var map = demo.App.Instance.LinkedMapModule.Open<demo.Module1.Value>("test1");
				for (int i = 100; i < 110; i++)
				{
                    var bean = new demo.Module1.Value
                    {
                        Int_1 = i
                    };
                    await map.PutAsync(i, bean);
				}
				return ResultCode.Success;
			}, "test1_LinkedMapPut").CallSynchronously());
		}

		[TestMethod]
		public void Test2_LinkedMapGet()
		{
			Assert.IsTrue(ResultCode.Success == demo.App.Instance.Zeze.NewProcedure(async () =>
			{
				var map = demo.App.Instance.LinkedMapModule.Open<demo.Module1.Value>("test1");
				for (int i = 100; i < 110; i++)
				{
					var bean = await map.GetAsync(i);
					Assert.IsTrue(bean.Int_1 == i);
				}
				return ResultCode.Success;
			}, "test2_LinkedMapGet").CallSynchronously());
		}

		[TestMethod]
		public void Test3_LinkedMapWalk()
		{
			var map = demo.App.Instance.LinkedMapModule.Open<demo.Module1.Value>("test1");
			var i = new AtomicInteger(0);
			int[] arr = { 100, 101, 102, 103, 104, 105, 106, 107, 108, 109 };
			Array.Reverse(arr);

			map.WalkAsync((key, value) =>
			{
				Assert.IsTrue(i.Get() < 10);
				Assert.IsTrue(value.Int_1 == arr[i.IncrementAndGet() - 1]);
				return true;
			}).Wait();
			Assert.IsTrue(i.Get() == 10);
		}

		[TestMethod]
		public void Test4_LinkedMapRemove()
		{
			Assert.IsTrue(ResultCode.Success == demo.App.Instance.Zeze.NewProcedure(async () =>
			{
				var map = demo.App.Instance.LinkedMapModule.Open<demo.Module1.Value>("test1");
				for (int i = 100; i < 110; i++)
				{
					var bean = await map.RemoveAsync(i);
					Assert.IsTrue(bean.Int_1 == i);
				}
				Assert.IsTrue(await map.IsEmptyAsync());
				return ResultCode.Success;
			}, "test2_LinkedMapRemove").CallSynchronously());
		}
	}
}
