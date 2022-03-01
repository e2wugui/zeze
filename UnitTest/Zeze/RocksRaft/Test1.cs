﻿using System;
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
					if (false == Transaction.Current.TryGetLog(ObjectId + 1, out var log)) return _i;
					return ((Log_i)log).Value;
				}

				set
				{
					Transaction.Current.PutLog(new Log_i() { Parent = this, VariableId = 1, Value = value, });
				}
			}

			public long L
			{
				get
				{
					if (false == Transaction.Current.TryGetLog(ObjectId + 2, out var log)) return _l;
					return ((Log_l)log).Value;
				}

				set
				{
					Transaction.Current.PutLog(new Log_l() { Parent = this, VariableId = 2, Value = value, });
				}
			}

			public CollMap1<int, int> Map1 => _map1;
			public CollMap2<int, Bean1> Map2 => _map2;

			public Bean2 Bean2 => _bean2;

            public sealed class Log_i : Log<int>
			{
				public override void Apply(Bean holder) { ((Bean1)holder)._i = Value; }
			}

			public sealed class Log_l : Log<long>
			{
				public override void Apply(Bean holder) { ((Bean1)holder)._l = Value; }
			}

			public sealed class Log_map1 : LogMap1<int, int>
            {
				public override void Apply(Bean holder) { ((Bean1)holder)._map1.Apply(this); }
			}

			public sealed class Log_bean2 : LogBean
			{
				public override void Apply(Bean holder) { base.Apply(((Bean1)holder)._bean2); }
			}

			public sealed class Log_map2 : LogMap2<int, Bean1>
			{
				public override void Apply(Bean holder) { ((Bean1)holder)._map2.Apply(this); }
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
				public override void Apply(Bean holder) { ((Bean2)holder)._i = Value; }
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
				value.I = 1;
				value.Bean2.I = 2;
				value.Map1.Put(3, 3);
				var bean1 = new Bean1();
				value.Map2.Put(4, bean1);
				bean1.I = 5;
				var bean1put = new Bean1();
				table.Put(1, bean1put);
 				return 0;
			}).Call();
        }
    }
}
