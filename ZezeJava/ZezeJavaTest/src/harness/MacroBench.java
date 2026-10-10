package harness;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;

/**
 * 宏观吞吐基准骨架：多轮迭代 + 统计 + JSON 留档（bench-results/&lt;benchTag&gt;/&lt;name&gt;.json）。
 * 配套 gradle 任务 benchCore/benchCorePlatform（-PbenchTag=xxx 命名本次运行，默认 adhoc）。
 *
 * <p>契约：roundBody 自包含——每轮自身完成数据重置与执行（首轮前另有 warmups 轮不计测量），
 * 结果校验留在最后由调用者做（轮内做断言会把断言耗时混进测量）。吞吐按 tasksPerRound/墙钟计。
 *
 * <p>优化循环对比协议：同机同 JVM 参数下 before/after 各跑一次（不同 benchTag 目录），
 * 中位数变化 &gt; 3× 基线轮内标准差才算改进。JSON 不进库（gitignored），绝对数机器相关。
 */
public final class MacroBench {
	@FunctionalInterface
	public interface RoundBody {
		void run() throws Exception;
	}

	// 测量段由 body 自计时（准备段不计）：适合 E 族 checkpoint——脏化记录在计时段外
	@FunctionalInterface
	public interface TimedRound {
		double run() throws Exception;
	}

	private MacroBench() {
	}

	public static double[] run(String name, int warmups, int rounds, long tasksPerRound, RoundBody body)
			throws Exception {
		for (int i = 0; i < warmups; i++)
			body.run();
		double[] tps = new double[rounds];
		for (int i = 0; i < rounds; i++) {
			long start = System.nanoTime();
			body.run();
			double seconds = (System.nanoTime() - start) / 1e9;
			tps[i] = tasksPerRound / seconds;
		}
		report(name, tasksPerRound, tps);
		return tps;
	}

	public static double[] runTimed(String name, int warmups, int rounds, long tasksPerRound, TimedRound body)
			throws Exception {
		for (int i = 0; i < warmups; i++)
			body.run();
		double[] tps = new double[rounds];
		for (int i = 0; i < rounds; i++) {
			double seconds = body.run();
			if (seconds <= 0)
				throw new IllegalStateException("timed round returned non-positive seconds: " + seconds);
			tps[i] = tasksPerRound / seconds;
		}
		report(name, tasksPerRound, tps);
		return tps;
	}

	private static void report(String name, long tasksPerRound, double[] tps) throws IOException {
		double[] sorted = tps.clone();
		Arrays.sort(sorted);
		int n = sorted.length;
		double median = n % 2 == 1 ? sorted[n / 2] : (sorted[n / 2 - 1] + sorted[n / 2]) / 2;
		double mean = Arrays.stream(sorted).average().orElseThrow();
		double variance = Arrays.stream(sorted).map(v -> (v - mean) * (v - mean)).sum() / n;
		double stdev = Math.sqrt(variance);
		// 兼容旧 console 格式（tasks/s=），另加统计行；中位数为主口径
		System.out.printf("%s tasks/s=%.2f tasks=%d rounds=%d median=%.2f mean=%.2f min=%.2f max=%.2f stdev=%.2f cv=%.4f%n",
				name, median, tasksPerRound, n, median, mean, sorted[0], sorted[n - 1], stdev, stdev / mean);
		writeJson(name, tasksPerRound, tps, median, mean, stdev);
	}

	private static void writeJson(String name, long tasksPerRound, double[] tps,
								  double median, double mean, double stdev) throws IOException {
		var sb = new StringBuilder(512);
		sb.append("{\n");
		sb.append("  \"name\": \"").append(name).append("\",\n");
		sb.append("  \"git\": \"").append(gitSha()).append("\",\n");
		sb.append("  \"timestamp\": \"").append(Instant.now()).append("\",\n");
		// Task.isVirtualThreadEnabled() 硬编码 true，不可作真标志；实际开关是 useVirtualThread 属性
		sb.append("  \"virtualThread\": ").append(Zeze.Util.PropertiesHelper.getBool("useVirtualThread", true)).append(",\n");
		sb.append("  \"useVirtualThreadProp\": \"").append(System.getProperty("useVirtualThread", "")).append("\",\n");
		sb.append("  \"jdk\": \"").append(System.getProperty("java.version")).append("\",\n");
		sb.append("  \"processors\": ").append(Runtime.getRuntime().availableProcessors()).append(",\n");
		sb.append("  \"tasksPerRound\": ").append(tasksPerRound).append(",\n");
		sb.append("  \"rounds\": [").append(Arrays.toString(tps)).append("],\n");
		sb.append("  \"median\": ").append(median).append(",\n");
		sb.append("  \"mean\": ").append(mean).append(",\n");
		sb.append("  \"min\": ").append(tps.length > 0 ? Arrays.stream(tps).min().orElseThrow() : 0).append(",\n");
		sb.append("  \"max\": ").append(tps.length > 0 ? Arrays.stream(tps).max().orElseThrow() : 0).append(",\n");
		sb.append("  \"stdev\": ").append(stdev).append("\n");
		sb.append("}\n");
		var tag = System.getProperty("bench.tag", "adhoc");
		var dir = Path.of("bench-results", tag);
		Files.createDirectories(dir);
		Files.writeString(dir.resolve(name + ".json"), sb.toString(), StandardCharsets.UTF_8);
	}

	private static String gitSha() {
		try {
			var p = new ProcessBuilder("git", "rev-parse", "--short", "HEAD")
					.redirectErrorStream(true).start();
			var out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
			return p.waitFor() == 0 ? out : "unknown";
		} catch (Exception e) {
			return "unknown";
		}
	}
}
