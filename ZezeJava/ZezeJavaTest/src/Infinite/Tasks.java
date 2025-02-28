package Infinite;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.DatabaseMemory;
import Zeze.Transaction.GoBackZeze;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Util.FuncLong;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.OutLong;
import Zeze.Util.PerfCounter;
import Zeze.Util.Random;
import org.apache.logging.log4j.Level;
import org.junit.Assert;

public final class Tasks {
	public static final boolean debugTradeSum = false;
	private static final ConcurrentHashMap<String, LongConcurrentHashMap<LongAdder>> CounterKey = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<String, LongAdder> CounterRun = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<String, LongAdder> CounterSuccess = new ConcurrentHashMap<>();

	static LongConcurrentHashMap<LongAdder> getKeyCounters(String name) {
		return CounterKey.computeIfAbsent(name, __ -> new LongConcurrentHashMap<>());
	}

	static LongAdder getKeyCounter(String name, long key) {
		return getKeyCounters(name).computeIfAbsent(key, __ -> new LongAdder());
	}

	static LongAdder getRunCounter(String name) {
		return CounterRun.computeIfAbsent(name, __ -> new LongAdder());
	}

	static LongAdder getSuccessCounter(String name) {
		return CounterSuccess.computeIfAbsent(name, __ -> new LongAdder());
	}

	static void clearAllCounters() {
		CounterKey.clear();
		CounterRun.clear();
		CounterSuccess.clear();
		Table1Long2Add1.commitCount.reset();
		tflushInt1TradeConcurrentVerify.keys = null;
	}

	// 所有以long为key的记录访问可以使用这个基类。
	// 其他类型的key需要再定义新的基类。
	static abstract class Task implements FuncLong {
		final Set<Long> Keys = new HashSet<>();
		demo.App App;

		int getKeyNumber() {
			return 1;
		}

		int getKeyBound() {
			return Simulate.AccessKeyBound;
		}

		boolean IsProcedure() {
			return true;
		}

		void Run() {
			Simulate.getInstance().randApp().Run(this);
		}

		void prepare() throws Exception {
		}

		abstract long process();

		void verify() throws Exception {
			// default verify
			var name = getClass().getName();
			var runCount = getRunCounter(name).sum();
			var successCount = getSuccessCounter(name).sum();
			var stats = PerfCounter.instance().getOrAddProcedureInfo(name);
			var abortCount = stats.getOrAddResult(Procedure.AbortException).sum();
			var tooManyTry = stats.getOrAddResult(Procedure.TooManyTry).sum();
			Simulate.logger.info("  totalCount({})={}", name, runCount);
			Simulate.logger.info("successCount({})={}", name, successCount);
			if (abortCount != 0)
				Simulate.logger.warn("  abortCount({})={}", name, abortCount);
			if (tooManyTry != 0)
				Simulate.logger.warn("  tooManyTry({})={}", name, tooManyTry);
			if (runCount != successCount + abortCount + tooManyTry) {
				Simulate.logger.error("verify failed({}): {} != {} = {} + {} + {}\n{}",
						name, runCount, successCount + abortCount + tooManyTry, successCount, abortCount, tooManyTry,
						stats.toString(false));
				Assert.fail();
			}
		}

		@Override
		public long call() {
			var name = getClass().getName();
			var result = process();
			if (result == 0) {
				var txn = Transaction.getCurrent();
				if (txn != null)
					txn.runWhileCommit(() -> getSuccessCounter(name).increment());
				else
					getSuccessCounter(name).increment();
			} else
				Simulate.logger.error("{}.process() = {}", name, result);
			return result;
		}
	}

	static final class TaskFactory {
		final Supplier<Task> Factory;
		final int Weight;

		TaskFactory(Supplier<Task> factory, int weight) {
			Factory = factory;
			Weight = weight;
		}
	}

	static final ArrayList<TaskFactory> taskFactorys = new ArrayList<>();
	static int TotalWeight;

	static {
		// 新的操作数据的测试任务在这里注册。weight是权重，see randCreateTask();
		taskFactorys.add(new TaskFactory(Table1Long2Add1::new, 100));
		taskFactorys.add(new TaskFactory(Table1List9AddOrRemove::new, 100));
		taskFactorys.add(new TaskFactory(tflushInt1Trade::new, 100));
		taskFactorys.add(new TaskFactory(tflushInt1TradeConcurrentVerify::new, 100));

		taskFactorys.sort(Comparator.comparingInt(a -> a.Weight));
		for (var task : taskFactorys)
			TotalWeight += task.Weight;
	}

