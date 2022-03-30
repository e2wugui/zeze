package UnitTest.Zeze.Misc;

import junit.framework.TestCase;
import Zeze.Services.ServiceManager.*;

public class TestServiceManager extends TestCase {
    public void testServiceInfos() {
        var infos = new ServiceInfos("TestBase");
        infos.Insert(new ServiceInfo("TestBase", "1"));
        infos.Insert(new ServiceInfo("TestBase", "3"));
        infos.Insert(new ServiceInfo("TestBase", "2"));
        assert "TestBase=[1,2,3,]".equals(infos.toString());
    }
}
