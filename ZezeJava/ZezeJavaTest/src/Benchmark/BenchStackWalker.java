package Benchmark;

import Zeze.Util.OutLong;

public class BenchStackWalker {
	public static long stackHash1() {
		/*
		long r = 0;
		for (var ste : new Throwable().getStackTrace()) {
			r += ste.getClassName().hashCode()
					+ ste.getMethodName().hashCode()
					+ ste.getLineNumber();
		}
		return r;
		/*/
		return new Throwable().hashCode();
		// */
	}

	static final StackWalker sw = StackWalker.getInstance();

	public static long stackHash2() {
		var r = new OutLong();
		sw.walk(sfs -> {
			sfs.forEach(sf -> r.value += sf.getClassName().hashCode() + sf.getMethodName().hashCode() + sf.getLineNumber());
			return 0;
		});
		return r.value;
	}

	public static long fib1(long i) {
		if (i < 2)
			return 1;
		return fib1(i - 1) + fib1(i - 2) + stackHash1();
	}

	public static long fib2(long i) {
		if (i < 2)
			return 1;
		return fib2(i - 1) + fib2(i - 2) + stackHash2();
	}

	public static void testAll() {
		final int I = 25;

		var t = System.nanoTime();
		var r = fib1(I);
		System.out.println("Exception:   " + r + ", " + (System.nanoTime() - t) / 1_000_000 + "ms");

		t = System.nanoTime();
		r = fib2(I);
		System.out.println("StackWalker: " + r + ", " + (System.nanoTime() - t) / 1_000_000 + "ms");
	}

	public static void main(String[] args) {
		for (var ste : new Throwable().getStackTrace()) {
			System.out.println(ste.getClassName() + "." + ste.getMethodName() + ":" + ste.getLineNumber());
		}
		System.out.println("---");

		StackWalker.getInstance().walk(sfs -> {
			sfs.forEach(sf -> System.out.println(sf.getClassName() + "." + sf.getMethodName() + ":" + sf.getLineNumber()));
			return 0;
		});
		System.out.println("---");

		for (int i = 0; i < 5; i++) {
			System.out.println("---");
			testAll();
		}
	}
}
