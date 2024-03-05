package Temp;

import java.util.ArrayList;
import java.util.Random;
import Zeze.Application;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Database;

public class TestDatabaseHalt {
	@SuppressWarnings({"InfiniteLoopStatement", "CallToPrintStackTrace"})
	public static void main(String [] args) throws Exception {
		var config = Config.load();
		var zeze = new Application("TestDatabaseHalt", config);
		var defaultDatabaseConf = config.getDatabaseConfMap().get("");
		var database = Config.createDatabase(zeze, defaultDatabaseConf);
		var tableCount = 5;
		var tables = new Database.AbstractKVTable[tableCount];
		for (var i = 0; i < tableCount; ++i)
			tables[i] = (Database.AbstractKVTable)database.openTable("TableTestDatabaseHalt" + i);

		// verify and get
		long count;
		{
			ByteBuffer key = ByteBuffer.Allocate();
			key.WriteInt(1);
			var counts = new ArrayList<Long>();
			for (Database.AbstractKVTable table : tables) {
				var value = table.find(key);
				if (null != value)
					counts.add(ByteBuffer.Wrap(value).ReadLong());
			}
			if (!counts.isEmpty() && counts.size() != tables.length)
				throw new RuntimeException("table count mismatch that has value."); // empty is ok.
			count = counts.get(0);
			for (var i = 1; i < counts.size(); ++i) {
				if (!counts.get(i).equals(count))
					throw new RuntimeException("halt test fail!!!");
			}
		}
		// start halt thread
		new Thread(() -> {
			try {
				Thread.sleep(new Random().nextInt(1000) + 500);
				Runtime.getRuntime().halt(1);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}).start();
		// infinite loop
		{
			while (true) {
				try (var trans = database.beginTransaction()) {
					ByteBuffer key = ByteBuffer.Allocate();
					key.WriteInt(1);
					ByteBuffer value = ByteBuffer.Allocate();
					value.WriteLong(count);
					for (Database.AbstractKVTable table : tables)
						table.replace(trans, key, value);
					trans.commit();
				}
				count++;
			}
		}
	}
}
