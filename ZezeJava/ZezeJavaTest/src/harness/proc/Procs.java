package harness.proc;

import java.io.File;
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
	 * 按空白分隔（parseLaunchSpec 契约不支持引号），路径反斜杠按 Properties 语义双写转义。
	 * classpath 经 {@link #specFreeClasspath()} 剔除含空格条目（IDEA runner 注入的
	 * Program Files rt jar 等）——空白分隔协议承载不了空格，fail-loud 会挡死 IDE 内
	 * 运行的全部进程编排用例；被剔除的只是 runner 注入件，harness.proc.* 主类纯 JDK
	 * 自包含不依赖它们。 */
	public static String specArgs(String main, String... args) {
		var parts = new ArrayList<String>();
		parts.add("-cp");
		parts.add(specFreeClasspath());
		parts.add("harness.proc." + main);
		parts.addAll(List.of(args));
		return String.join(" ", parts).replace("\\", "\\\\");
	}

	private static String javaHomeBin(String bin) {
		return java.nio.file.Path.of(System.getProperty("java.home"), "bin", bin).toString();
	}

	/** spec args 可承载的 classpath：剔除含空格的条目（契约按空白分隔、不支持引号——
	 * 含空格条目整条不可表达）。正常构建（gradle/命令行）路径无空格，恒等返回原值。 */
	private static String specFreeClasspath() {
		var cp = System.getProperty("java.class.path");
		if (cp == null || !cp.contains(" "))
			return cp;
		var kept = new ArrayList<String>();
		for (var entry : cp.split(File.pathSeparator))
			if (!entry.contains(" "))
				kept.add(entry);
		return String.join(File.pathSeparator, kept);
	}

	/** 直接 ProcessBuilder 用的完整命令行：列表参数逐个原样传递（含空格路径安全——
	 * JDK 侧负责引号处理，不经 spec 的空白分隔协议，无需 specArgs 的空格防线）。 */
	public static List<String> command(String main, String... args) {
		var cmd = new ArrayList<String>();
		cmd.add(JAVAW);
		cmd.add("-cp");
		cmd.add(System.getProperty("java.class.path"));
		cmd.add("harness.proc." + main);
		cmd.addAll(List.of(args));
		return cmd;
	}

	private Procs() {
	}
}
