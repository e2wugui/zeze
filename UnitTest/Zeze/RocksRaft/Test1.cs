using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Raft.RocksRaft;
using Zeze.Serialize;
using Zeze.Util;
using static Zeze.Raft.RocksRaft.Test;

namespace UnitTest.Zeze.RocksRaft
{
    [TestClass]
    public class Test1
    {
		private static void Remove1(Rocks rocks)
		{
			rocks.NewProcedure(async () =>
			{
				var table = rocks.GetTableTemplate("tRocksRaft").OpenTable<int, Bean1>();
				await table.RemoveAsync(1);

				Transaction.Current.RunWhileCommit(() =>
				{
					var c = Transaction.Current.Changes;

					Assert.IsTrue(c.Beans.Count == 1);

					Assert.IsTrue(c.Records.Count == 1);
					Assert.IsTrue(c.Records.TryGetValue(new TableKey(table.Name, 1), out var r));
					Assert.IsNull(r.PutValue);
					Assert.AreEqual(Changes.Record.Remove, r.State);
					Assert.IsTrue(r.LogBeans.Count == 1);
					Assert.IsTrue(r.LogBean.Count == 0);
				});
				return 0;
			}).CallSynchronously();
		}

		private static async Task Update(Table<int, Bean1> table, int num)
		{
			var value = await table.GetOrAddAsync(1);

			// 本层Bean变量修改日志
			value.I = 1 + num;

			// 下一层Bean变量修改日志
			value.Bean2.I = 2 + num;

			// 本层Bean容器变量修改日志
			value.Map1.Put(3 + num, 3 + num);

			// 本层Bean容器变量修改日志2
			var bean1 = new Bean1();
			value.Map2.Put(4 + num, bean1);

			// 容器内Bean修改日志。
			bean1.I = 5 + num;
		}

		private static void VerifyChanges(string except)
		{
			Transaction.Current.RunWhileCommit(() =>
			{
				var Changes = Transaction.Current.Changes;
				var sb = new StringBuilder();
				ByteBuffer.BuildString(sb, Changes.Records);
				if (string.IsNullOrEmpty(except))
				{
					Console.WriteLine(sb.ToString());
				}
				else
				{
					except = except.Replace("\r\n", "\n");
					Assert.AreEqual(except, sb.ToString());
				}
			});
		}

		private static void VerifyData(Rocks rocks, string except)
		{
			rocks.NewProcedure(async () =>
			{
				var table = rocks.GetTableTemplate("tRocksRaft").OpenTable<int, Bean1>(0);
				var value = await table.GetOrAddAsync(1);
				var current = value.ToString();
				if (string.IsNullOrEmpty(except))
				{
					Console.WriteLine(current);
				}
				else
				{
					Assert.AreEqual(except, current);
				}
				return 0;
			}).CallSynchronously();
		}

		private static void PutAndEdit(Rocks rocks)
		{
			rocks.NewProcedure(async () =>
			{
				var table = rocks.GetTableTemplate("tRocksRaft").OpenTable<int, Bean1>(0);
				await Update(table, 0);
                VerifyChanges(@"{(tRocksRaft#0,1):State=1 PutValue=Bean1(I=1 L=0 Map1={3:3} Bean2=Bean2(I=2) Map2={4:Bean1(I=5 L=0 Map1={} Bean2=Bean2(I=0) Map2={})})
Log=[]
AllLog=[{0:Value=Bean1(I=1 L=0 Map1={3:3} Bean2=Bean2(I=2) Map2={4:Bean1(I=5 L=0 Map1={} Bean2=Bean2(I=0) Map2={})})},{1:Value=1,3: Putted:{3:3} Removed:[],4:{1:Value=2},5: Putted:{4:Bean1(I=5 L=0 Map1={} Bean2=Bean2(I=0) Map2={})} Removed:[] Changed:[{1:Value=5}]}]}");
				return 0;
			}).CallSynchronously();
		}

