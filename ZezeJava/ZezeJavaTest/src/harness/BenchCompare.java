package harness;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 基准对比工具（优化循环判定器）：java -cp ... harness.BenchCompare &lt;dirA&gt; &lt;dirB&gt;
 *
 * <p>宏观（MacroBench JSON）：按文件名配对，median 对比；|Δ| &gt; 3×√(stdevA²+stdevB²) 判显著
 * （IMPROVED/REGRESSED），否则 NOISE。约定 dirA=before、dirB=after。
 *
 * <p>微基准（jmh-*.json，JMH 自身格式）：score±scoreError 置信区间不重叠判显著。
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
