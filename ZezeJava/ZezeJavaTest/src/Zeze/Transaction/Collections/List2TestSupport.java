package Zeze.Transaction.Collections;

import java.util.List;

import Zeze.Transaction.Bean;

/** Structural wire fixtures use the public OpLog values, rather than mutating a payload as a transaction. */
public final class List2TestSupport {
	private List2TestSupport() {
	}

	public static <V extends Bean> List<LogList1.OpLog<V>> addOperation(int index, V value) {
		return List.of(new LogList1.OpLog<>(LogList1.OpLog.OP_ADD, index, value));
	}

	public static <V extends Bean> List<LogList1.OpLog<V>> clearOperation() {
		return List.of(new LogList1.OpLog<>(LogList1.OpLog.OP_CLEAR, 0, null));
	}
}
