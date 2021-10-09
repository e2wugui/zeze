package Zeze.Transaction;

import java.util.concurrent.atomic.AtomicLong;

import Zeze.*;

/** 
 在Procedure中统计，由于嵌套存储过程存在，总数会比实际事务数多。
 一般嵌套存储过程很少用，事务数量也可以参考这里的数值，不单独统计。
 另外Transaction在重做时会在这里保存重做次数的统计。通过name和存储过程区分开来。
*/
public class ProcedureStatistics {
	private static ProcedureStatistics Instance = new ProcedureStatistics();
	public static ProcedureStatistics getInstance() {
		return Instance;
	}

	private java.util.concurrent.ConcurrentHashMap<String, Statistics> Procedures = new java.util.concurrent.ConcurrentHashMap<String, Statistics> ();
	public final java.util.concurrent.ConcurrentHashMap<String, Statistics> getProcedures() {
		return Procedures;
	}

	public final Statistics GetOrAdd(String procedureName) {
		return getProcedures().computeIfAbsent(procedureName, (key) -> new Statistics());
	}

	public static class Statistics {
		private java.util.concurrent.ConcurrentHashMap<Integer, AtomicLong> Results = new java.util.concurrent.ConcurrentHashMap<Integer, AtomicLong> ();
		public final java.util.concurrent.ConcurrentHashMap<Integer, AtomicLong> getResults() {
			return Results;
		}

		public final AtomicLong GetOrAdd(int result) {
			return getResults().computeIfAbsent(result, (key) -> new AtomicLong());
		}
	}
}