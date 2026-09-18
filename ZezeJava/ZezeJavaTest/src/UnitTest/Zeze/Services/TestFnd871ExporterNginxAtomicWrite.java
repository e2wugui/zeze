package UnitTest.Zeze.Services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Services.ServiceManager.BServiceInfosVersion;
import Zeze.Services.ServiceManager.ExporterConfig;
import Zeze.Services.ServiceManager.ExporterNginxConfig;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND8-71 回归：重写用Files.writeString(TRUNCATE_EXISTING)截断式覆写唯一真源——
 * 写中途崩溃/宕机/磁盘满时目标停在半截状态（nginx重启即拒绝启动、手写内容不可再生，
 * 后续轮次读残缺文件无法自愈）。
 * 修复：同目录临时文件+move前fsync+原子move（ATOMIC_MOVE失败回退）+finally清理，
 * 复刻GenModule.writeGeneratedFile惯例（FND7-33）。崩溃瞬间的原子性无法在测试中
 * 确定性注入，本用例钉住新写路径的端到端正确性与清理不变量：内容正确（upstream块
 * 重写、手写行保留）、成功与失败路径都不残留*.tmp、目标文件在失败时保持原样。
 */
@Fast
public class TestFnd871ExporterNginxAtomicWrite {

	@Test
	public void testRewriteAtomicAndNoTmpResidue() throws Exception {
		var dir = Files.createTempDirectory("a5-nginx-atomic");
		var cfgFile = dir.resolve("nginx.conf");
		Files.writeString(cfgFile, """
				# handwritten header
				upstream svc {
				    server 1.1.1.1:1;
				}
				# handwritten footer
				""");
		var share = new Properties();
		share.setProperty("-file", cfgFile.toString());
		share.setProperty("-version", "0");
		var exporter = new ExporterNginxConfig(new ExporterConfig(share, null));

		// 重写：新地址写入、旧地址消失、手写行保留
		var all = new BServiceInfosVersion();
		all.getOrAddInfos(0).insert(new BServiceInfo("svc", "2", 0, "2.2.2.2", 2));
		exporter.exportAll("svc", all);
		var out = Files.readString(cfgFile);
		Assertions.assertTrue(out.contains("2.2.2.2:2"), "新地址必须写入: " + out);
		Assertions.assertFalse(out.contains("1.1.1.1:1"), "旧地址不得残留");
		Assertions.assertTrue(out.contains("# handwritten header") && out.contains("# handwritten footer"),
				"手写内容必须保留（不可再生）");

		// 再次重写仍正确（move替换循环可用）
		var all2 = new BServiceInfosVersion();
		all2.getOrAddInfos(0).insert(new BServiceInfo("svc", "3", 0, "3.3.3.3", 3));
		exporter.exportAll("svc", all2);
		out = Files.readString(cfgFile);
		Assertions.assertTrue(out.contains("3.3.3.3:3") && !out.contains("2.2.2.2:2"), "二次重写正确");

		// 清理不变量：成功路径不残留临时文件
		try (var files = Files.list(dir)) {
			var leftovers = files.filter(p -> p.getFileName().toString().endsWith(".tmp")).toList();
			Assertions.assertTrue(leftovers.isEmpty(), "不得残留*.tmp: " + leftovers);
		}

		// 失败路径：目标文件置只读（Windows下替换只读文件必失败）→ exportAll抛异常、
		// 目标保持原样、无临时文件残留。截断式写（修复前）在同场景会在打开/写入阶段
		// 就破坏或改写目标。
		Assertions.assertTrue(cfgFile.toFile().setReadOnly(), "置目标只读（Windows生效）");
		try {
			var all3 = new BServiceInfosVersion();
			all3.getOrAddInfos(0).insert(new BServiceInfo("svc", "4", 0, "4.4.4.4", 4));
			Assertions.assertThrows(Exception.class, () -> exporter.exportAll("svc", all3),
					"目标不可替换时必须失败（不得静默）");
			out = Files.readString(cfgFile);
			Assertions.assertTrue(out.contains("3.3.3.3:3"), "失败时目标文件必须保持原样（截断式写会破坏真源）");
			try (var files = Files.list(dir)) {
				Assertions.assertTrue(files.filter(p -> p.getFileName().toString().endsWith(".tmp")).findAny().isEmpty(),
						"失败路径不得残留*.tmp");
			}
		} finally {
			cfgFile.toFile().setWritable(true); // 恢复以便TempDir清理
		}
	}
}
