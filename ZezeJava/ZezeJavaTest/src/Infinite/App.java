package Infinite;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import Zeze.Config;
import Zeze.Transaction.DispatchMode;
import Zeze.Util.Random;
import Zeze.Util.Task;

public class App {
	final demo.App app = new demo.App();
	private final ArrayList<Future<?>> RunningTasks = new ArrayList<>(Simulate.BatchTaskCount);
	private final Config config;
	private final CoverHistory coverHistory;

	public App(int serverId) {
		config = Config.load("zeze.xml");
		config.setServerId(serverId);
		config.setFastRedoWhenConflict(false);
		config.setCheckpointPeriod(1000);
		config.getServiceConfMap().remove("Zeze.Onz.Server");

		var tdef = config.getDefaultTableConf();
		// 提高并发
		tdef.setCacheCleanPeriod(1000);
		// 超出容量时，快速尝试。
		tdef.setCacheCleanPeriodWhenExceedCapacity(100);
		// 减少容量，实际使用记录数要超过一些。让TableCache.Cleanup能并发起来。
		tdef.setCacheCapacity(Simulate.CacheCapacity);
		tdef.setCacheFactor(1.0f);

		var tflush = config.getTableConfMap().get("demo_Module1_tflush");
		// 提高并发
		tflush.setCacheCleanPeriod(1000);
		// 超出容量时，快速尝试。
		tflush.setCacheCleanPeriodWhenExceedCapacity(100);
		// 减少容量，实际使用记录数要超过一些。让TableCache.Cleanup能并发起来。
		tflush.setCacheCapacity(Tasks.tflushInt1Trade.CacheCapacity);
		tflush.setCacheFactor(1.0f);

		config.getServiceConfMap().remove("TestServer");
		coverHistory = new CoverHistory(this.app);
	}

	public int getServerId() {
		return config.getServerId();
	}

	public void Start() throws Exception {
		app.Start(config);
	}

	public void Stop() throws Exception {
		for (var task : RunningTasks) {
			task.cancel(false);
		}
		app.Stop();
	}

	void Run(Tasks.Task task) {
		task.App = app;
		String name = task.getClass().getName();
		int keyBound = task.getKeyBound();
		for (int i = 0, n = task.getKeyNumber(); i < n; i++) {
			long key;
			do
				key = Random.getInstance().nextInt(keyBound);
			while (!task.Keys.add(key));
			Tasks.getKeyCounter(name, key).increment();
		}
		Tasks.getRunCounter(name).increment();
		RunningTasks.add(task.IsProcedure()
				? Task.runUnsafe(app.Zeze.newProcedure(task, name), DispatchMode.Normal)
				: Task.runUnsafe(task::call, name, DispatchMode.Normal));
	}

	public void startTest() {
		coverHistory.submitTasks();
	}

	public void WaitAllRunningTasksAndClear() throws ExecutionException, InterruptedException {
		Task.waitAll(RunningTasks);
		RunningTasks.clear();
		coverHistory.join();
	}
}
