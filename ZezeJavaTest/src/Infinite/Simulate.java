package Infinite;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

public class Simulate {
    static ArrayList<App> Apps = new ArrayList<>();

    @Before
    public void Before() throws Throwable {
        for (int serverId = 0; serverId < 10; ++serverId) {
            Apps.add(new App(serverId));
        }
        for (var app : Apps) {
            app.Start();
        }
    }

    @After
    public void After() throws Throwable {
        for (var app : Apps) {
            app.Stop();
        }
    }

    public static App randApp() {
        return randApp(Apps.size());
    }

    public static App randApp(int max) {
        if (max > Apps.size())
            max = Apps.size();
        return Apps.get(Zeze.Util.Random.getInstance().nextInt(max));
    }

    public final static int BatchTaskCount = 50000;
    public final static int CacheCapacity = 1000;
    public final static int AccessKeyBound = (int)(CacheCapacity * 1.20f);

    public boolean Infinite = false; // 当使用本目录的Main独立启动时，会设置为true。
    public static long BatchNumber = 0;

    @Test
    public void testMain() throws Throwable {
        Tasks.prepare();
        while (true) {
            ++BatchNumber;
            for (int i = 0; i < BatchTaskCount; ++i) {
                Tasks.randCreateTask().Run();
            }
            for (var app : Apps) {
                app.WaitAllRunningTasksAndClear();
            }
            Tasks.verifyBatch();
            if (!Infinite)
                break;
        }
        App.logger.fatal("Simulate Done.");
    }
}
