package Zeze.Util;

import java.util.Properties;
import Zeze.Services.ServiceManager.ExporterConfig;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
public class TestPropertiesHelperEmptyOptions {

	@Test
	public void emptyAndWhitespaceOptionsProduceNoProperties() {
		for (var options : new String[]{"", " ", "\t\r\n "})
			assertTrue(PropertiesHelper.parse(options).isEmpty(), () -> "options=" + options);
	}

	@Test
	public void emptyPrivateOptionsKeepSharedExporterSettings() {
		var shared = new Properties();
		shared.setProperty("-file", "nginx.conf");
		shared.setProperty("-version", "12");
		for (var options : new String[]{"", " \t\n"}) {
			var config = new ExporterConfig(shared, options);
			assertEquals("nginx.conf", config.getFile());
			assertEquals(12, config.getVersion());
		}
	}
}
