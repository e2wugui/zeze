using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Serialize;
using Zeze.Raft.RocksRaft;
using Zeze.Util;

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

            public override void FollowerApply(Log log)
            {
                throw new NotImplementedException();
            }

            public override void LeaderApplyNoRecursive(Log log)
            {
                throw new NotImplementedException();
            }

            public override Bean CopyBean()
            {
                throw new NotImplementedException();
            }
        }

        [TestMethod]
        public void TestRocksDbColumn()
        {
            FileSystem.DeleteDirectory("127.0.0.1_6000");
            var storage = new Rocks("127.0.0.1:6000");

            // 数据修改相关测试已经移到 UnitTest/Zeze/RocksRaft/ 下。
            storage.RegisterTableTemplate<int, Value>("int2value");

            var cpdir = storage.Checkpoint(out var index, out var term);
            try
            {
                Assert.IsTrue(storage.Backup(cpdir, "backup"));
                Assert.IsTrue(storage.Restore("backup"));
            }
            finally
            {
                Directory.Delete(cpdir, true);
            }
        }

    }
}
