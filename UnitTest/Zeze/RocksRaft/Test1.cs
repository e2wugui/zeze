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
    internal class Test1
    {
        public sealed class Bean1 : Bean
        {
			private int _i;
			private long _l;
			private CollMap1<int, int> _map1;
			private Bean2 _bean2;

			public int I
			{
				get
				{
					if (false == Transaction.Current.LogTryGet(ObjectId + 1, out var log)) return _i;
					return ((Log_i)log).Value;
				}

				set
				{
					Transaction.Current.LogPut(this.ObjectId, new Log_i() { Bean = this, VariableId = 1, Value = value, });
				}
			}

			public long L
			{
				get
				{
					if (false == Transaction.Current.LogTryGet(ObjectId + 2, out var log)) return _l;
					return ((Log_l)log).Value;
				}

				set
				{
					Transaction.Current.LogPut(this.ObjectId, new Log_l() { Bean = this, VariableId = 2, Value = value, });
				}
			}

			public CollMap1<int, int> Map1 => _map1;

			public Bean2 Bean2 => _bean2;

            public sealed class Log_i : Log<int>
			{
				public override void Apply(Bean holder) { ((Bean1)holder)._i = Value; }
			}

			public sealed class Log_l : Log<long>
			{
				public override void Apply(Bean holder) { ((Bean1)holder)._l = Value; }
			}

			public sealed class Log_map2 : LogMap1<int, int>
            {
				public override void Apply(Bean holder) { ((Bean1)holder)._map1.Apply(this); }
			}

			public sealed class Log_bean2 : LogBean
			{
				public override void Apply(Bean holder) { base.Apply(((Bean1)holder)._bean2); }
			}

			public Bean1()
            {
				_map1 = new CollMap1<int, int>() { VariableId = 3 };
				_bean2 = new Bean2() {  VariableId = 4 };
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
			}

		}

		public sealed class Bean2 : Bean
        {
			private int _i;

			public int I
			{
				get
				{
					if (false == Transaction.Current.LogTryGet(ObjectId + 1, out var log)) return _i;
					return ((Log_i)log).Value;
				}

				set
				{
					Transaction.Current.LogPut(this.ObjectId, new Log_i() { Bean = this, VariableId = 1, Value = value, });
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
		}

		[TestMethod]
        public void Test_1()
        {

        }
    }
}
