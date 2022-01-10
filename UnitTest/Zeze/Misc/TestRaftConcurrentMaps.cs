using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Raft;
using Zeze.Serialize;

namespace UnitTest.Zeze.Misc
{
    [TestClass]
    public class TestRaftConcurrentMaps
    {
        public class Value : Serializable
        {
            public int Int { get; set; }

            public void Decode(ByteBuffer bb)
            {
                Int = bb.ReadInt();
            }

            public void Encode(ByteBuffer bb)
            {
                bb.WriteInt(Int);
            }
        }

        [TestMethod]
        public void TestRocksDbColumn()
        {
            var storage = new MapsOnRocksDb(".");
            var map = storage.GetOrAdd<int, Value>("int2int");
            map.Update(1, (v) => v.Int = 0);
            Assert.AreEqual(0, map.GetOrAdd(1).Int);
            map.Update(1, (v) => v.Int++);
            Assert.AreEqual(1, map.GetOrAdd(1).Int);
            var cpdir = storage.Checkpoint();
            try
            {
                Assert.IsTrue(storage.Backup(cpdir, "backup"));
            }
            finally
            {
                Directory.Delete(cpdir, true);
            }
            map.Update(1, (v) => v.Int++);
            Assert.AreEqual(2, map.GetOrAdd(1).Int);
            Assert.IsTrue(storage.Restore("backup"));
            map = storage.GetOrAdd<int, Value>("int2int");
            Assert.AreEqual(1, map.GetOrAdd(1).Int);
        }

    }
}
