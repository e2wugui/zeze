using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Raft.RocksRaft;
using Zeze.Serialize;

namespace UnitTest.Zeze.RocksRaft
{
    [TestClass]
    public class Test1
    {
        public sealed class Bean1 : Bean
        {
			private int _i;
			private long _l;
			private CollMap1<int, int> _map1;
			private Bean2 _bean2;
			private CollMap2<int, Bean1> _map2;

			public int I
			{
				get
				{
					if (IsManaged)
                    {
						if (false == Transaction.Current.TryGetLog(ObjectId + 1, out var log)) return _i;
						return ((Log<int>)log).Value;
					}
                    else
                    {
						return _i;
					}
				}

				set
				{
					if (IsManaged)
					{
						Transaction.Current.PutLog(new Log<int>() { Bean = this, VariableId = 1, Value = value, });
					}
					else
					{
						_i = value;
					}
				}
			}

			public long L
			{
				get
				{
					if (IsManaged)
                    {
						if (false == Transaction.Current.TryGetLog(ObjectId + 2, out var log)) return _l;
						return ((Log<long>)log).Value;
					}
					else
                    {
						return _l;
                    }
				}

				set
				{
					if (IsManaged)
                    {
						Transaction.Current.PutLog(new Log<long>() { Bean = this, VariableId = 2, Value = value, });
					}
					else
                    {
						_l = value;
                    }
				}
			}

			public CollMap1<int, int> Map1 => _map1;
			public CollMap2<int, Bean1> Map2 => _map2;

			public Bean2 Bean2 => _bean2;

            public override void FollowerApply(Log log)
            {
				var blog = (LogBean)log;
				foreach (var vlog in blog.Variables.Values)
				{
					switch (vlog.VariableId)
					{
						case 1: _i = ((Log<int>)vlog).Value; break;
						case 2: _l = ((Log<long>)vlog).Value; break;
						case 3: _map1.FollowerApply(vlog); break;
						case 4: _bean2.FollowerApply(vlog); break;
						case 5: _map2.FollowerApply(vlog); break;
					}
				}
			}

            public override void LeaderApplyNoRecursive(Log vlog)
            {
				switch (vlog.VariableId)
				{
					case 1: _i = ((Log<int>)vlog).Value; break;
					case 2: _l = ((Log<long>)vlog).Value; break;
				}
			}

			public Bean1()
            {
				_map1 = new CollMap1<int, int>() { VariableId = 3 };
				_bean2 = new Bean2() { VariableId = 4 };
				_map2 = new CollMap2<int, Bean1>() { VariableId = 5 };
			}

			public override void Decode(ByteBuffer bb)
			{
				I = bb.ReadInt();
				L = bb.ReadLong();
				Map1.Decode(bb);
				Bean2.Decode(bb);
			}

			public override void Encode(ByteBuffer bb)
			{
				bb.WriteInt(I);
				bb.WriteLong(L);
				Map1.Encode(bb);
				Bean2.Encode(bb);
			}

			protected override void InitChildrenRootInfo(Record.RootInfo root)
			{
				_map1.InitRootInfo(root, this);
				_bean2.InitRootInfo(root, this);
				_map2.InitRootInfo(root, this);
			}

			public override string ToString()
			{
				return $"Bean1(I={I} L={L} Map1={Map1} Bean2={Bean2} Map2={Map2})";
			}
		}

		public sealed class Bean2 : Bean
        {
			private int _i;

			public int I
			{
				get
				{
					if (false == Transaction.Current.TryGetLog(ObjectId + 1, out var log)) return _i;
					return ((Log<int>)log).Value;
				}

				set
				{
					Transaction.Current.PutLog(new Log<int>() { Bean = this, VariableId = 1, Value = value, });
				}
			}

			public override void Decode(ByteBuffer bb)
			{
				I = bb.ReadInt();
			}

			public override void Encode(ByteBuffer bb)
			{
				bb.WriteInt(I);
			}

			protected override void InitChildrenRootInfo(Record.RootInfo root)
			{
			}

			public override string ToString()
			{
				return $"Bean2(I={I})";
			}

            public override void FollowerApply(Log log)
            {
				var blog = (LogBean)log;
				foreach (var vlog in blog.Variables.Values)
				{
					switch (vlog.VariableId)
					{
						case 1: _i = ((Log<int>)vlog).Value; break;
					}
				}
			}

            public override void LeaderApplyNoRecursive(Log vlog)
            {
				switch (vlog.VariableId)
				{
					case 1: _i = ((Log<int>)vlog).Value; break;
				}
			}
		}

		private void Remove1(Rocks rocks)
		{
			rocks.NewProcedure(() =>
			{
				var table = rocks.OpenTable<int, Bean1>("tRocksRaft");
				table.Remove(1);

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
			}).Call();
		}

