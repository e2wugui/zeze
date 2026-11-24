package Zeze.History;

import java.nio.file.Path;
import Zeze.Application;
import Zeze.Builtin.HistoryModule.tHistory;
import Zeze.Config;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Database;
import Zeze.Transaction.Table;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Fast
public class TestApplyDatabaseWaitsForStorage {
	@Test
	void waitsForCursorAndReplayTablesBeforeReadingThem(@TempDir Path home) throws Exception {
		var config = new Config();
		config.setServiceManager("disable");
		config.setNoDatabase(true);
		var business = new Config.DatabaseConf();
		business.setDatabaseUrl(home.toString());
		config.getDatabaseConfMap().put("", business);
		var app = new Application("ApplyDatabaseWaitsForStorage", config);
		var database = mock(Database.class);
		var cursor = mock(Database.AbstractKVTable.class);
		var storage = mock(Database.AbstractKVTable.class);
		var source = new tHistory();
		try {
			app.addTable("", source);
			var sourceDatabase = Table.class.getDeclaredField("database");
			sourceDatabase.setAccessible(true);
			sourceDatabase.set(source, app.getDatabase(""));
			app.getDatabases().put("replay", database);
			when(database.getZeze()).thenReturn(app);
			when(database.openTable("__cursor__", Bean.hash32("__cursor__"))).thenReturn(cursor);
			when(database.openTable(source.getName(), source.getId())).thenReturn(storage);

			var replay = new ApplyDatabaseZeze(app, "replay");
			replay.loadCursor();
			replay.open(source.getName()).get(new byte[] {1}, 0, 1);

			var order = inOrder(cursor, storage);
			order.verify(cursor).waitReady();
			order.verify(cursor).find(any());
			order.verify(storage).waitReady();
			order.verify(storage).find(any());
		} finally {
			app.stop();
		}
	}
}
