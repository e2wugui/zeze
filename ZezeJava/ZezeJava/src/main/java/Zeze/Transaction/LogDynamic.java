package Zeze.Transaction;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Collections.LogBean;

public class LogDynamic extends LogBean {
	public long specialTypeId;
	public Bean value;
	public LogBean logBean;

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

	public void setValue(Bean bean) {
		value = bean;
		var self = (DynamicBean)getThis();
		specialTypeId = self.getSpecialTypeIdFromBean.applyAsLong(bean);
	}

	@Override
	public void encode(ByteBuffer bb) {
		// encode Value & SpecialTypeId. Value maybe null.
		var self = (DynamicBean)getThis();
		bb.WriteString(self.parent().getClass().getName()); // use in decode reflect
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
	public void decode(ByteBuffer bb)
	{
		var parentTypeName = bb.ReadString();
		var hasValue = bb.ReadBool();
		if (hasValue)
		{
			specialTypeId = bb.ReadLong();
			try {
				var parentType = Class.forName(parentTypeName);
				var factory = parentType.getMethod("CreateBeanFromSpecialTypeId", Long.class);
				value = (Bean)factory.invoke(null, new Object[]{specialTypeId});
				value.decode(bb);
			} catch (Exception ex) {
				throw new RuntimeException(ex);
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
