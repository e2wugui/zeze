package UnitTest.Zeze.Misc;

import Zeze.Services.ServiceManager.ServiceInfo;
import Zeze.Services.ServiceManager.ServiceInfos;
import junit.framework.TestCase;

public class TestServiceManager extends TestCase {
	public void testServiceInfos() {
		var infos = new ServiceInfos("TestBase");
		infos.Insert(new ServiceInfo("TestBase", "1"));
		infos.Insert(new ServiceInfo("TestBase", "3"));
		infos.Insert(new ServiceInfo("TestBase", "2"));
		assertEquals("TestBase Version=0[1,2,3,]", infos.toString());
	}
}
