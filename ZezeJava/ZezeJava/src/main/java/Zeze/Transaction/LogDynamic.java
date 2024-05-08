package Zeze.Transaction;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Collections.CollOne;
import Zeze.Transaction.Collections.Collection;
import Zeze.Transaction.Collections.LogBean;
import Zeze.Util.Task;

public class LogDynamic extends LogBean {
	private static final int TYPE_ID = Bean.hash32("Zeze.Transaction.LogDynamic");

	long specialTypeId;
	Bean value;
	LogBean logBean;

	@Override
	public int getTypeId() {
		return TYPE_ID;
	}

	@Override
	public Log beginSavepoint() {
		var dup = new LogDynamic();
		dup.setThis(getThis());
		dup.setBelong(getBelong());
		dup.setVariableId(getVariableId());
		dup.specialTypeId = specialTypeId;
		dup.value = value;
		return dup;
	}

	@Override
	public void endSavepoint(Savepoint currentSp) {
		// 结束保存点，直接覆盖到当前的日志里面即可。
		currentSp.putLog(this);
	}

	// 收集内部的Bean发生了改变。
	@Override
	public void collect(Changes changes, Bean recent, Log vlog) {
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

	void setValue(long typeId, Bean bean) {
		specialTypeId = typeId;
		value = bean;
	}

	@Override
	public void encode(ByteBuffer bb) {
		// encode Value & SpecialTypeId. Value maybe null.
		var self = (DynamicBean)getThis();
		//noinspection DataFlowIssue
		bb.WriteString(self.parent().getClass().getName()); // use in decode reflect
		bb.WriteInt(self.variableId());
		if (null != value) {
			bb.WriteBool(true);
			bb.WriteLong(specialTypeId);
			value.encode(bb);
		} else {
			bb.WriteBool(false); // Value Tag
			if (null != logBean) {
				bb.WriteBool(true);
				logBean.encode(bb);
			} else {
				bb.WriteBool(false);
			}
		}
	}

	@Override
	public void decode(IByteBuffer bb) {
		var parentTypeName = bb.ReadString();
		var varId = bb.ReadInt();
		var hasValue = bb.ReadBool();
		if (hasValue) {
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
		} else {
			var hasLogBean = bb.ReadBool();
			if (hasLogBean) {
				logBean = new LogBean(); // XXX 确认直接可以使用这个类？
				logBean.decode(bb);
			}
		}
	}
}
