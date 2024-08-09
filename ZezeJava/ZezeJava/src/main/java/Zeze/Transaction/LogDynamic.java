package Zeze.Transaction;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Collections.CollOne;
import Zeze.Transaction.Collections.Collection;
import Zeze.Transaction.Collections.LogBean;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LogDynamic extends LogBean {
	private static final int TYPE_ID = Bean.hash32("Zeze.Transaction.LogDynamic");

	long specialTypeId;
	@Nullable Bean value;
	@Nullable LogBean logBean;

	public LogDynamic(Bean belong, int varId, Bean self) {
		super(belong, varId, self);
	}

	@Override
	public int getTypeId() {
		return TYPE_ID;
	}

	@Override
	public @NotNull Log beginSavepoint() {
		var dup = new LogDynamic(getBelong(), getVariableId(), getThis());
		dup.specialTypeId = specialTypeId;
		dup.value = value;
		return dup;
	}

	@Override
	public void endSavepoint(@NotNull Savepoint currentSp) {
		// 结束保存点，直接覆盖到当前的日志里面即可。
		currentSp.putLog(this);
	}

	// 收集内部的Bean发生了改变。
	@Override
	public void collect(@NotNull Changes changes, @NotNull Bean recent, @NotNull Log vlog) {
		if (logBean == null) {
			logBean = (LogBean)vlog;
			changes.collect(recent, this);
		}
	}

	@Override
	public void commit() {
		if (value != null) {
			var self = (DynamicBean)getThis();
			self.bean = value;
			self.typeId = specialTypeId;
		}
	}

	void setValue(long typeId, @NotNull Bean bean) {
		specialTypeId = typeId;
		value = bean;
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		// encode Value & SpecialTypeId. Value maybe null.
		var self = (DynamicBean)getThis();
		var parent = self.parent();
		var varId = self.variableId();
		if (parent instanceof Collection) {
			if (varId == 0)
				varId = parent.variableId();
			parent = parent.parent();
		}
		//noinspection DataFlowIssue
		bb.WriteString(parent.getClass().getName()); // use in decode reflect
		bb.WriteUInt(varId);
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

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		var parentTypeName = bb.ReadString();
		var varId = bb.ReadUInt();
		if (bb.ReadBool()) { // hasValue
			specialTypeId = bb.ReadLong();
			try {
				var parentType = Class.forName(parentTypeName);
				var factory = parentType.getMethod("createBeanFromSpecialTypeId_" + varId, long.class);
				var bean = (Bean)factory.invoke(null, new Object[]{specialTypeId});
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
				Task.forceThrow(e);
			}
		} else if (bb.ReadBool()) { // hasLogBean
			logBean = new LogBean(null, 0, null); // XXX 确认直接可以使用这个类？
			logBean.decode(bb);
		}
	}
}
