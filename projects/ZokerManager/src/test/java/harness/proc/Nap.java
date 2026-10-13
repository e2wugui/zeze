package harness.proc;

/**
 * 睡眠指定毫秒后以 0 退出（替代 ping -n N 127.0.0.1 的"活 N 秒真进程"形态）；
 * 被外部 destroy/destroyForcibly 终止时表现为被杀——两种生命周期语义均被测试使用。
 */
public final class Nap {
	public static void main(String[] args) throws InterruptedException {
		Thread.sleep(args.length > 0 ? Long.parseLong(args[0]) : 60_000);
	}

	private Nap() {
	}
}
