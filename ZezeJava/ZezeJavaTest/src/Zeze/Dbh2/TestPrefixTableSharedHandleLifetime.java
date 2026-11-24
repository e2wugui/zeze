package Zeze.Dbh2;

import Zeze.Config;
import Zeze.Dbh2.Master.MasterAgent;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Fast
public class TestPrefixTableSharedHandleLifetime {
	@Test
	void repeatedCloseKeepsSharedTableWhileAnotherPrefixIsOpen() {
		var manager = mock(Dbh2AgentManager.class);
		var master = mock(MasterAgent.class);
		when(manager.openDatabase(anyString(), anyString())).thenReturn(master);
		var conf = new Config.DatabaseConf();
		conf.setDatabaseUrl("dbh2://127.0.0.1:1/prefixLifetime");
		var database = new Database(null, manager, conf);
		try {
			var first = database.openTable("first___shared", 1);
			var second = database.openTable("second___shared", 2);
			try {
				first.close();
				first.close();
				var third = database.openTable("third___shared", 3);
				try {
					verify(manager).createTableAsync(eq(master), anyString(), anyString(), eq("shared"), any());
				} finally {
					third.close();
				}
			} finally {
				second.close();
			}
			var reopened = database.openTable("first___shared", 1);
			try {
				verify(manager, times(2)).createTableAsync(eq(master), anyString(), anyString(), eq("shared"), any());
			} finally {
				reopened.close();
			}
		} finally {
			database.close();
		}
	}
}
