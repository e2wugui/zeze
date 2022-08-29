using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Transaction;
using Zeze.Util;

namespace UnitTest.Zeze.Component
{
	[TestClass]
	public class TestAutoKey
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
		public void Test1_AutoKey()
		{
			Assert.AreEqual(ResultCode.Success, demo.App.Instance.Zeze.NewProcedure(async () =>
			{
				var autoKey = demo.App.Instance.Zeze.GetAutoKey("test1");
				var id = await autoKey.NextIdAsync();
				Assert.AreEqual(1, id);
				return ResultCode.Success;
			}, "test1_AutoKey").CallSynchronously());
			Assert.AreEqual(ResultCode.Success, demo.App.Instance.Zeze.NewProcedure(async () =>
			{
				var autoKey = demo.App.Instance.Zeze.GetAutoKey("test1");
				var id = await autoKey.NextIdAsync();
				Assert.AreEqual(2, id);
				return ResultCode.Success;
			}, "test1_AutoKey").CallSynchronously());
		}

		[TestMethod]
		public void Test2_AutoKey()
		{
			Assert.AreEqual(ResultCode.Success, demo.App.Instance.Zeze.NewProcedure(async () =>
			{
				var autoKey = demo.App.Instance.Zeze.GetAutoKey("test1");
				var id = await autoKey.NextIdAsync();
				Assert.AreEqual(1001, id);
				return ResultCode.Success;
			}, "test2_AutoKey").CallSynchronously());
			Assert.AreEqual(ResultCode.Success, demo.App.Instance.Zeze.NewProcedure(async () =>
			{
				var autoKey = demo.App.Instance.Zeze.GetAutoKey("test1");
				var id = await autoKey.NextIdAsync();
				Assert.AreEqual(1002, id);
				return ResultCode.Success;
			}, "test2_AutoKey").CallSynchronously());
		}

		[TestMethod]
		public void Test3_AutoKey()
		{
			Assert.AreEqual(ResultCode.Success, demo.App.Instance.Zeze.NewProcedure(async () =>
			{
				var autoKey = demo.App.Instance.Zeze.GetAutoKey("test1");
				var id = await autoKey.NextIdAsync();
				Assert.AreEqual(2001, id);
				return ResultCode.Success;
			}, "test3_AutoKey").CallSynchronously());
			Assert.AreEqual(ResultCode.Success, demo.App.Instance.Zeze.NewProcedure(async () =>
			{
				var autoKey = demo.App.Instance.Zeze.GetAutoKey("test1");
				var id = await autoKey.NextIdAsync();
				Assert.AreEqual(2002, id);
				return ResultCode.Success;
			}, "test3_AutoKey").CallSynchronously());
		}
	}
}
