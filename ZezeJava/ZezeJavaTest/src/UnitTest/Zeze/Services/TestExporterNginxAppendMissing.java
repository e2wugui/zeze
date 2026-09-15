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

	private static int countOccurrences(String s, String sub) {
		int count = 0;
		for (int i = s.indexOf(sub); i >= 0; i = s.indexOf(sub, i + 1))
			count++;
		return count;
	}
}