		private void Update(Table<int, Bean1> table)
		{
			var value = table.GetOrAdd(1);

			// 本层Bean变量修改日志
			value.I = 1;

			// 下一层Bean变量修改日志
			value.Bean2.I = 2;

			// 本层Bean容器变量修改日志
			value.Map1.Put(3, 3);

			// 本层Bean容器变量修改日志2
			var bean1 = new Bean1();
			value.Map2.Put(4, bean1);

			// 容器内Bean修改日志。
			bean1.I = 5;
		}

		private void VerifyChanges(string except)
		{
			Transaction.Current.RunWhileCommit(() =>
			{
				var Changes = Transaction.Current.Changes;
				var sb = new StringBuilder();
				ByteBuffer.BuildString(sb, Changes.Records);
				Console.WriteLine(sb.ToString());
				except = except.Replace("\r\n", "\n");
				//Assert.AreEqual(except, sb.ToString());
			});
		}

		private void VerifyData(Rocks rocks, string except)
		{
			rocks.NewProcedure(() =>
			{
				var table = rocks.OpenTable<int, Bean1>("tRocksRaft");
				var value = table.GetOrAdd(1);
				var current = value.ToString();
				Console.WriteLine(current);
				//Assert.AreEqual(except, current);
				return 0;
			}).Call();
		}

		private void Update1(Rocks rocks)
		{
			rocks.NewProcedure(() =>
			{
				var table = rocks.OpenTable<int, Bean1>("tRocksRaft");
				Update(table);
				VerifyChanges(@"{(tRocksRaft,1):State=1 PutValue=Bean1(I=1 L=0 Map1={3:3} Bean2=Bean2(I=2) Map2={4:Bean1(I=5 L=0 Map1={} Bean2=Bean2(I=0) Map2={})})
Log=[{1:Value=1,4:{1:Value=2},3: Putted:{3:3} Removed:[],5: Putted:{4:Bean1(I=5 L=0 Map1={} Bean2=Bean2(I=0) Map2={})} Removed:[] Changed:[{1:Value=5}]}]
AllLog=[{1:Value=1,4:{1:Value=2},3: Putted:{3:3} Removed:[],5: Putted:{4:Bean1(I=5 L=0 Map1={} Bean2=Bean2(I=0) Map2={})} Removed:[] Changed:[{1:Value=5}]},{0:Value=Bean1(I=1 L=0 Map1={3:3} Bean2=Bean2(I=2) Map2={4:Bean1(I=5 L=0 Map1={} Bean2=Bean2(I=0) Map2={})})}]}");
				return 0;
			}).Call();
		}

		private void Update2(Rocks rocks)
		{
			rocks.NewProcedure(() =>
			{
				var table = rocks.OpenTable<int, Bean1>("tRocksRaft");
				Update(table);
				VerifyChanges(@"{(tRocksRaft,1):State=2 PutValue=
Log=[{1:Value=1,4:{1:Value=2},3: Putted:{3:3} Removed:[],5: Putted:{4:Bean1(I=5 L=0 Map1={} Bean2=Bean2(I=0) Map2={})} Removed:[] Changed:[{1:Value=5}]}]
AllLog=[{1:Value=1,4:{1:Value=2},3: Putted:{3:3} Removed:[],5: Putted:{4:Bean1(I=5 L=0 Map1={} Bean2=Bean2(I=0) Map2={})} Removed:[] Changed:[{1:Value=5}]}]}");
				return 0;
			}).Call();
		}

		private void Update3(Rocks rocks)
		{
			rocks.NewProcedure(() =>
			{
				var table = rocks.OpenTable<int, Bean1>("tRocksRaft");
				Update(table);
				// 重新put，将会让上面的修改树作废。但所有的日志树都可以从All中看到。
				var bean1put = new Bean1();
				table.Put(1, bean1put);
				VerifyChanges(@"{(tRocksRaft,1):State=1 PutValue=Bean1(I=0 L=0 Map1={} Bean2=Bean2(I=0) Map2={})
Log=[]
AllLog=[{1:Value=1,4:{1:Value=2},3: Putted:{3:3} Removed:[],5: Putted:{4:Bean1(I=5 L=0 Map1={} Bean2=Bean2(I=0) Map2={})} Removed:[] Changed:[{1:Value=5}]},{0:Value=Bean1(I=0 L=0 Map1={} Bean2=Bean2(I=0) Map2={})}]}");
				return 0;
			}).Call();
		}

		[TestMethod]
        public void Test_1()
        {
			using var rocks = new Rocks();
			rocks.RegisterLog<LogMap1<int, int>>();
			rocks.RegisterLog<LogMap2<int, Bean1>>();

			Remove1(rocks);
			Update1(rocks);
			VerifyData(rocks, "");
			Update2(rocks);
			VerifyData(rocks, "");
			Update3(rocks);
			VerifyData(rocks, "");

			// 再次运行本测试，才会执行到 LoadSnapshot。
			rocks.Raft.LogSequence.Snapshot(true);
		}
	}
}
