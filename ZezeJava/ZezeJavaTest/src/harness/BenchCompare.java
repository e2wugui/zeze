package harness;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 基准对比工具（优化循环判定器）：java -cp ... harness.BenchCompare &lt;dirA&gt; &lt;dirB&gt;
 *
 * <p>宏观（MacroBench JSON）：按文件名配对，median 对比；|Δ| &gt; 3×√(stdevA²+stdevB²) 判显著
 * （IMPROVED/REGRESSED），否则 NOISE。约定 dirA=before、dirB=after。
 *
 * <p>游戏模拟（L_MmoGameSim*.json，flat key "op.&lt;name&gt;.p50_us" 等）：按完整文件名配对，
 * 输出全部活跃操作的计数、速率及延迟。尾延迟仅作信息输出，必须独立复测，不据此宣称等价。
 *
 * <p>微基准（jmh-*.json，JMH 自身格式）：score±scoreError 置信区间不重叠判显著。
 * 带 params 的 JMH 矩阵应使用 ZezeJavaTest/scripts/compare_jmh.py，避免旧名称配对混淆参数行。
 */
public final class BenchCompare {
	private BenchCompare() {
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.err.println("usage: BenchCompare <dirBefore> <dirAfter>");
			System.exit(2);
		}
		int regressions = 0;
		regressions += compareMacro(Path.of(args[0]), Path.of(args[1]));
		regressions += compareOps(Path.of(args[0]), Path.of(args[1]), System.out);
		regressions += compareJmh(Path.of(args[0]), Path.of(args[1]));
		System.exit(regressions > 0 ? 1 : 0);
	}

	private static int compareMacro(Path dirA, Path dirB) throws IOException {
		List<Path> files;
		try (Stream<Path> s = Files.list(dirA)) {
			files = s.filter(p -> p.getFileName().toString().endsWith(".json")
					&& !p.getFileName().toString().startsWith("jmh-")).sorted().toList();
		} catch (IOException e) {
			return 0;
		}
		System.out.println("=== Macro (median, gate = 3*sqrt(stdevA^2+stdevB^2)) ===");
		int regressions = 0;
		for (var fa : files) {
			var fb = dirB.resolve(fa.getFileName());
			if (!Files.exists(fb)) {
				System.out.printf("%-42s MISSING in %s%n", fa.getFileName(), dirB);
				continue;
			}
			double medianA = jsonDouble(fa, "median"), stdevA = jsonDouble(fa, "stdev");
			double medianB = jsonDouble(fb, "median"), stdevB = jsonDouble(fb, "stdev");
			double noise = Math.sqrt(stdevA * stdevA + stdevB * stdevB);
			double delta = medianB - medianA;
			double deltaPct = 100 * delta / medianA;
			String verdict;
			if (Math.abs(delta) > 3 * noise)
				verdict = delta > 0 ? "IMPROVED" : "REGRESSED";
			else
				verdict = "NOISE";
			if ("REGRESSED".equals(verdict))
				regressions++;
			System.out.printf("%-42s %12.0f -> %12.0f  %+7.2f%%  (noise %.0f)  %s%n",
					fa.getFileName(), medianA, medianB, deltaPct, noise, verdict);
		}
		return regressions;
	}

	private static final List<String> OP_FIELDS = List.of("count", "tps", "mean_us", "p50_us", "p95_us", "p99_us", "max_us");
	private static final Pattern OP_KEY = Pattern.compile(
			"\"op\\.([^\".]+)\\.(?:count|tps|mean_us|p50_us|p95_us|p99_us|max_us)\"\\s*:");

	// Local output stream keeps fixture tests independent of the process-wide stdout.
	static int compareOps(Path dirA, Path dirB, PrintStream out) throws IOException {
		var names = new TreeSet<>(gameOpFiles(dirA));
		names.addAll(gameOpFiles(dirB));
		if (names.isEmpty())
			return 0;
		out.println("=== Game ops (informational only, no gate; independent repeat required, no equivalence claim) ===");
		for (var name : names) {
			var fa = dirA.resolve(name);
			var fb = dirB.resolve(name);
			if (!Files.isRegularFile(fa) || !Files.isRegularFile(fb)) {
				out.printf("%s MISSING %s file; pairing requires the complete filename%n", name,
						!Files.isRegularFile(fa) ? "before" : "after");
				continue;
			}
			String ta = Files.readString(fa), tb = Files.readString(fb);
			var operations = new TreeSet<>(gameOperations(ta));
			operations.addAll(gameOperations(tb));
			out.printf("--- %s ---%n", name);
			if (operations.isEmpty()) {
				out.println("WARNING no operation metrics in either file; active operations cannot be assessed");
				continue;
			}
			for (var operation : operations) {
				String prefix = "op." + operation + '.';
				Double countA = opValue(ta, prefix + "count");
				Double countB = opValue(tb, prefix + "count");
				if (countA == null || countB == null)
					missingOpValue(out, prefix + "count", countA == null, countB == null);
				if (!(countA != null && countA > 0 || countB != null && countB > 0))
					continue;
				for (var field : OP_FIELDS) {
					String key = prefix + field;
					Double va = field.equals("count") ? countA : opValue(ta, key);
					Double vb = field.equals("count") ? countB : opValue(tb, key);
					boolean noEventsA = field.endsWith("_us") && countA != null && countA == 0;
					boolean noEventsB = field.endsWith("_us") && countB != null && countB == 0;
					if (!field.equals("count") && (va == null && !noEventsA || vb == null && !noEventsB))
						missingOpValue(out, key, va == null && !noEventsA, vb == null && !noEventsB);
					if (va == null && vb == null)
						continue;
					String percent = !noEventsA && !noEventsB && va != null && vb != null && va > 0
							? String.format(Locale.ROOT, "%+.2f%%", 100 * (vb - va) / va) : "N/A";
					out.printf(Locale.ROOT, "%-42s %16s -> %16s  %s%n", key,
							formatOpValue(va, noEventsA), formatOpValue(vb, noEventsB), percent);
				}
			}
		}
		return 0; // Tail information never changes the throughput regression verdict.
	}

	private static Set<String> gameOpFiles(Path dir) throws IOException {
		if (!Files.isDirectory(dir))
			return Set.of();
		try (Stream<Path> files = Files.list(dir)) {
			var names = new TreeSet<String>();
			files.filter(Files::isRegularFile).map(p -> p.getFileName().toString())
					.filter(n -> n.startsWith("L_MmoGameSim") && n.endsWith(".json")).forEach(names::add);
			return names;
		}
	}

	private static Set<String> gameOperations(String text) {
		var operations = new TreeSet<String>();
		var keys = OP_KEY.matcher(text);
		while (keys.find())
			operations.add(keys.group(1));
		return operations;
	}

	private static Double opValue(String text, String key) {
		try {
			double value = textDouble(text, key, 0, Integer.MAX_VALUE);
			return Double.isFinite(value) && value >= 0 ? value : null;
		} catch (IllegalStateException | NumberFormatException missingOrInvalid) {
			return null;
		}
	}

	private static void missingOpValue(PrintStream out, String key, boolean before, boolean after) {
		out.printf("WARNING %s MISSING/INVALID in %s%n", key, before && after ? "before and after" : before ? "before" : "after");
	}

	private static String formatOpValue(Double value, boolean noEvents) {
		return noEvents ? "N/A(no events)" : value == null ? "MISSING" : String.format(Locale.ROOT, "%.1f", value);
	}

	private static int compareJmh(Path dirA, Path dirB) throws IOException {
		var listA = readJmh(dirA);
		var listB = readJmh(dirB);
		if (listA.isEmpty() || listB.isEmpty())
			return 0;
		System.out.println("=== JMH (score, CI non-overlap) ===");
		int regressions = 0;
		for (var ra : listA) {
			for (var rb : listB) {
				if (!ra.name.equals(rb.name))
					continue;
				double deltaPct = 100 * (rb.score - ra.score) / ra.score;
				boolean overlap = ra.score + ra.error >= rb.score - rb.error
						&& rb.score + rb.error >= ra.score - ra.error;
				String verdict = overlap ? "NOISE" : (rb.score > ra.score ? "IMPROVED" : "REGRESSED");
				if ("REGRESSED".equals(verdict))
					regressions++;
				System.out.printf("%-52s %12.0f -> %12.0f  %+7.2f%%  %s%n",
						ra.name, ra.score, rb.score, deltaPct, verdict);
			}
		}
		return regressions;
	}

	private record JmhRow(String name, double score, double error) {
	}

	private static List<JmhRow> readJmh(Path dir) throws IOException {
		List<JmhRow> rows = new ArrayList<>();
		try (Stream<Path> s = Files.list(dir)) {
			for (var p : s.filter(f -> f.getFileName().toString().startsWith("jmh-")).toList()) {
				var text = Files.readString(p);
				for (int i = 0; ; ) {
					int b = text.indexOf("\"benchmark\"", i);
					if (b < 0)
						break;
					int colon = text.indexOf(':', b);
					int q1 = text.indexOf('"', colon);
					int q2 = text.indexOf('"', q1 + 1);
					String name = text.substring(q1 + 1, q2);
					int pm = text.indexOf("\"primaryMetric\"", q2);
					int next = text.indexOf("\"benchmark\"", q2);
					int scopeEnd = next < 0 ? text.length() : next;
					if (pm >= 0 && pm < scopeEnd) {
						double score = textDouble(text, "score", pm, scopeEnd);
						double error = textDouble(text, "scoreError", pm, scopeEnd);
						rows.add(new JmhRow(name, score, error));
					}
					i = q2;
				}
			}
		} catch (IOException ignored) {
			// 目录不存在或无 jmh 文件：跳过
		}
		return rows;
	}

	private static double jsonDouble(Path file, String key) throws IOException {
		return textDouble(Files.readString(file), key, 0, Integer.MAX_VALUE);
	}

	// 兼容两种 JSON 风格："key": v（MacroBench）与 "key" : v（JMH）
	private static double textDouble(String text, String key, int from, int to) {
		int i = text.indexOf("\"" + key + "\"", from);
		if (i < 0 || i > to)
			throw new IllegalStateException("key not found: " + key + " in range");
		i = text.indexOf(':', i + key.length() + 2);
		if (i < 0)
			throw new IllegalStateException("colon not found: " + key);
		i++;
		while (i < text.length() && Character.isWhitespace(text.charAt(i)))
			i++;
		int end = i;
		while (end < text.length() && (Character.isDigit(text.charAt(end)) || "+-.eE".indexOf(text.charAt(end)) >= 0))
			end++;
		return Double.parseDouble(text.substring(i, end));
	}
}
