package Zeze.Transaction.Collections;

import java.util.Objects;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.ChangeLogSource;
import Zeze.Transaction.Log;
import org.jspecify.annotations.Nullable;

/** PSortedMap2 的事务结构增量，只参与保存点和提交，不承担消费日志的编解码。 */
final class LogSortedMap2Delta<K extends Comparable<K>, V extends Bean> extends LogSortedMap1<K, V>
		implements ChangeLogSource {
	LogSortedMap2Delta(@Nullable Bean belong, int varId, @Nullable Bean self,
			org.pcollections.PSortedMap<K, V> value, Meta2<K, V> meta) {
		super(belong, varId, self, value, meta);
	}

	@Override
	public Log beginSavepoint() {
		return new LogSortedMap2Delta<>(getBelong(), getVariableId(), getThis(), getValue(), meta);
	}

	@Override
	protected boolean isValueChanged(@Nullable V oldValue, V newValue) {
		// Bean 值可变；与当前值 equals 相等的新实例仍必须作为替换重放。
		return oldValue != newValue;
	}

	@Override
	public Bean getSourceBean() {
		return Objects.requireNonNull(getThis());
	}

	@Override
	public LogSortedMap2<K, V> createChangeLog() {
		return new LogSortedMap2<>(getBelong(), getVariableId(), getSourceBean(), getValue().comparator(), meta,
				getReplaced(), getRemoved());
	}

	@Override
	public void encode(ByteBuffer bb) {
		throw new UnsupportedOperationException("transaction delta must be collected before encoding");
	}

	@Override
	public void decode(IByteBuffer bb) {
		throw new UnsupportedOperationException("transaction delta cannot decode");
	}
}
