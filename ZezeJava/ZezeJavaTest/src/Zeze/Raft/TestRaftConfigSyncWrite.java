package Zeze.Raft;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import Zeze.Util.AtomicOutputFile;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RaftConfig.SyncWrite 契约：默认普通写（false）；xml 属性解析、derive 拷贝、save 往返一致。
 * raft 族存储（LogSequence/RocksRaft状态机/Dbh2桶）的写选项唯一来源。
 */
@Fast
public class TestRaftConfigSyncWrite {
	@TempDir
	Path directory;

	private static final String THREE_NODES = """
			<raft Name="127.0.0.1:17650"%s>
			 <node Host="127.0.0.1" Port="17650"/>
			 <node Host="127.0.0.1" Port="17651"/>
			 <node Host="127.0.0.1" Port="17652"/>
			</raft>
			""";

	@Test
	public void defaultIsNormalWrite() {
		assertFalse(RaftConfig.loadFromString(THREE_NODES.formatted("")).isSyncWrite(), "默认必须普通写");
	}

	@Test
	public void parseTrueAndFalse() {
		assertTrue(RaftConfig.loadFromString(THREE_NODES.formatted(" SyncWrite=\"true\"")).isSyncWrite());
		assertFalse(RaftConfig.loadFromString(THREE_NODES.formatted(" SyncWrite=\"false\"")).isSyncWrite());
	}

	@Test
	public void derivePreservesValue() {
		assertTrue(RaftConfig.loadFromString(THREE_NODES.formatted(" SyncWrite=\"true\""))
				.derive("derived-name").isSyncWrite(), "derive必须拷贝SyncWrite");
		assertFalse(RaftConfig.loadFromString(THREE_NODES.formatted(""))
				.derive("derived-name").isSyncWrite());
	}

	@Test
	public void saveRoundTripSurvivesReload() throws Exception {
		var path = directory.resolve("raft.xml");
		AtomicOutputFile.writeAtomically(path, THREE_NODES.formatted(" SyncWrite=\"true\"")
				.getBytes(StandardCharsets.UTF_8));
		var loaded = RaftConfig.load(path.toString());
		assertTrue(loaded.isSyncWrite());
		loaded.save();
		assertTrue(RaftConfig.load(path.toString()).isSyncWrite(), "save必须回写SyncWrite属性");
		// 回滚为默认值也必须持久化（save全量回写语义，对齐TestRaftConfigSaveDefaults）
		loaded.setSyncWrite(false);
		loaded.save();
		assertFalse(RaftConfig.load(path.toString()).isSyncWrite());
	}
}
