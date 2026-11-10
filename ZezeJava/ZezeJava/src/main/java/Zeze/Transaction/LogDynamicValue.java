package Zeze.Transaction;

import java.util.Objects;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Collections.LogBean;
import org.jspecify.annotations.Nullable;

/** 可独立解码的 DynamicBean 增量。 */
public class LogDynamicValue extends LogBean {
	public static final int TYPE_ID = Bean.hash32("Zeze.Transaction.LogDynamic");

	long specialTypeId;
	@Nullable Bean value;
	@Nullable LogBean logBean;
	private @Nullable DynamicField field;

	protected LogDynamicValue(int varId, Bean self) {
		super(varId, Objects.requireNonNull(self));
		field = ((DynamicBean)self).dynamicField();
	}

	private LogDynamicValue(int varId, IByteBuffer bb) {
		super(varId, null);
		read(bb);
	}

	@Override
	public int getTypeId() {
		return TYPE_ID;
	}

	@Override
	public void encode(ByteBuffer bb) {
		if (value != null) {
			bb.WriteBool(true);
			var mapping = field;
			if (mapping == null)
				throw new IllegalStateException("dynamic replacement requires a declared field");
			mapping.encodeId(bb);
			bb.WriteLong(specialTypeId);
			value.encode(bb);
		} else {
			bb.WriteBool(false); // Value Tag
			if (logBean != null) {
				bb.WriteBool(true);
				logBean.encode(bb);
			} else
				bb.WriteBool(false);
		}
	}

	private void read(IByteBuffer bb) {
		if (bb.ReadBool()) { // hasValue
			var mapping = LogRegistry.requireDynamicField(DynamicField.Id.decode(bb));
			field = mapping;
			specialTypeId = bb.ReadLong();
			var bean = mapping.create.apply(specialTypeId);
			if (bean == null && specialTypeId == EmptyBean.TYPEID)
				bean = new EmptyBean();
			if (bean == null)
				throw new IllegalStateException("unknown dynamic value " + specialTypeId + " for " + mapping.id);
			bean = DynamicBean.unwrap(bean);
			bean.decode(bb);
			value = bean;
		} else if (bb.ReadBool()) { // hasLogBean
			logBean = LogBean.decode(0, bb);
		}
	}

	void checkField(@Nullable DynamicField target) {
		DynamicField.checkTarget(field, target);
	}

	public static LogDynamicValue decode(int varId, IByteBuffer bb) {
		return new LogDynamicValue(varId, bb);
	}
}
