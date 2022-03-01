using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Serialize;
using Zeze.Raft.RocksRaft;

namespace UnitTest.Zeze.Misc
{
    [TestClass]
    public class TestRaftConcurrentMaps
    {
        public class Value : Bean
        {
            public int Int { get; set; }

            public override void Decode(ByteBuffer bb)
            {
                Int = bb.ReadInt();
            }

            public override void Encode(ByteBuffer bb)
            {
                bb.WriteInt(Int);
            }

            protected override void InitChildrenRootInfo(Record.RootInfo root)
            {
            }
        }

        [TestMethod]
        public void TestRocksDbColumn()
        {
            var storage = new Rocks(".");
            var map = storage.OpenTable<int, Value>("int2value");
            var cpdir = storage.Checkpoint(out var index, out var term);
            try
            {
                Assert.IsTrue(storage.Backup(cpdir, "backup"));
            }
            finally
            {
                Directory.Delete(cpdir, true);
            }
        }

    }
}
