package Zeze.Util;

import Zeze.Net.Service;
import org.jetbrains.annotations.NotNull;

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
	private static final TableCounter noopTableCounter = new TableCounter() {
		@Override
		public @NotNull LongCounter cacheGet() {
			return noopCounter;
		}

		@Override
		public @NotNull LongCounter storageGet() {
			return noopCounter;
		}

		@Override
		public @NotNull LongCounter readLock() {
			return noopCounter;
		}

		@Override
		public @NotNull LongCounter writeLock() {
			return noopCounter;
		}

		@Override
		public @NotNull LongCounter tryReadLock() {
			return noopCounter;
		}

		@Override
		public @NotNull LongCounter tryWriteLock() {
			return noopCounter;
		}

		@Override
		public @NotNull LongCounter acquireShare() {
			return noopCounter;
		}

		@Override
		public @NotNull LongCounter acquireModify() {
			return noopCounter;
		}

		@Override
		public @NotNull LongCounter acquireInvalid() {
			return noopCounter;
		}

		@Override
		public @NotNull LongCounter reduceInvalid() {
			return noopCounter;
		}

		@Override
		public @NotNull LongCounter redo() {
			return noopCounter;
		}
	};

	private NoopCounter() {
	}

	@Override
	public @NotNull LongCounter allocCounter(@NotNull String name) {
		return noopCounter;
	}

	@Override
	public @NotNull LabeledCounterCreator allocLabeledCounterCreator(@NotNull String name, @NotNull String... labelNames) {
		return labels -> noopCounter;
	}

	@Override
	public @NotNull LabeledObserverCreator allocRunTimeObserverCreator(@NotNull String name,
																	   @NotNull String... labelNames) {
		return labels -> noopObserver;
	}

	@Override
	public @NotNull LongObserver getRunTimeObserver(@NotNull Object key) {
		return noopObserver;
	}

	@Override
	public void addTaskRunTime(@NotNull Object key, long timeNs) {
	}

	@Override
	public void serviceStart(@NotNull Service service) {
	}

	@Override
	public void serviceStop(@NotNull Service service) {
	}

	@Override
	public void procedureStart(@NotNull String name) {
	}

	@Override
	public void procedureEnd(@NotNull String name, long resultCode, long timeNs) {
	}

	@Override
	public void procedureRedo(@NotNull String name) {
	}

	@Override
	public void procedureRedoAndReleaseLock(@NotNull String name) {
	}

	@Override
	public @NotNull TableCounter getOrAddTableInfo(long tableId) {
		return noopTableCounter;
	}

	@Override
	public void addRecvSizeTime(long typeId, Class<?> cls, int size, long timeNs) {
	}

	@Override
	public void addSendSize(long typeId, int size) {
	}

	@Override
	public void addSendSize(byte @NotNull [] bytes, int offset, int length) {
	}
}
