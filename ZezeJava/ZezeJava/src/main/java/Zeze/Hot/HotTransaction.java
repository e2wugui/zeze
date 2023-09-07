package Zeze.Hot;

import java.util.ArrayList;
import Zeze.Util.Action0;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HotTransaction {
	private static final Logger logger = LogManager.getLogger(HotTransaction.class);
	private final String name;
	private final ArrayList<Action0> rollbacks = new ArrayList<>();
	private final ArrayList<Action0> commits = new ArrayList<>();

	public HotTransaction(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void whileRollback(Action0 action) {
		rollbacks.add(action);
	}

	public void whileCommit(Action0 action) {
		commits.add(action);
	}

	public void commit() throws Exception {
		for (var commit : commits)
			commit.run();
		commits.clear();
		rollbacks.clear();
	}

	public void rollback() {
		for (var i = rollbacks.size() - 1; i >= 0; --i) {
			try {
				rollbacks.get(i).run();
			} catch (Exception ex) {
				logger.error(name, ex);
			}
		}
		rollbacks.clear();
	}
}
