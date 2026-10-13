package Zeze.Services.ZokerImpl;

import harness.Extra;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Util.TimeThrottle;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

@Fast
@Extra
public class TestDisconnectedUploaderKeepsReopenedFile {
	@Test
	public void closingAnOldOwnerKeepsTheNewUpload(@TempDir Path temp) throws Exception {
		var manager = new DistributeManager(temp.resolve("distributes").toFile(),
				temp.resolve("services").toFile());
		var firstOwner = new StubSocket();
		var oldOwner = new StubSocket();
		var newOwner = new StubSocket();
		try {
			var first = manager.open("svc", "file", firstOwner);
			assertSame(first, manager.open("svc", "file", oldOwner));
			first.append(0, new Binary(new byte[]{1}));
			assertEquals(0, manager.closeAndVerify("svc", "file",
					new Binary(MessageDigest.getInstance("MD5").digest(new byte[]{1})), firstOwner));
			var reopened = manager.open("svc", "file", newOwner);
			manager.closeBySocket(oldOwner);
			manager.append("svc", "file", 1, new Binary(new byte[]{2}));
			assertArrayEquals(new byte[]{1, 2}, Files.readAllBytes(reopened.getCanonicalFile().toPath()));
		} finally {
			manager.closeAll();
			firstOwner.close();
			oldOwner.close();
			newOwner.close();
		}
	}

	private static final class StubSocket extends AsyncSocket {
		StubSocket() { super(null); }
		@Override public Type getType() { return Type.eClient; }
		@Override public @Nullable SocketAddress getRemoteAddress() { return null; }
		@Override public @Nullable TimeThrottle getTimeThrottle() { return null; }
		@Override protected void doClose(@Nullable Throwable ex, boolean gracefully) { }
		@Override public boolean Send(byte @NotNull [] bytes, int offset, int length) { return true; }
	}
}
