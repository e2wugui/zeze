package Zeze.Services;

import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Services.ServiceManager.BServiceInfos;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Fast
public class TestServiceSnapshotIdentityOrder {
	@Test
	public void decodedLegacyOrderSupportsLookupAndRemoval() {
		var output = ByteBuffer.Allocate();
		var numeric = new BServiceInfo("service", "\uff12");
		var named = new BServiceInfo("service", "@node");
		output.WriteUInt(2);
		named.encode(output); // 旧比较器遇到 @ 时按字符串排序。
		numeric.encode(output);

		var infos = new BServiceInfos();
		infos.decode(output);
		assertNotNull(infos.findServiceInfo(numeric));
		assertNotNull(infos.remove(named));
	}
}
