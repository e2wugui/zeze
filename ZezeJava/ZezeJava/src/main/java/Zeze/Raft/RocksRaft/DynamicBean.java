package Zeze.Raft.RocksRaft;

import java.util.Objects;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.HasManagedException;
import org.jspecify.annotations.Nullable;

/** A field-bound wrapper for a replaceable RocksRaft Bean. */
public final class DynamicBean extends Bean {
	private final DynamicField field;
	private @Nullable Bean bean;
	private long specialTypeId;
	private @Nullable Object mapKey;

	DynamicBean(DynamicField field, int variableId) {
		super(variableId);
		this.field = field;
	}

	DynamicField field() {
		return field;
	}

	private @Nullable LogDynamic replacement() {
		var txn = Transaction.getCurrent();
		return txn != null && isManaged() ? (LogDynamic)txn.getLog(objectId()) : null;
	}

	public @Nullable Bean getBean() {
		var log = replacement();
		return log != null ? log.value : bean;
	}

	public long getSpecialTypeId() {
		var log = replacement();
		return log != null ? log.specialTypeId : specialTypeId;
	}

	public boolean isEmpty() {
		return getBean() == null;
	}

	public void clear() {
		set(0, null);
	}

	public void setBean(Bean value) {
		if (value == getBean())
			return;
		if (value instanceof DynamicBean || value instanceof Collection)
			throw new IllegalArgumentException("expected a RocksRaft dynamic payload");
		set(field.identify(value), value);
	}

	private void set(long type, @Nullable Bean value) {
		if (isManaged()) {
			var txn = Objects.requireNonNull(Transaction.getCurrent());
			attach(value, rootInfo());
			var log = new LogDynamic(field);
			log.setBelong(this);
			log.replace(type, value);
			txn.putLog(log);
		} else {
			if (value != null && value.isManaged())
				throw new HasManagedException();
			specialTypeId = type;
			bean = value;
		}
	}

	private void attach(@Nullable Bean value, Record.@Nullable RootInfo root) {
		if (value != null)
			value.initRootInfo(root, this);
	}

	@Override
	protected void initChildrenRootInfo(Record.@Nullable RootInfo root) {
		attach(bean, root);
	}

	public void assign(DynamicBean other) {
		if (other == this)
			return;
		var value = other.getBean();
		if (value == null)
			clear();
		else if (field == other.field)
			set(other.getSpecialTypeId(), value.copy());
		else
			setBean(value.copy());
	}

	@Override
	public DynamicBean copy() {
		var copy = field.newValue();
		copy.assign(this);
		return copy;
	}

	@Override
	public @Nullable Object mapKey() {
		return mapKey;
	}

	@Override
	public void mapKey(Object key) {
		mapKey = key;
	}

	@Override
	public void encode(ByteBuffer bb) {
		var log = replacement();
		var value = log != null ? log.value : bean;
		bb.WriteLong(log != null ? log.specialTypeId : specialTypeId);
		if (value != null)
			value.encode(bb);
		else
			bb.WriteByte(0);
	}

	@Override
	public void decode(IByteBuffer bb) {
		long type = bb.ReadLong();
		var value = field.readValue(type, bb);
		if (value == null && bb.ReadByte() != 0)
			throw new IllegalStateException("invalid empty RocksRaft dynamic value");
		set(type, value);
	}

	public DynamicBean decodeTagged(IByteBuffer bb, int tag) {
		if ((tag & ByteBuffer.TAG_MASK) == ByteBuffer.DYNAMIC)
			decode(bb);
		else
			bb.SkipUnknownFieldOrThrow(tag, "RocksRaft Dynamic");
		return this;
	}

	@Override
	public LogBean createLogBean() {
		var log = new LogDynamic(field);
		log.setBelong(parent());
		log.setThis(this);
		log.setVariableId(variableId());
		return log;
	}

	@Override
	public void followerApply(Log log) {
		var dynamic = (LogDynamic)log;
		DynamicField.checkMatch(field, dynamic.dynamicField());
		if (dynamic.replaced) {
			attach(dynamic.value, rootInfo());
			install(dynamic);
		} else {
			var current = bean;
			if (current == null || specialTypeId != dynamic.specialTypeId)
				throw new IllegalStateException("RocksRaft dynamic edit expected type " + dynamic.specialTypeId
						+ ", found " + specialTypeId);
			current.followerApply(Objects.requireNonNull(dynamic.edit));
		}
	}

	@Override
	public void leaderApplyNoRecursive(Log log) {
		install((LogDynamic)log);
	}

	private void install(LogDynamic log) {
		specialTypeId = log.specialTypeId;
		bean = log.value;
	}

	@Override
	public void buildString(StringBuilder sb, int level) {
		var value = getBean();
		if (value != null)
			value.buildString(sb, level);
		else
			super.buildString(sb, level);
	}
}
