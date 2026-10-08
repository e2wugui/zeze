package harness.proc;

/**
 * 睡眠后按环境变量条件退出（替代 cmd /c ping & if %VAR%==x exit N 形态）：
 * 环境变量 envName == envValue 时以 exitCode 退出，否则以 0 退出——同时探
 * 启动规格 env= 透传与子进程存活时长两个维度。
 */
public final class NapEnv {
	public static void main(String[] args) throws InterruptedException {
		if (args.length != 4)
			throw new IllegalArgumentException("usage: NapEnv <millis> <envName> <envValue> <exitCode>");
		Thread.sleep(Long.parseLong(args[0]));
		System.exit(args[2].equals(System.getenv(args[1])) ? Integer.parseInt(args[3]) : 0);
	}

	private NapEnv() {
	}
}
