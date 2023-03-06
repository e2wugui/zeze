package Zeze.Dbh2;

public class StateMachine extends Zeze.Raft.StateMachine {
	@Override
	public SnapshotResult snapshot(String path) throws Exception {
		return null;
	}

	@Override
	public void loadSnapshot(String path) throws Exception {

	}
}
