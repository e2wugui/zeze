package Zeze.Dbh2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.OutLong;
import Zeze.Util.OutObject;
import Zeze.Util.Task;

public class BenchClient {

	private static Database newDatabase(String masterIp, int masterPort, String dbName) {
		var databaseConf = new Config.DatabaseConf();
		databaseConf.setDatabaseType(Config.DbType.Dbh2);
		databaseConf.setDatabaseUrl("dbh2://" + masterIp + ":" + masterPort + "/" + dbName);
		databaseConf.setName("dbh2");
		return new Database(null, databaseConf);
	}

	public static void main(String [] args) throws ExecutionException, InterruptedException, IOException {
		Task.tryInitThreadPool(null, null, null);
		Zeze.Net.Selectors.getInstance().add(Runtime.getRuntime().availableProcessors() - 1);

		var tableNumber = 4;
		var threadNumber = 4;
		var valueSize = 12;
		var masterIp = "127.0.0.1";
		var masterPort = 30000;
		var tableAccess = 2;

		for (int i = 0; i < args.length; ++i) {
			if (args[i].equals("-tableNumber"))
				tableNumber = Integer.parseInt(args[++i]);
			else if (args[i].equals("-threadNumber"))
				threadNumber = Integer.parseInt(args[++i]);
			else if (args[i].equals("-valueSize"))
				valueSize = Integer.parseInt(args[++i]);
			else if (args[i].equals("-masterIp"))
				masterIp = args[++i];
			else if (args[i].equals("-masterPort"))
				masterPort = Integer.parseInt(args[++i]);
			else if (args[i].equals("-tableAccess"))
				tableAccess = Integer.parseInt(args[++i]);
		}

		var tableAccessFinal = tableAccess;
		var database = newDatabase(masterIp, masterPort, "dbh2TestDb");
		var tables = new ArrayList<Zeze.Transaction.Database.AbstractKVTable>();
		for (int i = 0; i < tableNumber; ++i)
			tables.add((Database.AbstractKVTable)database.openTable("table" + i));

		var value = ByteBuffer.Wrap(Zeze.Util.Random.nextBinary(valueSize));
		var running = new OutObject<>(true);
		var futures = new ArrayList<Future<?>>();
		var transCounter = new AtomicLong();
		for (int i = 0; i < threadNumber; ++i) {
			futures.add(Task.runUnsafe(() -> {
				while (running.value) {
					// 限制所有key的范围，防止服务器占用太大硬盘。
					try (var trans = database.beginTransaction()) {
						for (int a = 0; a < tableAccessFinal; ++a) {
							var key = (Zeze.Util.Random.getInstance().nextLong() + 1) % 1000_00000;
							var keyBb = ByteBuffer.Allocate();
							keyBb.WriteLong(key);
							var table = tables.get(Zeze.Util.Random.getInstance().nextInt(tables.size()));
							table.replace(trans, keyBb, value);
						}
						trans.commit();
						transCounter.incrementAndGet();
					}
				}
			}, "table thread"));
		}
		var lastReportTime = new OutLong(System.currentTimeMillis());
		var lastReportCount = new OutLong();
		var reportTimer = Task.scheduleUnsafe(2000, 2000, () -> {
			var now = System.currentTimeMillis();
			var elapse = (now - lastReportTime.value) / 1000.0f;
			lastReportTime.value = now;
			var countNow = transCounter.get();
			var diff = countNow - lastReportCount.value;
			lastReportCount.value = countNow;

			System.out.println("transaction/s: " + diff / elapse);
		});

		var inputReader = new BufferedReader(new InputStreamReader(System.in));
		while (true) {
			var line = inputReader.readLine();
			if (line == null || line.equals("exit"))
				break;
		}
		reportTimer.cancel(true);
		running.value = false;
		for (var future : futures)
			future.get();
	}
}
