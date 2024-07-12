package Temp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.LongAdder;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.DatabaseRocksDb;
import Zeze.Transaction.Transaction;
import Zeze.Util.OutInt;
import Zeze.Util.Task;
import demo.App;
import demo.Module1.BValue;
import org.apache.logging.log4j.LogManager;

public class TestTransactionHalt {
	private static final int KEY_COUNT = 100;
	private static final int PROC_CONC = 100;
	private static final int CHECKPOINT_PERIOD = 0;
	private static final LongAdder counter = new LongAdder();

	private static long add() {
		var rand = ThreadLocalRandom.current();
		var r1 = App.Instance.demo_Module1.getTable1().getOrAdd(rand.nextLong(KEY_COUNT));
		var r3 = App.Instance.demo_Module1.getTable3().getOrAdd(rand.nextLong(KEY_COUNT));
		r1.setInt_1(r1.getInt_1() + 1);
		r3.setInt_1(r3.getInt_1() + 1);
		Transaction.whileCommit(() -> {
			counter.increment();
			Task.run(App.Instance.Zeze.newProcedure(TestTransactionHalt::add, "add"));
		});
		return 0L;
	}

	public static void main(String[] args) throws Exception {
		var cfg = Config.load("zeze.xml");
		// 不要ServiceManager.Agent。
		var sm = cfg.getServiceConf("Zeze.Services.ServiceManager.Agent");
		assert sm != null;
		sm.forEachConnector(sm::removeConnector);

		cfg.setCheckpointPeriod(CHECKPOINT_PERIOD);
		demo.App.getInstance().Start(cfg);

		var total1 = new OutInt();
		var total3 = new OutInt();
		App.Instance.Zeze.newProcedure(() -> {
			for (long k = 0; k < KEY_COUNT; k++) {
				total1.value += App.Instance.demo_Module1.getTable1().getOrAdd(k).getInt_1();
				total3.value += App.Instance.demo_Module1.getTable3().getOrAdd(k).getInt_1();
			}
			return 0L;
		}, "init").call();
		if (total1.value != total3.value && args.length == 0)
			throw new AssertionError("check failed: " + total1.value + " != " + total3.value);

		App.Instance.Zeze.newProcedure(() -> {
			for (long k = 0; k < KEY_COUNT; k++) {
				App.Instance.demo_Module1.getTable1().getOrAdd(k).setInt_1(0);
				App.Instance.demo_Module1.getTable3().getOrAdd(k).setInt_1(0);
			}
			return 0L;
		}, "init").call();

		// 基本不可能会发生这个情况：setLong2(0) 全部 flush 前就halt了。保险起见判断一下。
		App.Instance.Zeze.checkpointRun();

		if (App.Instance.demo_Module1.getTable1().getDatabase() instanceof DatabaseRocksDb) {
			DatabaseRocksDb.verifyAction = () -> {
				var rocksDb = (DatabaseRocksDb)App.Instance.demo_Module1.getTable1().getDatabase();
				var table1 = App.Instance.demo_Module1.getTable1().getName();
				var table3 = App.Instance.demo_Module1.getTable3().getName();
				var query = new HashMap<String, Set<ByteBuffer>>();
				var keys1 = query.computeIfAbsent(table1, key -> new HashSet<>());
				var keys2 = query.computeIfAbsent(table3, key -> new HashSet<>());
				for (long key = 0; key < KEY_COUNT; ++key) {
					var bbKey1 = ByteBuffer.Allocate();
					bbKey1.WriteLong(key);
					keys1.add(bbKey1);
					var bbKey2 = ByteBuffer.Allocate();
					bbKey2.WriteLong(key);
					keys2.add(bbKey2);
				}
				var result = rocksDb.finds(query);
				var sum1 = sum(result.get(table1));
				var sum3 = sum(result.get(table3));
				if (sum1 != sum3)
					System.out.print("+" + sum1 + "!=" + sum3);
				else
					System.out.print(".");
			};
		}
		for (int i = 0; i < PROC_CONC; i++)
			Task.run(App.Instance.Zeze.newProcedure(TestTransactionHalt::add, "add"));

		Task.scheduleUnsafe(1000, () -> {
			System.out.println("transactions: " + counter.sum());
			LogManager.shutdown();
			Runtime.getRuntime().halt(0);
		});

		Thread.sleep(Integer.MAX_VALUE);
	}

	public static int sum(Map<ByteBuffer, ByteBuffer> rs) {
		var sum = 0;
		if (null != rs) {
			//var sb = new StringBuilder(); sb.append("{");
			for (var e : rs.entrySet()) {
				//var key = e.getKey().ReadLong();
				var value = new BValue();
				value.decode(e.getValue());
				sum += value.getInt_1();
				//sb.append(key).append("=").append(value.getInt1()).append(",");
			}
			//sb.append("}"); System.out.println(sb);
		}
		return sum;
	}
}
