package Temp;

import java.util.ArrayList;
import java.util.Random;
import Zeze.Application;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Database;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TestDatabaseHalt {
	private static final Logger logger = LogManager.getLogger(TestDatabaseHalt.class);

	@SuppressWarnings({"InfiniteLoopStatement", "CallToPrintStackTrace"})
	public static void main(String[] args) throws Exception {
		Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
			//noinspection CallToPrintStackTrace
			e.printStackTrace();
			logger.error("uncaught exception in {}:", t, e);
		});

		var config = Config.load();
		var zeze = new Application("TestDatabaseHalt", config);
		zeze.getServiceManager().start();
		zeze.getServiceManager().waitReady();
		var defaultDatabaseConf = config.getDatabaseConfMap().get("");
		logger.info(defaultDatabaseConf.getDatabaseUrl());
		zeze.getDbh2AgentManager().start();
		var database = Config.createDatabase(zeze, defaultDatabaseConf);
		var tableCount = 5;
		var tables = new Database.AbstractKVTable[tableCount];
		for (var i = 0; i < tableCount; ++i)
			tables[i] = (Database.AbstractKVTable)database.openTable("TableTestDatabaseHalt" + i);
		for (var table : tables)
			table.waitReady();
		// verify and get
		var count = 0L;
		{
			ByteBuffer key = ByteBuffer.Allocate();
			key.WriteInt(1);
			var counts = new ArrayList<Long>();
			for (Database.AbstractKVTable table : tables) {
				var value = table.find(key);
				if (null != value)
					counts.add(ByteBuffer.Wrap(value).ReadLong());
			}
			logger.info("counts={}", counts);
			if (!counts.isEmpty()) {
				if (counts.size() != tables.length)
					throw new RuntimeException("table count mismatch that has value."); // empty is ok.
				count = counts.get(0);
				for (var i = 1; i < counts.size(); ++i) {
					if (!counts.get(i).equals(count))
						throw new RuntimeException("halt test fail!!!");
				}
			}
		}
		// start halt thread
		new Thread(() -> {
			try {
				Thread.sleep(new Random().nextInt(500) + 500);
				logger.info("halt!");
				Runtime.getRuntime().halt(1314);
			} catch (Exception ex) {
				logger.error("", ex);
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
