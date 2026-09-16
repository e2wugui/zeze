package UnitTest.Zeze.Services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Services.ServiceManager.BServiceInfosVersion;
import Zeze.Services.ServiceManager.ExporterConfig;
import Zeze.Services.ServiceManager.ExporterNginxConfig;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * FND6-30：配置文件中不存在同名upstream块时exportAll原实现恒不更新（hasChanged恒false）
 * ——新增服务或首次部署未预置空块时该服务地址永不进nginx、无自愈无告警（未文档化的
 * 隐含契约）。修复：整文件未命中时在文件尾追加自产格式块，后续轮转可正常识别重写。
 */
@Fast
public class TestExporterNginxAppendMissing {

	@Test
	public void testMissingBlockAppendedAndRewrittenNextRound() throws Exception {
		var dir = Files.createTempDirectory("nginx_export_fnd6_30");
		var cfgFile = dir.resolve("nginx.conf");
		Files.writeString(cfgFile, """
				upstream other {
				    server 9.9.9.9:9;
				}
				""");
		var share = new Properties();
		share.setProperty("-file", cfgFile.toString());
		share.setProperty("-version", "0");
		var exporter = new ExporterNginxConfig(new ExporterConfig(share, null));

		var all = new BServiceInfosVersion();
		all.getOrAddInfos(0).insert(new BServiceInfo("newsvc", "2", 0, "2.2.2.2", 2));
		exporter.exportAll("newsvc", all);

		var out = Files.readString(cfgFile);
		Assertions.assertTrue(out.contains("upstream newsvc {"), "缺块必须追加（FND6-30）: " + out);
		Assertions.assertTrue(out.contains("2.2.2.2:2"), "新服务地址必须写入");
		Assertions.assertTrue(out.contains("9.9.9.9:9"), "无关块不动");
		Assertions.assertTrue(out.indexOf("upstream newsvc {") > out.indexOf("upstream other {"),
				"追加发生在文件尾");

		// 第二轮：服务地址变化——已追加的块被识别并原位重写（不再重复追加）。
		var all2 = new BServiceInfosVersion();
		all2.getOrAddInfos(0).insert(new BServiceInfo("newsvc", "3", 0, "3.3.3.3", 3));
		exporter.exportAll("newsvc", all2);
		var out2 = Files.readString(cfgFile);
		Assertions.assertFalse(out2.contains("2.2.2.2:2"), "旧地址不得残留");
		Assertions.assertTrue(out2.contains("3.3.3.3:3"), "新地址必须写入");
		Assertions.assertEquals(1, countOccurrences(out2, "upstream newsvc {"), "块不得重复追加");
	}

	@Test
	public void testEmptyFileAppends() throws Exception {
		var dir = Files.createTempDirectory("nginx_export_fnd6_30b");
		var cfgFile = dir.resolve("nginx.conf");
		Files.writeString(cfgFile, "");
		var share = new Properties();
		share.setProperty("-file", cfgFile.toString());
		share.setProperty("-version", "0");
		var exporter = new ExporterNginxConfig(new ExporterConfig(share, null));

		var all = new BServiceInfosVersion();
		all.getOrAddInfos(0).insert(new BServiceInfo("svc", "1", 0, "1.1.1.1", 1));
		exporter.exportAll("svc", all);
		Assertions.assertTrue(Files.readString(cfgFile).contains("1.1.1.1:1"), "空文件同样须追加");
	}

	@Test
	public void testNoIdentitiesSkipsWriteAndReload() throws Exception {
		var dir = Files.createTempDirectory("nginx_export_fnd6_30c");
		var cfgFile = dir.resolve("nginx.conf");
		Files.writeString(cfgFile, "");
		var share = new Properties();
		share.setProperty("-file", cfgFile.toString());
		share.setProperty("-version", "0");
		var exporter = new ExporterNginxConfig(new ExporterConfig(share, null));

		// 无任何identity：不得产出空upstream块（nginx reload报[emerg] no servers are
		// inside upstream，整个reload失败波及同文件其他服务）。
		exporter.exportAll("svc", new BServiceInfosVersion());
		Assertions.assertEquals(0, Files.size(cfgFile), "无identity必须跳过写盘");

		// 有identity但passiveIp全空（全部实例下线态）：同样跳过。
		var blankIp = new BServiceInfosVersion();
		blankIp.getOrAddInfos(0).insert(new BServiceInfo("svc", "1", 0, " ", 1));
		exporter.exportAll("svc", blankIp);
		Assertions.assertEquals(0, Files.size(cfgFile), "passiveIp全空必须跳过写盘");
	}

