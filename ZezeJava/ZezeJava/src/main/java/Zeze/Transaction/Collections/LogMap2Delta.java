package Zeze.Transaction.Collections;

import java.util.Objects;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.ChangeLogSource;
import Zeze.Transaction.Log;
import org.jspecify.annotations.Nullable;

/** PMap2 的事务结构增量，只参与保存点和提交。 */
final class LogMap2Delta<K, V extends Bean> extends LogMap1<K, V> implements ChangeLogSource {
	LogMap2Delta(@Nullable Bean belong, int varId, @Nullable Bean self,
			org.pcollections.PMap<K, V> value, Meta2<K, V> meta) {
		super(belong, varId, self, value, meta);
	}

	@Override
	public Log beginSavepoint() {
		return new LogMap2Delta<>(getBelong(), getVariableId(), getThis(), getValue(), meta);
	}

	@Override
	protected boolean isValueChanged(@Nullable V oldValue, V newValue) {
		return oldValue != newValue;
	}

	@Override
	public Bean getSourceBean() {
		return Objects.requireNonNull(getThis());
	}

	@Override
	public LogMap2<K, V> createChangeLog() {
		return new LogMap2<>(getBelong(), getVariableId(), getSourceBean(), meta, getReplaced(), getRemoved());
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
