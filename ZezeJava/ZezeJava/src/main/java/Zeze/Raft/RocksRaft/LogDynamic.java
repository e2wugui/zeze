package Zeze.Raft.RocksRaft;

import java.util.Objects;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

/** A dynamic replacement or an edit of the current payload. */
final class LogDynamic extends LogBean {
	static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.LogDynamic");

	private final DynamicField field;
	boolean replaced;
	long specialTypeId;
	@Nullable Bean value;
	@Nullable LogBean edit;

	LogDynamic(DynamicField field) {
		super(TYPE_ID);
		this.field = field;
	}

	@Override
	DynamicField dynamicField() {
		return field;
	}

	void replace(long type, @Nullable Bean bean) {
		replaced = true;
		specialTypeId = type;
		value = bean;
		edit = null;
	}

	@Override
	public Log beginSavepoint() {
		// setBean creates a new replacement log at each write.
		return this;
	}

	@Override
	public void endSavepoint(Savepoint currentSp) {
		currentSp.putLog(this);
	}

	@Override
	public void collect(Changes changes, Bean recent, Log child) {
		boolean first = !replaced && edit == null;
		if (child instanceof LogDynamic replacement && replacement.getBelong() == recent && replacement.replaced)
			replace(replacement.specialTypeId, replacement.value);
		else {
			var current = (DynamicBean)recent;
			var childEdit = (LogBean)child;
			if (replaced || childEdit.getThis() != current.getBean())
				return;
			specialTypeId = current.getSpecialTypeId();
			edit = childEdit;
		}
		if (first)
			changes.collect(recent, this);
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteBool(replaced);
		bb.WriteLong(specialTypeId);
		if (replaced) {
			var replacement = value;
			if (replacement != null)
				replacement.encode(bb);
		} else
			Objects.requireNonNull(edit).encode(bb);
	}

	@Override
	public void decode(IByteBuffer bb) {
		boolean replacement = bb.ReadBool();
		long type = bb.ReadLong();
		if (replacement)
			replace(type, field.readValue(type, bb));
		else {
			if (type == 0)
				throw new IllegalStateException("empty RocksRaft dynamic value cannot be edited");
			var child = new LogBean();
			child.decode(bb);
			replaced = false;
			specialTypeId = type;
			value = null;
			edit = child;
		}
	}
}