	@Test
	public void testExistingBlockAllOfflineKeepsOriginalBlock() throws Exception {
		var dir = Files.createTempDirectory("nginx_export_fnd6_30d");
		var cfgFile = dir.resolve("nginx.conf");
		Files.writeString(cfgFile, """
				upstream svc {
				    server 1.1.1.1:1;
				}
				""");
		var share = new Properties();
		share.setProperty("-file", cfgFile.toString());
		share.setProperty("-version", "0");
		var exporter = new ExporterNginxConfig(new ExporterConfig(share, null));

		// 服务地址全部下线（无infos）：保持原块不动（重写成空块会让nginx reload emerg
		// 失败；旧地址由nginx自行502，实例重新上线后恢复重写）。
		exporter.exportAll("svc", new BServiceInfosVersion());
		var out = Files.readString(cfgFile);
		Assertions.assertEquals(1, countOccurrences(out, "upstream svc {"), "原块保持");
		Assertions.assertTrue(out.contains("1.1.1.1:1"), "下线态旧地址保留在原块中");

		// 重新上线后恢复重写。
		var online = new BServiceInfosVersion();
		online.getOrAddInfos(0).insert(new BServiceInfo("svc", "2", 0, "2.2.2.2", 2));
		exporter.exportAll("svc", online);
		var out2 = Files.readString(cfgFile);
		Assertions.assertFalse(out2.contains("1.1.1.1:1"), "重新上线后旧地址被重写掉");
		Assertions.assertTrue(out2.contains("2.2.2.2:2"), "新地址写入");
		Assertions.assertEquals(1, countOccurrences(out2, "upstream svc {"), "块不重复");
	}

	@Test
	public void testBomFirstLineRecognizedNotDuplicated() throws Exception {
		var dir = Files.createTempDirectory("nginx_export_fnd6_30e");
		var cfgFile = dir.resolve("nginx.conf");
		Files.writeString(cfgFile, """
				\uFEFFupstream svc {
				    server 1.1.1.1:1;
				}
				""");
		var share = new Properties();
		share.setProperty("-file", cfgFile.toString());
		share.setProperty("-version", "0");
		var exporter = new ExporterNginxConfig(new ExporterConfig(share, null));

		// BOM文件首行同名块必须被识别原位重写——不识别则误判未命中在文件尾追加重复块，
		// nginx报upstream duplicate emerg。
		var all = new BServiceInfosVersion();
		all.getOrAddInfos(0).insert(new BServiceInfo("svc", "2", 0, "2.2.2.2", 2));
		exporter.exportAll("svc", all);
		var out = Files.readString(cfgFile);
		Assertions.assertEquals(1, countOccurrences(out, "upstream svc {"), "BOM首行块不得重复追加");
		Assertions.assertTrue(out.contains("2.2.2.2:2"), "新地址写入");
		Assertions.assertFalse(out.contains("1.1.1.1:1"), "旧地址重写掉");
	}

	@Test
	public void testReloadCommandWithArgsCompletes() throws Exception {
		// FND6-30补钉桩：reload命令带参数（如"nginx -s reload"形态）。ProcessBuilder不像
		// Runtime.exec(String)按空白切分——漏split会把整串当可执行名，命令必然start失败。
		// 用"java -version"（带参、往stderr写输出）钉住：切分正确则正常完成不挂起，
		// 输出经DISCARD丢弃不填管道。java不在PATH的环境跳过。
		try {
			new ProcessBuilder("java", "-version").start().destroyForcibly();
		} catch (IOException e) {
			Assumptions.assumeTrue(false, "环境无java命令，跳过");
		}

		var dir = Files.createTempDirectory("nginx_export_fnd6_30f");
		var cfgFile = dir.resolve("nginx.conf");
		Files.writeString(cfgFile, "");
		var share = new Properties();
		share.setProperty("-file", cfgFile.toString());
		share.setProperty("-version", "0");
		share.setProperty("-reload", "java -version");
		var exporter = new ExporterNginxConfig(new ExporterConfig(share, null));

		var all = new BServiceInfosVersion();
		all.getOrAddInfos(0).insert(new BServiceInfo("svc", "1", 0, "1.1.1.1", 1));
		exporter.exportAll("svc", all); // 内含reload：必须正常返回（不抛、不挂起）
		Assertions.assertTrue(Files.readString(cfgFile).contains("1.1.1.1:1"), "reload异常不得阻断导出");
	}

	private static int countOccurrences(String s, String sub) {
		int count = 0;
		for (int i = s.indexOf(sub); i >= 0; i = s.indexOf(sub, i + 1))
			count++;
		return count;
	}
}
