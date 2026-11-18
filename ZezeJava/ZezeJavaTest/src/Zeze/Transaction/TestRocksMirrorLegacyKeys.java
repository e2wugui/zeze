package Zeze.Transaction;

import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import Zeze.Application;
import Zeze.Builtin.RedoQueue.BTaskId;
import Zeze.Config;
import Zeze.Dbh2.Dbh2Agent;
import Zeze.Dbh2.Dbh2AgentManager;
import Zeze.Dbh2.Master.MasterAgent;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.Action2;
import Zeze.Util.KV;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Test;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Fast
public class TestRocksMirrorLegacyKeys {
	private static final String TABLE = "TestRocksMirrorLegacyKeys_records";

	@Test
	void coldReadBackfillsLegacyKeyButCheckpointRejectsOversizedNewKey() throws Exception {
		var manager = mock(Dbh2AgentManager.class);
		var master = mock(MasterAgent.class);
		var bucket = mock(Dbh2Agent.class);
		when(manager.openDatabase(anyString(), anyString())).thenReturn(master);
		doAnswer(invocation -> {
			Action2<Integer, Boolean> callback = invocation.getArgument(4);
			callback.run(0, true);
			return null;
		}).when(manager).createTableAsync(eq(master), anyString(), anyString(), anyString(), any());
		when(manager.locateBucket(eq(master), anyString(), anyString(), anyString(), any()))
				.thenReturn("bucket");
		when(manager.openBucket("bucket")).thenReturn(bucket);

		var config = new Config();
		config.setServiceManager("disable");
		config.setServerId(FastServerIds.TEST_ROCKS_MIRROR_LEGACY_KEYS);
		config.setDefaultTableConf(new Config.TableConf());
		config.setCheckpointMode(CheckpointMode.Table);
		var defaultDb = new Config.DatabaseConf();
		defaultDb.setDatabaseUrl("rocks_mirror_legacy_keys");
		config.getDatabaseConfMap().put("", defaultDb);
		var tableConf = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
				.createElement("TableConf");
		tableConf.setAttribute("Name", TABLE);
		tableConf.setAttribute("DatabaseName", "dbh2");
		new Config.TableConf(config, tableConf);
		var app = new Application("TestRocksMirrorLegacyKeys", config);
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("dbh2://127.0.0.1:1/mirrorLegacy");
		dbConf.setDisableOperates(true);
		app.getDatabases().put("dbh2", new Zeze.Dbh2.Database(app, manager, dbConf));
		try {
			app.start();
			var table = new TableDynamic<>(app, TABLE, ByteBuffer::Wrap, Binary::new,
					BTaskId::new, false, null, Binary.class, BTaskId.class);
			var checkpoint = requireNonNull(app.getCheckpoint());
			var mirror = table.getLocalRocksCacheTable();

			var legacy = new Binary("x".repeat(Database.eMaxKeyLength + 1));
			var stored = new BTaskId();
			stored.setTaskId(10);
			when(bucket.get("mirrorLegacy", TABLE, legacy)).thenReturn(KV.create(true, ByteBuffer.encode(stored)));
			assertNull(mirror.find(table, legacy));
			assertEquals(10L, requireNonNull(table.selectDirty(legacy)).getTaskId());
			assertEquals(10L, requireNonNull(mirror.find(table, legacy)).getTaskId());

			var tooLong = new Binary(new byte[Database.eMaxKeyLength + 1]);
			var rejectedRecord = dirtyRecord(table, tooLong);
			var failure = assertThrows(RuntimeException.class,
					() -> checkpoint.flush(List.of(rejectedRecord), null, null));
			assertInstanceOf(IllegalArgumentException.class, failure.getCause());
			assertTrue(rejectedRecord.getDirty());
			verify(manager, never()).commit(any());
			assertNull(mirror.find(table, tooLong));

			var maxKey = new Binary(new byte[Database.eMaxKeyLength]);
			var acceptedRecord = dirtyRecord(table, maxKey);
			checkpoint.flush(List.of(acceptedRecord), null, null);
			verify(manager).commit(any());
			assertFalse(acceptedRecord.getDirty());
			assertEquals(20L, requireNonNull(mirror.find(table, maxKey)).getTaskId());
		} finally {
			app.stop();
		}
	}

	private static Record1<Binary, BTaskId> dirtyRecord(TableX<Binary, BTaskId> table, Binary key) {
		var value = new BTaskId();
		value.setTaskId(20);
		var record = new Record1<>(table, key, value);
		record.setDirty(true);
		return record;
	}
}
