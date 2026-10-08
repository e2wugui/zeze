package harness.proc;

import java.util.ArrayList;
import java.util.List;

/**
 * 测试用真子进程的无窗口启动工具：Windows 上 GUI 子系统的 javaw 永不分配控制台，
 * 等价 CREATE_NO_WINDOW（JDK 未公开进程创建标志，此为纯 Java 等价物），同时保留
 * 真实 pid/退出码/存活/强杀全语义——Zoker 进程编排类测试专用。被启动的 main 见本包
 * {@link Exit}/{@link Nap}/{@link Flood}/{@link NapEnv}。
 */
public final class Procs {

	/** java.home 定位的 GUI 子系统 java（Windows=javaw.exe 不分配控制台；非 Windows 即 java）。 */
	public static final String JAVAW = javaHomeBin(
			System.getProperty("os.name", "").toLowerCase().contains("win") ? "javaw.exe" : "java");

	/** spec（Properties 语义，\\ 为转义符）里 command= 行用的 javaw 路径：反斜杠双写。 */
	public static String specJavaw() {
		return JAVAW.replace("\\", "\\\\");
	}

	/** spec 的 args= 值（不含 command= 的可执行名）：-cp、classpath、主类与附加参数，
	 * 按空白分隔（parseLaunchSpec 契约），classpath 含空格时 fail-loud；
	 * 路径反斜杠按 Properties 语义双写转义。 */
	public static String specArgs(String main, String... args) {
		var cp = System.getProperty("java.class.path");
		if (cp != null && cp.contains(" "))
			throw new IllegalStateException("java.class.path 含空格，空白分隔的 spec args 无法承载：" + cp);
		var parts = new ArrayList<String>();
		parts.add("-cp");
		parts.add(cp);
		parts.add("harness.proc." + main);
		parts.addAll(List.of(args));
		return String.join(" ", parts).replace("\\", "\\\\");
	}

	private static String javaHomeBin(String bin) {
		return java.nio.file.Path.of(System.getProperty("java.home"), "bin", bin).toString();
	}

	/** 直接 ProcessBuilder 用的完整命令行。 */
	public static List<String> command(String main, String... args) {
		var cp = System.getProperty("java.class.path");
		//noinspection StringConcatenationMissingWhitespace
		if (cp != null && cp.contains(" "))
			throw new IllegalStateException("java.class.path 含空格，空白分隔的 spec args 无法承载：" + cp);
		var cmd = new ArrayList<String>();
		cmd.add(JAVAW);
		cmd.add("-cp");
		cmd.add(cp);
		cmd.add("harness.proc." + main);
		cmd.addAll(List.of(args));
		return cmd;
	}

	private Procs() {
	}
}
