package UnitTest.Zeze.Util;

import Zeze.Util.AtomicFileWriter;
import harness.Fast;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * U1-F1：AtomicOutputFile 构造器中 FileChannel.open 失败时已创建的 tmp 不被清理，
 * 按失败次数累积——违反 close() 注释自立的"周期性失败的调用点不得累积tmp"纪律
 * （Rocks.java 快照 zip 等周期调用点反复失败即按次累积）。
 * <p>
 * 修复：构造器内 open 包 try/catch，失败时 best-effort 删除 temp 后重抛。
 * 说明：open 失败（createTempFile 成功之后）无确定性触发手段——需要 mockito 静态
 * mock 或自定义 FileSystemProvider 注入，本测试模块均不依赖；本用例覆盖可达的
 * 构造失败模式（父目录不存在）固化"构造抛 IOException 则零 tmp 残留"契约，
 * open 失败路径的清理由代码审阅保障（与 close()/abort() 同用 deleteTempBestEffort）。
 */
@Fast
public class TestAtomicOutputFileCtorFailure {

	@Test
	public void testCtorFailureLeavesNoTmp() throws IOException {
		var dir = Files.createTempDirectory("atomic-ctor-fail-");
		// 可达的构造失败：父目录不存在（createTempFile 即抛 NoSuchFileException）
		var missingParent = dir.resolve("no-such-dir").resolve("target.bin");
		assertThrows(IOException.class, () -> AtomicFileWriter.openOutput(missingParent));
		assertEquals(0, countTmp(dir), "构造失败不得留下任何 *.tmp");

		// 正常路径不回归：构造成功 → abort → 目标未动且无 tmp 残留
		var target = dir.resolve("ok.bin");
		try (var out = AtomicFileWriter.openOutput(target)) {
			out.write("x".getBytes());
			out.abort();
		}
		assertFalse(Files.exists(target));
		assertEquals(0, countTmp(dir));
	}

	private static int countTmp(Path dir) throws IOException {
		try (var list = Files.list(dir)) {
			return (int)list.filter(p -> p.getFileName().toString().endsWith(".tmp")).count();
		}
	}
}
