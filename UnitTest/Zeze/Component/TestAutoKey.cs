using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Serialize;
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

		private static long MakeId(long index)
		{
			var bb = ByteBuffer.Allocate(8);
			var serverId = demo.App.Instance.Zeze.Config.ServerId;
			if (serverId > 0)
				bb.WriteInt(serverId);
			bb.WriteLong(index);
			return ByteBuffer.ToLongBE(bb.Bytes, bb.ReadIndex, bb.Size);
		}

		[TestMethod]
		public void Test1_AutoKey()
		{
			Assert.AreEqual(ResultCode.Success, demo.App.Instance.Zeze.NewProcedure(async () =>
			{
				var autoKey = demo.App.Instance.Zeze.GetAutoKey("test1");
				var id = await autoKey.NextIdAsync();
				Assert.AreEqual(MakeId(1), id);
				return ResultCode.Success;
			}, "test1_AutoKey").CallSynchronously());
			Assert.AreEqual(ResultCode.Success, demo.App.Instance.Zeze.NewProcedure(async () =>
			{
				var autoKey = demo.App.Instance.Zeze.GetAutoKey("test1");
				var id = await autoKey.NextIdAsync();
				Assert.AreEqual(MakeId(2), id);
				return ResultCode.Success;
			}, "test1_AutoKey").CallSynchronously());
		}

		[TestMethod]
		public void Test2_AutoKey()
		{
			var allocCount = demo.App.Instance.Zeze.GetAutoKey("test1").GetAllocateCount();
			Assert.AreEqual(ResultCode.Success, demo.App.Instance.Zeze.NewProcedure(async () =>
			{
				var autoKey = demo.App.Instance.Zeze.GetAutoKey("test1");
				var id = await autoKey.NextIdAsync();
				Assert.AreEqual(MakeId(allocCount + 1), id);
				return ResultCode.Success;
			}, "test2_AutoKey").CallSynchronously());
			Assert.AreEqual(ResultCode.Success, demo.App.Instance.Zeze.NewProcedure(async () =>
			{
				var autoKey = demo.App.Instance.Zeze.GetAutoKey("test1");
				var id = await autoKey.NextIdAsync();
				Assert.AreEqual(MakeId(allocCount + 2), id);
				return ResultCode.Success;
			}, "test2_AutoKey").CallSynchronously());
		}

		[TestMethod]
		public void Test3_AutoKey()
		{
			var allocCount = demo.App.Instance.Zeze.GetAutoKey("test1").GetAllocateCount();
			Assert.AreEqual(ResultCode.Success, demo.App.Instance.Zeze.NewProcedure(async () =>
			{
				var autoKey = demo.App.Instance.Zeze.GetAutoKey("test1");
				var id = await autoKey.NextIdAsync();
				Assert.AreEqual(MakeId(allocCount * 2L + 1), id);
				return ResultCode.Success;
			}, "test3_AutoKey").CallSynchronously());
			Assert.AreEqual(ResultCode.Success, demo.App.Instance.Zeze.NewProcedure(async () =>
			{
				var autoKey = demo.App.Instance.Zeze.GetAutoKey("test1");
				var id = await autoKey.NextIdAsync();
				Assert.AreEqual(MakeId(allocCount * 2L + 2), id);
				return ResultCode.Success;
			}, "test3_AutoKey").CallSynchronously());
		}
	}
}
