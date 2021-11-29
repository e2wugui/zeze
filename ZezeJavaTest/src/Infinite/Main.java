package Infinite;

public class Main {
    public final static void main(String[] args) throws Throwable {
        var simulate = new Simulate();
        simulate.Infinite = true; // 一直执行。
        simulate.Before();
        try {
            simulate.testMain();
        } finally {
            simulate.After();
        }
    }
}
