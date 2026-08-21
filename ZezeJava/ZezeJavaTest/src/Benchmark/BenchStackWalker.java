package Benchmark;

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
		return new Throwable().getStackTrace().length;
		// */
	}

	static final StackWalker sw = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

	public static long stackHash2() {
		return sw.walk(sfs -> {
			long h = 0;
			for (var it = sfs.iterator(); it.hasNext(); ) {
				var sf = it.next();
				h += 1; // sf.getClassName().hashCode() + sf.getMethodName().hashCode() + sf.getLineNumber();
			}
			return h;
		});
	}

	public static long fib1(long i) {
		if (i < 2)
			return 0;
		return fib1(i - 1) + fib1(i - 2) + stackHash1();
	}

	public static long fib2(long i) {
		if (i < 2)
			return 0;
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
