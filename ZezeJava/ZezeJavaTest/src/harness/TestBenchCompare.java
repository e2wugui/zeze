package harness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Fast
public class TestBenchCompare {
	private static String metrics(String op, long count, int p99) {
		String prefix = "\"op." + op + '.';
		int latency = count == 0 ? 0 : 1;
		return prefix + "count\": " + count + ",\n"
				+ prefix + "tps\": " + count + ",\n"
				+ prefix + "mean_us\": " + latency + ",\n"
				+ prefix + "p50_us\": " + latency + ",\n"
				+ prefix + "p95_us\": " + latency + ",\n"
				+ prefix + "p99_us\": " + (count == 0 ? 0 : p99) + ",\n"
				+ prefix + "max_us\": " + (count == 0 ? 0 : p99 + 1) + '\n';
	}

	private static void fixture(Path directory, String filename, String contents) throws Exception {
		Files.createDirectories(directory);
		Files.writeString(directory.resolve(filename), '{' + contents + '}');
	}

	private static String compare(Path before, Path after) throws Exception {
		var bytes = new ByteArrayOutputStream();
		try (var out = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
			assertEquals(0, BenchCompare.compareOps(before, after, out), "tail metrics remain informational");
		}
		return bytes.toString(StandardCharsets.UTF_8);
	}

	@Test
	public void backendAndLegacyFilenamesBothExposeEveryActiveMetric(@TempDir Path root) throws Exception {
		var before = root.resolve("before");
		var after = root.resolve("after");
		fixture(before, "L_MmoGameSim_ktt.json", metrics("equipChange", 10, 99));
		fixture(after, "L_MmoGameSim_ktt.json", metrics("equipChange", 20, 199));
		fixture(before, "L_MmoGameSim.json", metrics("legacyOnly", 30, 88));
		fixture(after, "L_MmoGameSim.json", metrics("legacyOnly", 40, 188));
		var output = compare(before, after);
		assertTrue(output.contains("--- L_MmoGameSim_ktt.json ---"));
		assertTrue(output.contains("--- L_MmoGameSim.json ---"));
		for (var field : new String[] {"count", "tps", "mean_us", "p50_us", "p95_us", "p99_us", "max_us"}) {
			assertTrue(output.contains("op.equipChange." + field), output);
			assertTrue(output.contains("op.legacyOnly." + field), output);
		}
		assertTrue(output.contains("99.0") && output.contains("199.0"), "p99 values must be printed");
		assertTrue(output.contains("independent repeat required"));
		assertTrue(output.contains("no equivalence claim"));
		assertFalse(output.contains("WARNING"));
	}

	@Test
	public void zeroEventsAndZeroBaselineDoNotProduceNaN(@TempDir Path root) throws Exception {
		var before = root.resolve("before");
		var after = root.resolve("after");
		fixture(before, "L_MmoGameSim_ktt.json", metrics("inactive", 0, 0) + ','
				+ metrics("appeared", 0, 0) + ',' + metrics("subMicro", 10, 0));
		fixture(after, "L_MmoGameSim_ktt.json", metrics("inactive", 0, 0) + ','
				+ metrics("appeared", 5, 99) + ',' + metrics("subMicro", 10, 0));
		var output = compare(before, after);
		assertFalse(output.contains("op.inactive."), "inactive operations should not invent latency comparisons");
		assertTrue(output.contains("op.appeared.p99_us"));
		assertTrue(output.contains("N/A(no events)"));
		assertTrue(output.contains("op.subMicro.p99_us"), "an active operation may legitimately have a zero-microsecond percentile");
		assertFalse(output.contains("NaN"));
		assertFalse(output.contains("Infinity"));
		assertFalse(output.contains("WARNING"));
	}

	@Test
	public void mismatchedBackendsAndMissingActiveMetricsAreExplicit(@TempDir Path root) throws Exception {
		var before = root.resolve("before");
		var after = root.resolve("after");
		fixture(before, "L_MmoGameSim_kt.json", metrics("onlyBeforeBackend", 10, 99));
		fixture(after, "L_MmoGameSim_ktt.json", metrics("onlyAfterBackend", 10, 99));
		fixture(before, "L_MmoGameSim.json", metrics("loot", 10, 99));
		fixture(after, "L_MmoGameSim.json", metrics("loot", 10, 99).replace("\"op.loot.p99_us\": 99,\n", "")
				+ ",\n\"op.unknown.mean_us\": 1");
		fixture(before, "L_MmoGameSim_empty.json", "\"median\": 100");
		fixture(after, "L_MmoGameSim_empty.json", "\"median\": 100");
		var output = compare(before, after);
		assertTrue(output.contains("L_MmoGameSim_kt.json MISSING after file"));
		assertTrue(output.contains("L_MmoGameSim_ktt.json MISSING before file"));
		assertTrue(output.contains("WARNING op.loot.p99_us MISSING/INVALID in after"));
		assertTrue(output.contains("WARNING op.unknown.count MISSING/INVALID in before and after"));
		assertTrue(output.contains("--- L_MmoGameSim_empty.json ---"));
		assertTrue(output.contains("WARNING no operation metrics in either file"));
		assertFalse(output.contains("op.onlyBeforeBackend."), "different backend filenames must not be cross-paired");
		assertFalse(output.contains("op.onlyAfterBackend."));
	}
}
