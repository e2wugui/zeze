package Zeze.Raft;

import java.nio.file.Files;
import java.nio.file.Path;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Fast
public class TestRaftDumpCommittedSnapshot {
	@Test
	public void selectsCommittedGenerationDespiteNewerCandidate(@TempDir Path directory) throws Exception {
		var committed = Files.createFile(directory.resolve(LogSequence.snapshotFileName + ".10"));
		Files.createFile(directory.resolve(LogSequence.snapshotFileName + ".20"));

		assertEquals(committed, Zeze.Raft.Test.findDumpSnapshot(directory.toString(), 10));
		assertNull(Zeze.Raft.Test.findDumpSnapshot(directory.toString(), -1));
	}
}
