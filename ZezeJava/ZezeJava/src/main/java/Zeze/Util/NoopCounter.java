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
	public @NotNull LongCounter tableCounter(long tableId, @NotNull TableMetric metric) {
		return noopCounter;
	}

	@Override
	public void addRecvSizeTime(long typeId, Class<?> cls, int size, long timeNs) {
	}

	@Override
	public void addSendSize(long typeId, int size) {
	}
}
