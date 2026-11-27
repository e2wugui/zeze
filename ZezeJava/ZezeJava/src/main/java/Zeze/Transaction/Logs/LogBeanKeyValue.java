package Zeze.Transaction.Logs;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Encodable;
import Zeze.Transaction.Collections.BeanKeyMeta;
import Zeze.Transaction.Log;

/** BeanKey 字段变更值日志，独立于本地事务目标。 */
public class LogBeanKeyValue<T extends Encodable> extends Log {
	private final BeanKeyMeta<T> meta;
	public T value;

	protected LogBeanKeyValue(int varId, BeanKeyMeta<T> meta, T value) {
		super(varId);
		this.meta = meta;
		this.value = value;
	}

	@Override
	public int getTypeId() {
		return meta.logTypeId;
	}

	@Override
	public String getTypeName() {
		return meta.name;
	}

	@Override
	public void encode(ByteBuffer bb) {
		value.encode(bb);
	}

	@Override
	public String toString() {
		return value.toString();
	}

	public static <T extends Encodable> LogBeanKeyValue<T> decode(int varId, BeanKeyMeta<T> meta, IByteBuffer bb) {
		return new LogBeanKeyValue<>(varId, meta, meta.valueDecoder.apply(bb));
	}
}
