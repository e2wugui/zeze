package UnitTest.Zeze.Misc;

import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Services.ServiceManager.BServiceInfos;
import junit.framework.TestCase;

public class TestServiceManager extends TestCase {
	public void testServiceInfos() {
		var infos = new BServiceInfos("TestBase");
		infos.Insert(new BServiceInfo("TestBase", "1"));
		infos.Insert(new BServiceInfo("TestBase", "3"));
		infos.Insert(new BServiceInfo("TestBase", "2"));
		assertEquals("TestBase Version=0[1,2,3,]", infos.toString());
	}
}
