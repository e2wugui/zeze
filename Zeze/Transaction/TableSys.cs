﻿using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Serialize;

namespace Zeze.Transaction
{
    public class TableSys : Table
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

        public AutoKeys AutoKeys => storage.AutoKeys;

        internal override Storage Open(Zeze zeze, Database database)
        {
            if (null != storage)
                throw new Exception("tablesys has opened");
            storage = new StorageSys(zeze, database);
            return storage;
        }

        class StorageSys : Storage
        {
            public AutoKeys AutoKeys { get; }

            private readonly ByteBuffer keyOfAutoKeys;
		    private ByteBuffer snapshotValue = null;

            internal StorageSys(Zeze zeze, Database database)
            {
                DatabaseTable = database.OpenTable("_sys_");

                int localInitValue = zeze.Config.AutoKeyLocalId;
                int localStep = zeze.Config.AutoKeyLocalStep;

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
                snapshotValue = AutoKeys.Encode();
                return null == snapshotValue ? 0 : 1;
            }

            public int Snapshot()
            {
                return null == snapshotValue ? 0 : 1;
            }

            public int Flush()
            {
                if (null == snapshotValue)
                    return 0;
                DatabaseTable.Replace(keyOfAutoKeys, snapshotValue);
                return 1;
            }

            public void Cleanup()
            {
                snapshotValue = null;
            }

        }
    }
}
