package Zeze.Net;

import java.net.InetSocketAddress;
import Zeze.Util.ReplayAttackPolicy;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestDatagramSessionIdentity {
	@Test
	void closingAnOldSessionDoesNotRemoveItsReplacement() throws Exception {
		Task.tryInitThreadPool();
		var service = new Service("TestDatagramSessionIdentity.replacement");
		try {
			var socket = service.bindUdp(new InetSocketAddress(0));
			var remote = new InetSocketAddress("127.0.0.1", 1);
			var original = socket.createSession(remote, 27, null, ReplayAttackPolicy.AllowDisorder);
			assertNotNull(original);
			socket.removeSession(original);
			var replacement = socket.createSession(remote, 27, null, ReplayAttackPolicy.AllowDisorder);
			assertNotNull(replacement);
			try {
				assertFalse(socket.containsSession(original));
				original.close();
				assertTrue(socket.containsSession(replacement));
				assertFalse(replacement.isClosed());
			} finally {
				original.close();
				replacement.close();
			}
		} finally {
			service.stop();
		}
	}

	@Test
	void foreignSessionWithTheSameTokenCannotRemoveTheLocalOne() throws Exception {
		Task.tryInitThreadPool();
		var service = new Service("TestDatagramSessionIdentity.foreign");
		try {
			var socket = service.bindUdp(new InetSocketAddress(0));
			var other = service.bindUdp(new InetSocketAddress(0));
			var remote = new InetSocketAddress("127.0.0.1", 1);
			var localSession = socket.createSession(remote, 28, null, ReplayAttackPolicy.AllowDisorder);
			var foreignSession = other.createSession(remote, 28, null, ReplayAttackPolicy.AllowDisorder);
			assertNotNull(localSession);
			assertNotNull(foreignSession);
			socket.removeSession(foreignSession);
			assertTrue(socket.containsSession(localSession));
			assertFalse(socket.containsSession(foreignSession));
		} finally {
			service.stop();
		}
	}
}
