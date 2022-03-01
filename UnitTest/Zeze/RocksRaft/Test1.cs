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
						return ((Log_i)log).Value;
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
						Transaction.Current.PutLog(new Log_i() { Parent = this, VariableId = 1, Value = value, });
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
						return ((Log_l)log).Value;
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
						Transaction.Current.PutLog(new Log_l() { Parent = this, VariableId = 2, Value = value, });
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

            public sealed class Log_i : Log<int>
			{
				public override void FollowerApply(Bean parent) { ((Bean1)parent)._i = Value; }
				public override void LeaderApply() { ((Bean1)Parent)._i = Value; }
			}

			public sealed class Log_l : Log<long>
			{
				public override void FollowerApply(Bean parent) { ((Bean1)parent)._l = Value; }
				public override void LeaderApply() { ((Bean1)Parent)._l = Value; }
			}

			public sealed class Log_map1 : LogMap1<int, int>
            {
				public override void FollowerApply(Bean parent) { ((Bean1)parent)._map1.FollowerApply(this); }
				public override void LeaderApply() { ((Bean1)Parent)._map1.LeaderApply(this); }
			}

			public sealed class Log_bean2 : LogBean
			{
				public override void FollowerApply(Bean parent) { base.FollowerApply(((Bean1)parent)._bean2); }
				public override void LeaderApply() { base.FollowerApply(((Bean1)Parent)._bean2);}
			}

			public sealed class Log_map2 : LogMap2<int, Bean1>
			{
				public override void FollowerApply(Bean parent) { ((Bean1)parent)._map2.FollowerApply(this); }
				public override void LeaderApply() { ((Bean1)Parent)._map2.LeaderApply(this); }
			}

			public Bean1()
            {
				_map1 = new CollMap1<int, int>() { VariableId = 3, LogFactory = () => new Log_map1() };
				_bean2 = new Bean2() { VariableId = 4 };
				_map2 = new CollMap2<int, Bean1>() { VariableId = 5, LogFactory = () => new Log_map2() };
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
					return ((Log_i)log).Value;
				}

				set
				{
					Transaction.Current.PutLog(new Log_i() { Parent = this, VariableId = 1, Value = value, });
				}
			}

			public sealed class Log_i : Log<int>
			{
				public override void FollowerApply(Bean holder) { ((Bean2)holder)._i = Value; }
				public override void LeaderApply() { ((Bean2)Parent)._i = Value; }
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
		}

		[TestMethod]
        public void Test_1()
        {
			var rocks = new Rocks();
			var table = rocks.OpenTable<int, Bean1>("tRocksRaft");
			new Procedure(() =>
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

				// 重新put，将会让上面的修改树作废。但所有的日志树都可以从All中看到。
				var bean1put = new Bean1();
				table.Put(1, bean1put);

 				return 0;
			}).Call();
        }
    }
}
