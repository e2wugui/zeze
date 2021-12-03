package Infinite;

import UnitTest.Zeze.Trans.TestGlobal;
import Zeze.Config;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

public class App {
    static final Logger logger = LogManager.getLogger(TestGlobal.class);

    demo.App app;
    Config config;

    public App(int serverId) {
        config = Config.Load("zeze.xml");
        config.setServerId(serverId);
        config.setFastRedoWhenConfict(false);
        config.setCheckpointPeriod(1000);
        var tdef = config.getDefaultTableConf();
        tdef.setCacheCleanPeriod(1000); // 提高并发
        tdef.setCacheCleanPeriodWhenExceedCapacity(0); // 超出容量时，快速尝试。
        tdef.setCacheCapacity(Simulate.CacheCapacity); // 减少容量，实际使用记录数要超过一些。让TableCache.Cleanup能并发起来。
        app = new demo.App();
    }

    public void Start() throws Throwable {
        app.Start(config);
    }

    public void Stop() throws Throwable {
        app.Stop();
    }

    public ArrayList<Task> RunningTasks = new ArrayList<>(Simulate.BatchTaskCount);

    public void Run(Tasks.Task task) {
        task.App = app;
        final int keyNumber = task.getKeyNumber();
        final int keyBound = task.getKeyBound();
        while (task.Keys.size() < keyNumber) {
            task.Keys.add((long)Zeze.Util.Random.getInstance().nextInt(keyBound));
        }
        for (var key : task.Keys) {
            Tasks.getRunCounter(task.getClass().getName(), key).incrementAndGet();
        }
        RunningTasks.add(Zeze.Util.Task.Run(app.Zeze.NewProcedure(task::call, task.getClass().getName())));
    }

    public void WaitAllRunningTasksAndClear() {
        Task.WaitAll(RunningTasks);
        RunningTasks.clear();
    }
}