	static Task randCreateTask() {
		var rand = Random.getInstance().nextInt(TotalWeight);
		for (var task : taskFactorys) {
			if (rand < task.Weight)
				return task.Factory.get();
			rand -= task.Weight;
		}
		throw new RuntimeException("impossible!");
	}

	static void prepare() throws Exception {
		for (var tf : taskFactorys)
			tf.Factory.get().prepare();
	}

	static void verify() {
		for (var tf : taskFactorys) {
			try {
				tf.Factory.get().verify();
			} catch (Exception e) {
				Simulate.logger.error("verify exception:", e);
			}
		}
	}

	static class Table1Long2Add1 extends Task {
		@Override
		void prepare() throws Exception {
			// 所有使用 Table1 的测试都可以依赖这个 prepare，不需要单独写了。
			var app = Simulate.getInstance().randApp().app;
			app.Zeze.newProcedure(() -> {
				for (long key = 0; key < Simulate.AccessKeyBound; key++)
					app.demo_Module1.getTable1().remove(key);
				return 0L;
			}, Table1Long2Add1.class.getName() + ".prepare").call();
			app.Zeze.checkpointRun();
		}

		static final LongAdder commitCount = new LongAdder();

		@Override
		long process() {
			var key = Keys.iterator().next();
			var value = App.demo_Module1.getTable1().getOrAdd(key);
			var oldV = value.getLong2();
			value.setLong2(oldV + 1);
			var newV = value.getLong2();
			Transaction.whileCommit(() -> {
				var newV2 = App.demo_Module1.getTable1().getOrAdd(key).getLong2();
				var level = oldV + 1 != newV || newV != newV2 ? Level.ERROR : Level.INFO;
				if (level == Level.ERROR || debugTradeSum)
					Simulate.logger.log(level, "{} --- key:{}, value:{}->{}->{}",
							App.getZeze().getConfig().getServerId(), key, oldV, newV, newV2);
				commitCount.increment();
			});
			return 0L;
		}

		@Override
		void verify() throws Exception {
			super.verify();

			// verify 时，所有任务都执行完毕，不需要考虑并发。
			var name = Table1Long2Add1.class.getName();
			var app = Simulate.getInstance().randApp().app; // 任何一个app都能查到相同的结果。
			var success = getSuccessCounter(name).sum();
			var sum = 0L;
			for (var it = getKeyCounters(name).keyIterator(); it.hasNext(); ) {
				for (int tryCount = 10; ; ) {
					try {
						var key = it.next();
						var v1 = app.demo_Module1.getTable1().selectDirty(key);
						if (v1 == null)
							Simulate.logger.warn("app.demo_Module1.getTable1().selectDirty({}) = null", key);
						else {
							var v = v1.getLong2();
							sum += v;
							if (debugTradeSum)
								Simulate.logger.info("--- key:{} value:{}", key, v);
						}
						break;
					} catch (GoBackZeze e) {
						if (--tryCount <= 0)
							throw e;
						try {
							//noinspection BusyWait
							Thread.sleep(200);
						} catch (InterruptedException ex) {
							throw new RuntimeException(ex);
						}
					}
				}
			}
			if (success != sum) {
				var a = new OutLong();
				app.demo_Module1.getTable1().walk((k, v) -> {
					a.value += v.getLong2();
					return true;
				});
				Simulate.logger.error("Table1Long2Add1 verify failed: {} != {} (walk:{}, commitCount:{})",
						success, sum, a.value, commitCount.sum());
			}
			Assert.assertEquals(success, sum);
			Simulate.logger.debug("{}.verify OK!", name);
		}
	}

	static class Table1List9AddOrRemove extends Task {
		@Override
		long process() {
			var value = App.demo_Module1.getTable1().getOrAdd(Keys.iterator().next());
			// 使用 bool4 变量：用来决定添加或者删除。
			if (value.isBool4()) {
				// Simulate.logger.error("list9.size={}", value.getList9().size());
				if (!value.getList9().isEmpty())
					value.getList9().remove(value.getList9().size() - 1);
				value.setBool4(!value.getList9().isEmpty());
			} else {
				value.getList9().add(new demo.Bean1());
				if (value.getList9().size() > 50)
					value.setBool4(true); // 改成删除模式。
			}
			return 0L;
		}
	}

