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
        public Schemas SchemasPrevious => storage.SchemasPrevious;
        public Application Zeze { get; private set; }

        internal void SaveSchemas(Schemas schemas)
        {
            // 这个是在Zeze.Start过程中设置，加个严格的全局写锁，以后Checkpoint保存一次就会清除。
            Zeze.Checkpoint.FlushReadWriteLock.EnterWriteLock();
            try
            {
                var bb = ByteBuffer.Allocate();
                schemas.Encode(bb);
                storage.snapshotOfSchemas = bb;
            }
            finally
            {
                Zeze.Checkpoint.FlushReadWriteLock.ExitWriteLock();
            }
        }

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

            public Schemas SchemasPrevious { get; }

            private readonly ByteBuffer keyOfSchemas;
            internal ByteBuffer snapshotOfSchemas = null;
            private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

            internal StorageSys(Application app, Database database)
            {
                DatabaseTable = database.OpenTable("_sys_");

                int localInitValue = app.Config.AutoKeyLocalId;
                int localStep = app.Config.AutoKeyLocalStep;

                keyOfAutoKeys = ByteBuffer.Allocate(32);
                keyOfAutoKeys.WriteString("zeze.AutoKeys." + localInitValue);
                AutoKeys = new AutoKeys(DatabaseTable.Find(keyOfAutoKeys), localInitValue, localStep);

                keyOfSchemas = ByteBuffer.Allocate(32);
                // 本来是localId无关的，为了合服不冲突，单独记录。
                keyOfSchemas.WriteString("zeze.Schemas." + localInitValue);
                var bbSchemas = DatabaseTable.Find(keyOfSchemas);
                if (null != bbSchemas)
                {
                    // 上一次的结构一值记着，直到下一次重启。
                    try
                    {
                        SchemasPrevious = new Schemas();
                        SchemasPrevious.Decode(bbSchemas);
                        SchemasPrevious.Compile();
                    }
                    catch (Exception ex)
                    {
                        SchemasPrevious = null;
                        logger.Error(ex, "Schemas Implement Changed?");
                    }
                }
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
                if (null != snapshotOfSchemas)
                    ++c;
                return c;
            }

            public int Snapshot()
            {
                int c = 0;
                if (null != snapshotOfAutoKeys)
                    ++c;
                if (null != snapshotOfSchemas)
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
                if (null != snapshotOfSchemas)
                {
                    DatabaseTable.Replace(keyOfSchemas, snapshotOfSchemas);
                    ++c;
                }
                return c;
            }

            public void Cleanup()
            {
                snapshotOfAutoKeys = null;
                snapshotOfSchemas = null;
            }

        }
    }
}
