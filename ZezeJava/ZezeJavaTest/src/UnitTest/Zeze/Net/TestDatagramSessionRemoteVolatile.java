package UnitTest.Zeze.Net;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import Zeze.Net.DatagramSession;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * N1-F1回归：DatagramSession.remote跨线程可见性。selector线程在processDatagram写（NAT重绑），
 * Send可在任意业务线程读——无happens-before边时应答持续发往失效地址且无任何日志。
 * 修复：remote声明为volatile。并发正确性本身无法确定性观测，以字段修饰符断言守住回归
 * （同文件lastMalformedWarnTime的volatile先例佐证作者意识）。
 */
@Fast
public class TestDatagramSessionRemoteVolatile {

	@Test
	public void testRemoteFieldIsVolatile() throws Exception {
		Field field = DatagramSession.class.getDeclaredField("remote");
		Assertions.assertTrue(Modifier.isVolatile(field.getModifiers()),
				"DatagramSession.remote 必须为 volatile（跨线程读写无happens-before边）");
	}
}
