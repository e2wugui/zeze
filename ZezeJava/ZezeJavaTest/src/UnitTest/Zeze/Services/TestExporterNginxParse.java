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
 * FND5-36 回归：ExporterNginxConfig用split(" ")[1]解析upstream行——nginx
 * 合法写法"upstream svc{"（名字后无空格直接花括号）解析出"svc{"≠服务名，
 * 该块永不重写：已下线服务的server行永久残留并随reload生效；"upstream{"
 * 一类行split[1]抛AIOOBE被triggerOnChanged捕获，同批其余服务导出一并丢失。
 * 修复：按空白正则切分取第二token去尾'{'；解析不出名字记告警跳过（不抛）。
 * 本工具自产格式"upstream svc {"不受影响。
 */
@Fast
public class TestExporterNginxParse {

	@Test
	public void testUpstreamNoSpaceBeforeBraceRewritten() throws Exception {
		var dir = Files.createTempDirectory("nginx_export_fnd5_36");
		var cfgFile = dir.resolve("nginx.conf");
		Files.writeString(cfgFile, """
				upstream demo {
				    server 9.9.9.9:9;
				}
				upstream svc1{
				    server 1.1.1.1:1;
				}
				""");
		var share = new Properties();
		share.setProperty("-file", cfgFile.toString());
		share.setProperty("-version", "0");
		var exporter = new ExporterNginxConfig(new ExporterConfig(share, null));

		var all = new BServiceInfosVersion();
		all.getOrAddInfos(0).insert(new BServiceInfo("svc1", "2", 0, "2.2.2.2", 2));
		Assertions.assertDoesNotThrow(() -> exporter.exportAll("svc1", all), "解析不得抛异常");

		var out = Files.readString(cfgFile);
		Assertions.assertTrue(out.contains("2.2.2.2:2"), "新地址必须写入: " + out);
		Assertions.assertFalse(out.contains("1.1.1.1:1"),
				"“upstream svc1{”块必须被识别重写，下线地址不得残留（FND5-36）");
		Assertions.assertTrue(out.contains("9.9.9.9:9"), "无关块不动");
	}

	@Test
	public void testTabSeparatedAndSelfProducedFormat() throws Exception {
		var dir = Files.createTempDirectory("nginx_export_fnd5_36b");
		var cfgFile = dir.resolve("nginx.conf");
		Files.writeString(cfgFile, "upstream\t\tsvc2 {\n    server 1.1.1.1:1;\n}\n");
		var share = new Properties();
		share.setProperty("-file", cfgFile.toString());
		share.setProperty("-version", "0");
		var exporter = new ExporterNginxConfig(new ExporterConfig(share, null));

		var all = new BServiceInfosVersion();
		all.getOrAddInfos(0).insert(new BServiceInfo("svc2", "3", 0, "3.3.3.3", 3));
		Assertions.assertDoesNotThrow(() -> exporter.exportAll("svc2", all), "tab分隔写法不得抛异常");

		var out = Files.readString(cfgFile);
		Assertions.assertTrue(out.contains("3.3.3.3:3"), "tab写法块必须重写: " + out);
		Assertions.assertFalse(out.contains("1.1.1.1:1"), "下线地址不得残留");
	}
}
