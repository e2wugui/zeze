package Zeze.Dbh2;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.OutLong;
import Zeze.Util.OutObject;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BenchClient {
	private static final Logger logger = LogManager.getLogger(BenchClient.class);

	private static Database newDatabase(String masterIp, int masterPort) {
		var databaseConf = new Config.DatabaseConf();
		databaseConf.setDatabaseType(Config.DbType.Dbh2);
		databaseConf.setDatabaseUrl("dbh2://" + masterIp + ":" + masterPort + "/" + "dbh2TestDb");
		databaseConf.setName("dbh2");
		return new Database(null, databaseConf);
	}

	public static void main(String[] args) {
		try {
			Task.tryInitThreadPool(null, null, null);
			Zeze.Net.Selectors.getInstance().add(Runtime.getRuntime().availableProcessors() - 1);

			var tableNumber = 4;
			var threadNumber = 4;
			var valueSize = 12;
			var masterIp = "127.0.0.1";
			var masterPort = 30000;
			var tableAccess = 2;

			for (int i = 0; i < args.length; ++i) {
				switch (args[i]) {
				case "-tableNumber":
					tableNumber = Integer.parseInt(args[++i]);
					break;
				case "-threadNumber":
					threadNumber = Integer.parseInt(args[++i]);
					break;
				case "-valueSize":
					valueSize = Integer.parseInt(args[++i]);
					break;
				case "-masterIp":
					masterIp = args[++i];
					break;
				case "-masterPort":
					masterPort = Integer.parseInt(args[++i]);
					break;
				case "-tableAccess":
					tableAccess = Integer.parseInt(args[++i]);
					break;
				default:
					throw new RuntimeException("unknown option " + args[i]);
				}
			}

			var tableAccessFinal = tableAccess;
			var database = newDatabase(masterIp, masterPort);
			var tables = new ArrayList<Zeze.Transaction.Database.AbstractKVTable>();
			for (int i = 0; i < tableNumber; ++i)
				tables.add((Database.AbstractKVTable)database.openTable("table" + i));

			var value = ByteBuffer.Wrap(Zeze.Util.Random.nextBinary(valueSize));
			var running = new OutObject<>(true);
			var futures = new ArrayList<Future<?>>();
			var transCounter = new AtomicLong();
			for (int i = 0; i < threadNumber; ++i) {
				futures.add(Task.runUnsafe(() -> {
					while (Boolean.TRUE.equals(running.value)) {
						// 限制所有key的范围，防止服务器占用太大硬盘。
						try (var trans = database.beginTransaction()) {
							for (int a = 0; a < tableAccessFinal; ++a) {
								var key = (Zeze.Util.Random.getInstance().nextLong() + 1) % 1000_00000;
								var keyBb = ByteBuffer.Allocate(9);
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
		} catch (Exception e) {
			logger.error("", e);
		}
	}
}
