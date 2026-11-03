package Zeze.Onz;

import java.net.ServerSocket;
import java.nio.file.Path;
import Zeze.Builtin.Onz.BSavedCommits;
import Zeze.Util.RocksDatabase;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestCoordinatorDefaultsToDurableRecords {
	@Test
	void defaultOptionsSyncBothSagaIntentAndCommitDecision(@TempDir Path directory) throws Exception {
		int port;
		try (var socket = new ServerSocket(0)) {
			port = socket.getLocalPort();
		}
		// 纯协调者不创建 Application 缓存目录，无需占用 FastServerIds 的有库号段。
		try (var fixture = GcOnzFastStubSupport.startNonSharedOnzServer(
				60701, port, directory, "durable-records", 60702)) {
			var server = fixture.onzServer;
			assertTrue(server.getWriteOptions().sync(), "默认决策与投递意图须在返回前同步 WAL");
			assertFalse(server.getWriteOptions().disableWAL());
			var key = new byte[8];
			var state = new BSavedCommits.Data();
			state.getOnzs().add(OnzTransaction.encodeSagaParticipant("durable-records"));
			server.saveCommitPoint(key, state, AbstractOnz.ePreparing);
			server.saveCommitPoint(key, state, AbstractOnz.eCommitting);
			server.removeCommitRecord(key);
			assertSame(RocksDatabase.getSyncWriteOptions(), server.getWriteOptions());
		}
	}
}