	// 在随机两个记录内进行交易。
	static class tflushInt1Trade extends Task {
		static final int KeyBoundTrade = Simulate.AccessKeyBound / 2;
		static final int CacheCapacity = Simulate.CacheCapacity / 2;

		@Override
		int getKeyNumber() {
			return 2;
		}

		@Override
		int getKeyBound() {
			return KeyBoundTrade;
		}

		@Override
		void Run() {
			Simulate.getInstance().randApp(2).Run(this);
		}

		@Override
		long process() {
			var it = Keys.iterator();
			long k1, k2;
			if (Random.getInstance().nextBoolean()) { // random swap
				k1 = it.next();
				k2 = it.next();
			} else {
				k2 = it.next();
				k1 = it.next();
			}
			var v1 = App.demo_Module1.getTflush().getOrAdd(k1);
			var v2 = App.demo_Module1.getTflush().getOrAdd(k2);
			var m1 = v1.getInt_1();
			var m2 = v2.getInt_1();
			var money = Random.getInstance().nextInt(1000);
			v1.setInt_1(m1 - money);
			v2.setInt_1(m2 + money);
			var r1 = v1.getInt_1();
			var r2 = v2.getInt_1();
			if (debugTradeSum) {
				Transaction.whileCommit(() -> Simulate.logger.info("{} --- {}:{}-{}={} {}:{}+{}={}",
						App.getZeze().getConfig().getServerId(), k1, m1, money, r1, k2, m2, money, r2));
			}
			return 0L;
		}

		@Override
		void verify() throws Exception {
			super.verify();

			var app = Simulate.getInstance().randApp().app; // 任何一个app都能查到相同的结果。
			long sum = 0;
			for (int key = 0; key < KeyBoundTrade; key++) {
				for (int tryCount = 10; ; ) {
					try {
						try {
							var value = app.demo_Module1.getTable1().selectDirty((long)key);
							if (value != null)
								sum += value.getInt_1();
						} catch (IllegalStateException e) {
							if (e.getMessage() != null && e.getMessage().contains("Acquire Failed"))
								key--; // retry
							else
								throw e;
						}
						break;
					} catch (GoBackZeze e) {
						if (--tryCount <= 0)
							throw e;
						try {
							//noinspection BusyWait
							Thread.sleep(200);
						} catch (InterruptedException ex) {
							throw new RuntimeException(ex);
						}
					}
				}
			}
			Assert.assertEquals(0, sum);
		}
	}

	static class tflushInt1TradeConcurrentVerify extends Task {
		private static volatile HashSet<ByteBuffer> keys;

		@Override
		int getKeyNumber() {
			return 0;
		}

		@Override
		boolean IsProcedure() {
			return false;
		}

		@Override
		long process() {
			var table1 = App.demo_Module1.getTflush();
			if (keys == null) {
				synchronized (tflushInt1TradeConcurrentVerify.class) {
					if (keys == null) {
						var ks = new HashSet<ByteBuffer>();
						for (int key = 0; key < tflushInt1Trade.KeyBoundTrade; key++)
							ks.add(table1.encodeKey((long)key));
						keys = ks;
					}
				}
			}
			var db = table1.getDatabase();
			if (db instanceof DatabaseMemory) {
				long sum = 0;
				var all = ((DatabaseMemory)db).finds(table1.getName(), keys);
				for (var valueBytes : all.values()) {
					if (valueBytes != null)
						sum += table1.decodeValue(valueBytes).getInt_1();
				}
				if (debugTradeSum && sum != 0) {
					for (var e : all.entrySet()) {
						var valueBytes = e.getValue();
						if (valueBytes != null) {
							valueBytes.ReadIndex = 0;
							e.getKey().ReadIndex = 0;
							// var k = table1.decodeKey(e.getKey());
							// var v = table1.decodeValue(valueBytes).getInt1();
							// Simulate.logger.info("=== {}:{}", k, v);
						}
					}
				}
				Assert.assertEquals(0, sum);
			}
			return 0L;
		}
	}

	private Tasks() {
	}
}