		private static void Edit(Rocks rocks)
		{
			rocks.NewProcedure(async () =>
			{
				var table = rocks.GetTableTemplate("tRocksRaft").OpenTable<int, Bean1>(0);
				await Update(table, 10);
                VerifyChanges(@"{(tRocksRaft#0,1):State=2 PutValue=
Log=[{1:Value=11,3: Putted:{13:13} Removed:[],4:{1:Value=12},5: Putted:{14:Bean1(I=15 L=0 Map1={} Bean2=Bean2(I=0) Map2={})} Removed:[] Changed:[{1:Value=15}]}]
AllLog=[{1:Value=11,3: Putted:{13:13} Removed:[],4:{1:Value=12},5: Putted:{14:Bean1(I=15 L=0 Map1={} Bean2=Bean2(I=0) Map2={})} Removed:[] Changed:[{1:Value=15}]}]}");
				return 0;
			}).CallSynchronously();
		}

		private static void EditAndPut(Rocks rocks)
		{
			rocks.NewProcedure(async () =>
			{
				var table = rocks.GetTableTemplate("tRocksRaft").OpenTable<int, Bean1>(0);
				await Update(table, 20);
				// 重新put，将会让上面的修改树作废。但所有的日志树都可以从All中看到。
				var bean1put = new Bean1();
				await table.PutAsync(1, bean1put);
                VerifyChanges(@"{(tRocksRaft#0,1):State=1 PutValue=Bean1(I=0 L=0 Map1={} Bean2=Bean2(I=0) Map2={})
Log=[]
AllLog=[{0:Value=Bean1(I=0 L=0 Map1={} Bean2=Bean2(I=0) Map2={})},{1:Value=21,3: Putted:{23:23} Removed:[],4:{1:Value=22},5: Putted:{24:Bean1(I=25 L=0 Map1={} Bean2=Bean2(I=0) Map2={})} Removed:[] Changed:[{1:Value=25}]}]}");
				return 0;
			}).CallSynchronously();
		}

		private static void EditInContainer(Rocks rocks)
        {
			rocks.NewProcedure(async () =>
			{
				var table = rocks.GetTableTemplate("tRocksRaft").OpenTable<int, Bean1>(0);
				var value = await table.GetOrAddAsync(1);
				var edit = value.Map2.Get(14);
				edit.Bean2.I = 2222;
                VerifyChanges(@"{(tRocksRaft#0,1):State=2 PutValue=
Log=[{5: Putted:{} Removed:[] Changed:[{4:{1:Value=2222}}]}]
AllLog=[{5: Putted:{} Removed:[] Changed:[{4:{1:Value=2222}}]}]}");
				return 0;
			}).CallSynchronously();
		}

		private static void NestProcedure(Rocks rocks)
        {
			rocks.NewProcedure(async () =>
			{
				var table = rocks.GetTableTemplate("tRocksRaft").OpenTable<int, Bean1>(0);
				var value = await table.GetAsync(1);
				value.Bean2.I = 3333;

				await rocks.NewProcedure(async () =>
				{
					var table = rocks.GetTableTemplate("tRocksRaft").OpenTable<int, Bean1>(0);
					var value = await table.GetAsync(1);
					value.Bean2.I = 4444;
					Assert.AreEqual(4444, value.Bean2.I);
					return -1;
				}).CallAsync();

                VerifyChanges(@"{(tRocksRaft#0,1):State=2 PutValue=
Log=[{4:{1:Value=3333}}]
AllLog=[{4:{1:Value=3333}}]}");
				return 0;
			}).CallSynchronously();
        }

		private static void NestProcedureContainer(Rocks rocks)
        {
			rocks.NewProcedure(async() =>
			{
				await rocks.NewProcedure(async () =>
				{
					var table = rocks.GetTableTemplate("tRocksRaft").OpenTable<int, Bean1>(0);
					var value = await table.GetAsync(1);
					value.Map2.Put(4444, new Bean1());
					value.Map1.Put(4444, 4444);
					value.Map1.Remove(3);
					value.Map2.Remove(4);
					return 0;
				}).CallAsync();

                VerifyChanges(@"{(tRocksRaft#0,1):State=2 PutValue=
Log=[{3: Putted:{4444:4444} Removed:[3],5: Putted:{4444:Bean1(I=0 L=0 Map1={} Bean2=Bean2(I=0) Map2={})} Removed:[4] Changed:[]}]
AllLog=[{3: Putted:{4444:4444} Removed:[3],5: Putted:{4444:Bean1(I=0 L=0 Map1={} Bean2=Bean2(I=0) Map2={})} Removed:[4] Changed:[]}]}");
				return 0;
			}).CallSynchronously();
        }

