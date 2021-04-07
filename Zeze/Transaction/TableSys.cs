using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Serialize;

namespace Zeze.Transaction
{
    public sealed class TableSys : Table
    {
        private StorageSys storage;

        public override bool IsMemory => false;

        internal override void Close()
        {
            storage = null;
        }

        internal TableSys() : base("_sys_")
        {

        }

        public override ChangeVariableCollector CreateChangeVariableCollector(int variableId)
        {
            throw new NotImplementedException();
        }

        public AutoKeys AutoKeys => storage.AutoKeys;
        public Application Zeze { get; private set; }

        internal override Storage Open(Application app, Database database)
        {
            if (null != storage)
                throw new Exception("tablesys has opened");
            Zeze = app;
            storage = new StorageSys(app, database);
            return storage;
        }

        internal override void ReduceInvalidAllLocalOnly(int GlobalCacheManagerHashIndex)
        {
        }

        sealed class StorageSys : Storage
        {
            public AutoKeys AutoKeys { get; }

            private readonly ByteBuffer keyOfAutoKeys;
		    private ByteBuffer snapshotOfAutoKeys = null;

            private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

            internal StorageSys(Application app, Database database)
            {
                DatabaseTable = database.OpenTable("_sys_");

                int localInitValue = app.Config.AutoKeyLocalId;
                int localStep = app.Config.AutoKeyLocalStep;

                keyOfAutoKeys = ByteBuffer.Allocate(32);
                keyOfAutoKeys.WriteString("zeze.AutoKeys." + localInitValue);
                AutoKeys = new AutoKeys(DatabaseTable.Find(keyOfAutoKeys), localInitValue, localStep);
            }

            public Database.Table DatabaseTable { get; }

            public void Close()
            {
                DatabaseTable.Close();
            }

            public int EncodeN()
            {
                return 0;
            }

            public int Encode0()
            {
                snapshotOfAutoKeys = AutoKeys.Encode();
                int c = 0;
                if (null != snapshotOfAutoKeys)
                    ++c;
                return c;
            }

            public int Snapshot()
            {
                int c = 0;
                if (null != snapshotOfAutoKeys)
                    ++c;
                return c;
            }

            public int Flush()
            {
                int c = 0;
                if (null != snapshotOfAutoKeys)
                {
                    DatabaseTable.Replace(keyOfAutoKeys, snapshotOfAutoKeys);
                    ++c;
                }
                return c;
            }

            public void Cleanup()
            {
                snapshotOfAutoKeys = null;
            }

        }
    }
}
