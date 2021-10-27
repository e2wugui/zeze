package Benchmark;

import demo.App;
import junit.framework.TestCase;

public class ABasicSimpleAddOneThread extends TestCase {
    public final static long AddCount = 10_000_000L;

    public void testBenchmark() {
        App.Instance.Start();
        try {
            App.Instance.Zeze.NewProcedure(this::Remove, "remove").Call();
            System.out.println("benchmark start...");
            var b = new Zeze.Util.Benchmark();
            for (long i = 0; i < AddCount; ++i) {
                App.Instance.Zeze.NewProcedure(this::Add, "Add").Call();
            }
            b.Report(this.getClass().getName(), AddCount);
            App.Instance.Zeze.NewProcedure(this::Check, "check").Call();
            App.Instance.Zeze.NewProcedure(this::Remove, "remove").Call();
        }
        finally {
            App.Instance.Stop();
        }
    }

    private int Check() {
        var r = App.Instance.demo_Module1.getTable1().getOrAdd(1L);
        assert r.getLong2() == AddCount;
        //System.out.println(r.getLong2());
        return 0;
    }

    private int Add() {
        var r = App.Instance.demo_Module1.getTable1().getOrAdd(1L);
        r.setLong2(r.getLong2() + 1);
        return 0;
    }

    private int Remove() {
        App.Instance.demo_Module1.getTable1().remove(1L);
        return 0;
    }
}
