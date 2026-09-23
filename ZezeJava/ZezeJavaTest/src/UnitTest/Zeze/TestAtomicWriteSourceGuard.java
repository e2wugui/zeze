package UnitTest.Zeze;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RFD1-03 I1机械守门：白名单外禁止截断式写（规约见AGENTS.md）。语句级匹配（拼到
 * 分号、先剥注释）；append/只读/CREATE_NEW/纯READ豁免。白名单调整须在提交信息说明理由。
 */
@Fast
public class TestAtomicWriteSourceGuard {

	/** 违例规则：regex命中语句 + 谓词确认是"截断式写"形态。 */
	private record Rule(Pattern regex, Predicate<String> isWrite) {
	}

	private static final List<Rule> RULES = List.of(
			// new FileOutputStream(...) 仅两参append形式豁免
			new Rule(Pattern.compile("new\\s+FileOutputStream\\("),
					stmt -> !stmt.contains(", true")),
			// new RandomAccessFile(...) 仅 "rw" 模式违例（"r" 只读豁免）
			new Rule(Pattern.compile("new\\s+RandomAccessFile\\("),
					stmt -> stmt.contains("\"rw\"")),
			// Files.newOutputStream/write/writeString 无APPEND且无CREATE_NEW（建新或失败，不截断）
			new Rule(Pattern.compile("Files\\.(newOutputStream|writeString|write)\\("),
					stmt -> !stmt.contains("APPEND") && !stmt.contains("CREATE_NEW")),
			// FileChannel.open 含 WRITE 即违例（纯 READ 豁免）
			new Rule(Pattern.compile("FileChannel\\.open\\("),
					stmt -> stmt.contains("WRITE")));

	// 白名单（相对各 src/main/java 根；理由随条目维护，调整须在提交信息说明）：
	// 原语自身及伴生；锁；.installing分块接收；zeze_cache锁文件（I4互斥，内容无关）；
	// 流式存储与可再生产物（dump/构建产物/索引重建——候选迁移点，迁移后收缩）。
	private static final Set<String> ALLOWLIST = Set.of(
			"Zeze/Util/AtomicFileWriter.java", // 原语自身（fsync助手）
			"Zeze/Util/AtomicOutputFile.java", // 原语伴生（temp句柄，rename前唯一合法截断点）
			"Zeze/Raft/ReceiveSnapshotting.java", // .installing 分块接收（RandomAccessFile rw；随R4登记表抽取自Raft.java迁移）
			"Zeze/Util/FileMutex.java", // 跨进程互斥锁文件（RandomAccessFile rw，互斥用，内容无关；ex-Zeze/Application.java随FND8-26抽取迁移）
			"Zeze/MQ/MQFileWithIndex.java", // MQ流式存储（追加+索引，既有自愈）
			"Zeze/Services/Daemon.java", // 运维临时文件
			"Zeze/Services/Log4jQuery/LogIndex.java", // 索引重建（可再生）
			"Zeze/Services/ZokerImpl/FileBin.java", // Zoker流式存储（候选迁移点）
			"Zeze/Transaction/AchillesHeelDaemon.java", // 记录文件（可再生）
			"Zeze/Arch/LinkdProvider.java", // 诊断dump（可再生）
			"Zeze/Arch/LinkdProviderService.java", // 诊断dump（可再生）
			"Zeze/Util/ClassReloader.java", // 临时agent jar即建即用；attach子进程classpath仅含本类，引原语会NoClassDefFoundError
			"Zeze/Util/DumpRocksDb.java" // dump工具
	);

	@Test
	public void noRawWriteOutsideAllowlist() throws IOException {
		// fnd7_*redo等测试临时目录可能在遍历中被同JVM其他测试并发清理（30轮压测1/30撞上）：
		// Files.walk懒迭代抛UncheckedIOException(NoSuchFileException)是扫描竞争非违例，重扫即可。
		var roots = walkExisting(Path.of(".."),
						p -> p.endsWith("src/main/java") && Files.isDirectory(p)
								&& !p.toString().contains("build"))
				.stream().limit(64).toList();
		assertTrue(!roots.isEmpty(), "必须找到至少一个src/main/java（ZezeJava/ZezeJavaTest同仓布局）");

		var violations = new ArrayList<String>();
		for (var root : roots) {
			for (var p : walkExisting(root, f -> f.toString().endsWith(".java"))) {
				var rel = root.relativize(p).toString().replace('\\', '/');
				if (ALLOWLIST.contains(rel))
					continue;
				for (var stmt : statementsOf(Files.readString(p, StandardCharsets.UTF_8))) {
					for (var rule : RULES) {
						var m = rule.regex().matcher(stmt);
						if (m.find() && rule.isWrite().test(stmt)) {
							violations.add(rel + " -> " + m.group().trim()
									+ " : " + stmt.strip().replaceAll("\\s+", " "));
							break;
						}
					}
				}
			}
		}
		assertTrue(violations.isEmpty(), () -> "I1违例（白名单外截断式写，须经Zeze.Util.AtomicFileWriter）：\n"
				+ String.join("\n", violations));
	}

	/** NoSuchFileException（并发删除）最多重扫3次，仍失败则透传；IOException照旧包装。 */
	private static List<Path> walkExisting(Path start, Predicate<Path> filter) {
		for (var attempt = 0; ; attempt++) {
			try (var walk = Files.walk(start)) {
				return walk.filter(filter).toList();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			} catch (UncheckedIOException e) {
				if (e.getCause() instanceof NoSuchFileException && attempt < 3)
					continue;
				throw e;
			}
		}
	}

	/** 剥注释（注释文本不得误触）后按';'重组语句：行级匹配会被换行实参骗过。 */
	private static List<String> statementsOf(String source) {
		var noComments = source.replaceAll("//[^\n]*", "")
				.replaceAll("(?s)/\\*.*?\\*/", "");
		var stmts = new ArrayList<String>();
		for (var raw : noComments.split(";"))
			if (!raw.isBlank())
				stmts.add(raw.replace('\n', ' ').replace('\r', ' '));
		return stmts;
	}
}
