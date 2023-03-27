package Dbh2;

import java.util.ArrayList;
import Zeze.Config;
import Zeze.Dbh2.Database;
import Zeze.Dbh2.Dbh2AgentManager;
import Zeze.Dbh2.Dbh2Manager;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.Task;
import org.junit.Assert;
import org.junit.Test;

// 测试整体结构(Dbh2Manager,Master,Agent)
public class Dbh2FullTest {

	private Database newDatabase(String dbName) {
		var databaseConf = new Config.DatabaseConf();
		databaseConf.setDatabaseType(Config.DbType.Dbh2);
		databaseConf.setDatabaseUrl("dbh2://127.0.0.1:30000/" + dbName);
		databaseConf.setName("dbh2");
		return new Database(null, databaseConf);
	}

	@Test
	public void testFull() throws Exception {
		Task.tryInitThreadPool(null, null, null);

		var master = new Zeze.Dbh2.Master.Main();
		master.start();
		var managers = new ArrayList<Dbh2Manager>();
		for (int i = 0; i < 3; ++i)
			managers.add(new Zeze.Dbh2.Dbh2Manager("manager" + i));
		for (var manager : managers)
			manager.start();

		var database = newDatabase("dbh2TestDb");
		var table1 = (Database.AbstractKVTable)database.openTable("table1");
		var table2 = (Database.AbstractKVTable)database.openTable("table2");

		var key = ByteBuffer.Wrap(new byte[] {});
		var key1 = ByteBuffer.Wrap(new byte[] { 1 });
		var value = ByteBuffer.Wrap(new byte[] { 1, 2, 3, 4 });

		try {
			try (var trans = database.beginTransaction()) {
				table1.replace(trans, key, value);
				table1.replace(trans, key1, value);
				table2.replace(trans, key, value);
				table2.replace(trans, key1, value);
				trans.commit();
			}
			{
				var valueFindKey = table1.find(key);
				Assert.assertNotNull(valueFindKey);
				Assert.assertEquals(valueFindKey, value);

				var valueFindKey1 = table1.find(key1);
				Assert.assertNotNull(valueFindKey1);
				Assert.assertEquals(valueFindKey1, value);
			}
			{
				var valueFindKey = table2.find(key);
				Assert.assertNotNull(valueFindKey);
				Assert.assertEquals(valueFindKey, value);

				var valueFindKey1 = table2.find(key1);
				Assert.assertNotNull(valueFindKey1);
				Assert.assertEquals(valueFindKey1, value);
			}

		} finally {
			master.stop();
			for (var manager : managers)
				manager.stop();
			database.close();
			Dbh2AgentManager.getInstance().clear();
		}
	}
}
