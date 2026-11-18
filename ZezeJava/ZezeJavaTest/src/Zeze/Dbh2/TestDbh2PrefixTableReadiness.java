package Zeze.Dbh2;

import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Config;
import Zeze.Dbh2.Master.MasterAgent;
import Zeze.Transaction.Procedure;
import Zeze.Util.Action2;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Fast
public class TestDbh2PrefixTableReadiness {
	@Test
	void sharedCreationFailureReachesBothPrefixesAndAllowsReopening() throws Exception {
		var manager = mock(Dbh2AgentManager.class);
		var master = mock(MasterAgent.class);
		when(manager.openDatabase(anyString(), anyString())).thenReturn(master);
		var resultCode = new AtomicInteger((int)Procedure.Closed);
		doAnswer(invocation -> {
			Action2<Integer, Boolean> callback = invocation.getArgument(4);
			var code = resultCode.get();
			callback.run(code, code == 0);
			return null;
		}).when(manager).createTableAsync(eq(master), anyString(), anyString(), eq("shared"), any());
		var conf = new Config.DatabaseConf();
		conf.setDatabaseUrl("dbh2://127.0.0.1:1/prefixReadiness");
		var database = new Database(null, manager, conf);
		try {
			var first = database.openTable("first___shared", 1);
			var second = database.openTable("second___shared", 2);
			try {
				var failure = assertThrows(RuntimeException.class, first::waitReady);
				assertTrue(failure.getMessage().contains("rc=" + Procedure.Closed));
				assertThrows(RuntimeException.class, second::waitReady);
				verify(manager).createTableAsync(eq(master), anyString(), anyString(), eq("shared"), any());
			} finally {
				second.close();
				first.close();
			}

			resultCode.set(0);
			var reopened = database.openTable("first___shared", 1);
			try {
				assertDoesNotThrow(reopened::waitReady);
				verify(manager, times(2))
						.createTableAsync(eq(master), anyString(), anyString(), eq("shared"), any());
			} finally {
				reopened.close();
			}
		} finally {
			database.close();
		}
	}
}
