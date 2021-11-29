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

    public App randApp() {
        return Apps.get(Zeze.Util.Random.getInstance().nextInt(Apps.size()));
    }

    public final static int BatchTaskCount = 50000;

    public boolean Infinite = false; // 当使用本目录的Main独立启动时，会设置为true。
    public static long BatchNumber = 0;

    @Test
    public void testMain() {
        while (Infinite) {
            ++BatchNumber;
            for (var app : Apps) {
                app.ClearRunningTasks();
            }
            for (int i = 0; i < BatchTaskCount; ++i) {
                randApp().Run(Tasks.randCreateTask());
            }
            for (var app : Apps) {
                app.WaitAllRunningTasks();
            }
            Tasks.verifyBatch();
        }
    }
}
