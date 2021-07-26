using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Serialize;

namespace Zeze.Transaction
{
    public sealed class TableSys : Table
    {
        private StorageSys TStorage;
        public override Storage Storage => TStorage;

        public override bool IsMemory => false;

        internal override void Close()
        {
            TStorage = null;
        }

        internal TableSys() : base("_sys_")
        {

        }

        public override ChangeVariableCollector CreateChangeVariableCollector(int variableId)
        {
            throw new NotImplementedException();
        }

        public AutoKeys AutoKeys => TStorage.AutoKeys;
        public Application Zeze { get; private set; }

        internal override Storage Open(Application app, Database database)
        {
            if (null != TStorage)
                throw new Exception("tablesys has opened");
            Zeze = app;
            TStorage = new StorageSys(app, database);
            return TStorage;
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

            public override void Close()
            {
                DatabaseTable.Close();
            }

            public override int EncodeN()
            {
                return 0;
            }

            public override int Encode0()
            {
                snapshotOfAutoKeys = AutoKeys.Encode();
                int c = 0;
                if (null != snapshotOfAutoKeys)
                    ++c;
                return c;
            }

            public override int Snapshot()
            {
                int c = 0;
                if (null != snapshotOfAutoKeys)
                    ++c;
                return c;
            }

            public override int Flush(Database.Transaction t)
            {
                int c = 0;
                if (null != snapshotOfAutoKeys)
                {
                    DatabaseTable.Replace(t, keyOfAutoKeys, snapshotOfAutoKeys);
                    ++c;
                }
                return c;
            }

            public override void Cleanup()
            {
                snapshotOfAutoKeys = null;
            }

        }
    }
}
