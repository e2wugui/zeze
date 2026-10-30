package Zeze.Services;

import java.nio.file.Files;
import java.util.Properties;
import Zeze.Services.ServiceManager.AbstractAgent;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Services.ServiceManager.BServiceInfos;
import Zeze.Services.ServiceManager.BServiceInfosVersion;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import Zeze.Services.ServiceManager.ExporterConfig;
import Zeze.Services.ServiceManager.ExporterNginxConfig;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestServiceDiscoverySnapshotIsolation {
	@Test
	void capturedVersionBucketsKeepTheirOriginalRegistrations() {
		var state = new AbstractAgent.SubscribeState(new BSubscribeInfo("service"));
		var original = new BServiceInfo("service", "1", 0, "127.0.0.1", 1234);
		state.onRegister(original);
		var snapshot = state.getServiceInfosVersion();
		state.onRegister(new BServiceInfo("service", "1", 0, "127.0.0.1", 5678));
		assertSame(original, snapshot.getInfos(0).findServiceInfoByIdentity("1"));
		state.onUnRegister(original);
		assertSame(original, snapshot.getInfos(0).findServiceInfoByIdentity("1"));
		assertNull(state.getServiceInfos(0));
	}

	@Test
	void unregisterBetweenNginxValidationAndRenderingCannotEmptyCapturedBucket() throws Exception {
		var state = new AbstractAgent.SubscribeState(new BSubscribeInfo("service"));
		var original = new BServiceInfo("service", "1", 0, "127.0.0.1", 1234);
		state.onRegister(original);
		var snapshot = new BServiceInfosVersion(state.getServiceInfosVersion()) {
			private int reads;
			@Override
			public BServiceInfos getInfos(long version) {
				if (++reads == 2)
					state.onUnRegister(original); // 校验已经通过，实际生成行之前完成一次注销。
				return super.getInfos(version);
			}
		};
		var file = Files.createTempFile("nginx-snapshot", ".conf");
		try {
			Files.writeString(file, "upstream service { server 127.0.0.1:1234; }\n");
			var properties = new Properties();
			properties.setProperty("-file", file.toString());
			new ExporterNginxConfig(new ExporterConfig(properties, null)).exportAll("service", snapshot);
			assertTrue(Files.readString(file).contains("server 127.0.0.1:1234;"),
					"同一次导出的已校验桶不能在生成时变成nginx非法空upstream");
			assertNull(state.getServiceInfos(0), "注销必须确实发生，不靠忽略状态更新通过");
		} finally {
			Files.deleteIfExists(file);
		}
	}
}
