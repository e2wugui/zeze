package Zeze.Transaction;

import java.util.Objects;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Collections.CollOne;
import Zeze.Transaction.Collections.Collection;
import Zeze.Transaction.Collections.LogBean;
import Zeze.Util.Task;
import org.jspecify.annotations.Nullable;

/** DynamicBean change payload, independent of any local transaction target. */
public class LogDynamicValue extends LogBean {
	public static final int TYPE_ID = Bean.hash32("Zeze.Transaction.LogDynamic");

	long specialTypeId;
	@Nullable Bean value;
	@Nullable LogBean logBean;
	private final String parentTypeName;
	private final int factoryVariableId;

	protected LogDynamicValue(int varId, Bean self) {
		super(varId, Objects.requireNonNull(self));
		var dynamic = (DynamicBean)self;
		var parent = dynamic.parent();
		var factoryVarId = dynamic.variableId();
		if (parent instanceof Collection) {
			if (factoryVarId == 0)
				factoryVarId = parent.variableId();
			parent = parent.parent();
		}
		parentTypeName = Objects.requireNonNull(parent).getClass().getName();
		factoryVariableId = factoryVarId;
	}

	private LogDynamicValue(int varId, IByteBuffer bb) {
		super(varId, null);
		parentTypeName = bb.ReadString();
		factoryVariableId = bb.ReadUInt();
		read(bb);
	}

	@Override
	public int getTypeId() {
		return TYPE_ID;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteString(parentTypeName); // use in decode reflect
		bb.WriteUInt(factoryVariableId);
		if (value != null) {
			bb.WriteBool(true);
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
			specialTypeId = bb.ReadLong();
			try {
				var parentType = Class.forName(parentTypeName);
				var factory = parentType.getMethod("createBeanFromSpecialTypeId_" + factoryVariableId, long.class);
				var bean = (Bean)factory.invoke(null, specialTypeId);
				if (bean instanceof DynamicBean)
					bean = ((DynamicBean)bean).getBean();
				if (bean instanceof Collection) {
					if (bean instanceof CollOne)
						bean = ((CollOne<?>)bean).getValue();
					else
						throw new IllegalStateException("can not set Collection Bean into LogDynamic");
				}
				bean.decode(bb);
				value = bean;
			} catch (Exception e) {
				throw Task.forceThrow(e);
			}
		} else if (bb.ReadBool()) { // hasLogBean
			logBean = LogBean.decode(0, bb);
		}
	}

	public static LogDynamicValue decode(int varId, IByteBuffer bb) {
		return new LogDynamicValue(varId, bb);
	}
}