		private static Rocks GetLeader(List<Rocks> rocks, Rocks skip)
        {
			while (true)
            {
				foreach (var rock in rocks)
				{
					if (rock == skip)
						continue;
					if (rock.IsLeader)
						return rock;
				}
				System.Threading.Thread.Sleep(1000);
			}
		}

		[TestMethod]
        public void Test_1()
        {
			FileSystem.DeleteDirectory("127.0.0.1_6000");
			FileSystem.DeleteDirectory("127.0.0.1_6001");
			FileSystem.DeleteDirectory("127.0.0.1_6002");

			using var rocks1 = new Rocks("127.0.0.1:6000");
			using var rocks2 = new Rocks("127.0.0.1:6001");
			using var rocks3 = new Rocks("127.0.0.1:6002");

			var rockslist = new List<Rocks> { rocks1, rocks2, rocks3 };
			foreach (var rr in rockslist)
            {
                Rocks.RegisterLog<LogMap1<int, int>>();
                Rocks.RegisterLog<LogMap2<int, Bean1>>();
				rr.RegisterTableTemplate<int, Bean1>("tRocksRaft");
			}

			// start
			rocks1.Raft.Server.Start();
			rocks2.Raft.Server.Start();
			rocks3.Raft.Server.Start();

			// leader
			var leader = GetLeader(rockslist, null);
            RunLeader(leader);
			leader.Raft.Server.Stop();

			// 只简单验证一下最新的数据。
			var newleader = GetLeader(rockslist, leader);
            VerifyData(newleader, "Bean1(I=0 L=0 Map1={} Bean2=Bean2(I=0) Map2={})");
		}

		private static void RunLeader(Rocks rocks)
        {
            Remove1(rocks);

            PutAndEdit(rocks);
            VerifyData(rocks, "Bean1(I=1 L=0 Map1={3:3} Bean2=Bean2(I=2) Map2={4:Bean1(I=5 L=0 Map1={} Bean2=Bean2(I=0) Map2={})})");

            Edit(rocks);
            VerifyData(rocks, "Bean1(I=11 L=0 Map1={3:3,13:13} Bean2=Bean2(I=12) Map2={4:Bean1(I=5 L=0 Map1={} Bean2=Bean2(I=0) Map2={}),14:Bean1(I=15 L=0 Map1={} Bean2=Bean2(I=0) Map2={})})");

            EditInContainer(rocks);
            VerifyData(rocks, "Bean1(I=11 L=0 Map1={3:3,13:13} Bean2=Bean2(I=12) Map2={4:Bean1(I=5 L=0 Map1={} Bean2=Bean2(I=0) Map2={}),14:Bean1(I=15 L=0 Map1={} Bean2=Bean2(I=2222) Map2={})})");

            NestProcedure(rocks);
            VerifyData(rocks, "Bean1(I=11 L=0 Map1={3:3,13:13} Bean2=Bean2(I=3333) Map2={4:Bean1(I=5 L=0 Map1={} Bean2=Bean2(I=0) Map2={}),14:Bean1(I=15 L=0 Map1={} Bean2=Bean2(I=2222) Map2={})})");

            NestProcedureContainer(rocks);
            VerifyData(rocks, "Bean1(I=11 L=0 Map1={13:13,4444:4444} Bean2=Bean2(I=3333) Map2={14:Bean1(I=15 L=0 Map1={} Bean2=Bean2(I=2222) Map2={}),4444:Bean1(I=0 L=0 Map1={} Bean2=Bean2(I=0) Map2={})})");

            EditAndPut(rocks);
            VerifyData(rocks, "Bean1(I=0 L=0 Map1={} Bean2=Bean2(I=0) Map2={})");

			// 再次运行本测试，才会执行到 LoadSnapshot。
			rocks.Raft.LogSequence.Snapshot(true).Wait();
		}
	}
}
