package Zeze.Util;

import org.jspecify.annotations.Nullable;

import Zeze.Net.Service;

/**
 * 空实现：禁用统计（-DZezeCounter为空或"null"）时的instance。
 * 所有方法无操作，alloc*返回共享空仪表，调用方无需判空。
 */
public final class NoopCounter implements ZezeCounter {
	public static final NoopCounter instance = new NoopCounter();

	private static final LongCounter noopCounter = v -> {
	};
	private static final LongObserver noopObserver = v -> {
	};
	private static final ProcedureCounter noopProcedureCounter = new ProcedureCounter() {
		@Override
		public void start() {
		}

		@Override
		public void end(long resultCode, long timeNs) {
		}

		@Override
		public void redo() {
		}

		@Override
		public void redoAndReleaseLock() {
		}

		@Override
		public void manyLocks(int count) {
		}
	};
	private NoopCounter() {
	}

	@Override
	public LongCounter allocCounter(String name) {
		return noopCounter;
	}

	@Override
	public LabeledCounterCreator allocLabeledCounterCreator(String name, String... labelNames) {
		return labels -> noopCounter;
	}

	@Override
	public LabeledObserverCreator allocRunTimeObserverCreator(String name,
																	   String... labelNames) {
		return labels -> noopObserver;
	}

	@Override
	public LongObserver getRunTimeObserver(Object key) {
		return noopObserver;
	}

	@Override
	public void serviceStart(Service service) {
	}

	@Override
	public void serviceStop(Service service) {
	}

	@Override
	public ProcedureCounter allocProcedureCounter(String name) {
		return noopProcedureCounter;
	}

	@Override
	public LongCounter tableCounter(long tableId, TableMetric metric) {
		return noopCounter;
	}

	@Override
	public void addRecvSizeTime(long typeId, @Nullable Class<?> cls, int size, long timeNs) {
	}

	@Override
	public void addSendSize(long typeId, int size) {
	}
}
