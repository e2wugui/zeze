package Zeze.Transaction;

import Zeze.Config;

public class DatabaseTikv extends Database {
    public DatabaseTikv(Config.DatabaseConf conf) {
        super(conf);

    }

    @Override
    public Transaction BeginTransaction() {
        return null;
    }

    @Override
    public Table OpenTable(String name) {
        return null;
    }

    public class TikvTrans implements Database.Transaction {

        @Override
        public void Commit() {

        }

        @Override
        public void Rollback() {

        }

        @Override
        public void close() throws Exception {

        }
    }

}
