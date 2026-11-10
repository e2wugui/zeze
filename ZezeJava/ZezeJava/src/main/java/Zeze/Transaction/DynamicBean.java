package Zeze.Transaction;

import java.util.function.LongFunction;
import java.util.function.ToLongFunction;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Collections.CollOne;
import Zeze.Transaction.Collections.Collection;
import Zeze.Transaction.Collections.LogBean;
import org.jspecify.annotations.Nullable;

/** 动态类型 Bean：运行时按 typeId 持有可替换的内部 bean，替换以 LogDynamic 日志参与事务与序列化。 */
public final class DynamicBean extends Bean implements DynamicBeanReadOnly {
	private static final int VALUE_VARIABLE_ID = 1;

	Bean bean = new EmptyBean();
	long typeId = EmptyBean.TYPEID;
	private transient final ToLongFunction<? super Bean> getBean;
	private transient final LongFunction<@Nullable Bean> createBean;
	private transient @Nullable Object mapKey;
	private transient final @Nullable DynamicField field;

	public DynamicBean(DynamicField field) {
		this(field.id.fieldId(), field.get, field.create, field);
	}

	public DynamicBean(int variableId, ToLongFunction<? super Bean> get, LongFunction<@Nullable Bean> create) {
		this(variableId, get, create, null);
	}

	private DynamicBean(int variableId, ToLongFunction<? super Bean> get, LongFunction<@Nullable Bean> create,
						@Nullable DynamicField field) {
		super(variableId);
		this.field = field;
		getBean = get;
		createBean = create;
	}

	@Nullable DynamicField dynamicField() {
		return field;
	}

	@Override
	public Bean getBean() {
		if (!isManaged())
			return bean;
		var txn = Transaction.getCurrentVerifyRead(this);
		if (txn == null)
			return bean;
		var log = (LogDynamic)txn.getLog(dynamicLogKey());
		var replacement = log != null ? log.value : null;
		return replacement != null ? replacement : bean;
	}

	public void setBean(@Nullable Bean bean) {
		if (bean == null)
			bean = new EmptyBean();
		setBeanWithSpecialTypeId(getBean.applyAsLong(bean), bean);
	}

	static Bean unwrap(Bean bean) {
		if (bean instanceof DynamicBean) // 不允许嵌套放入DynamicBean,否则序列化会输出错误的数据流
			bean = ((DynamicBean)bean).getBean();
		if (bean instanceof Collection) {
			if (bean instanceof CollOne)
				bean = ((CollOne<?>)bean).getValue();
			else
				throw new IllegalStateException("can not set Collection Bean into DynamicBean");
		}
		return bean;
	}

	private void setBeanWithSpecialTypeId(long specialTypeId, Bean bean) {
		bean = unwrap(bean);
		if (!isManaged()) {
			typeId = specialTypeId;
			this.bean = bean;
			return;
		}
		// 先校验宿主和写权限，避免失败时把新值留在受管状态。
		var parent = requireLogParent();
		var txn = Transaction.getCurrentVerifyWrite(this);
		bean.initRootInfoWithRedo(rootInfo, this);
		//noinspection deprecation
		bean.variableId(VALUE_VARIABLE_ID);
		var log = (LogDynamic)txn.logGetOrAdd(dynamicLogKey(parent), () -> new LogDynamic(parent, variableId(), this));
		log.setValue(specialTypeId, bean);
	}

	@Override
	public long getTypeId() {
		if (!isManaged())
			return typeId;
		var txn = Transaction.getCurrentVerifyRead(this);
		if (txn == null)
			return typeId;
		var log = (LogDynamic)txn.getLog(dynamicLogKey());
		return log != null && log.value != null ? log.specialTypeId : typeId;
	}

	long dynamicLogKey() {
		return dynamicLogKey(requireLogParent());
	}

	private long dynamicLogKey(Bean parent) {
		// 同一集合的元素共享宿主和字段号，用自身 objectId 的零号槽避免碰撞。
		return parent instanceof Collection ? objectId() : parent.objectId() + variableId();
	}

	private Bean requireLogParent() {
		var parent = parent();
		if (parent == null)
			throw new IllegalStateException("DynamicBean has no parent for its transaction log");
		return parent;
	}

	@Override
	public long typeId() {
		return getTypeId();
	}

	public LongFunction<@Nullable Bean> getCreateBean() {
		return createBean;
	}

	public Bean newBean(long typeId) {
		var bean = getCreateBean().apply(typeId);
		if (bean == null) {
			if (typeId == EmptyBean.TYPEID)
				bean = new EmptyBean();
			else
				throw new IllegalStateException("incompatible DynamicBean typeId=" + typeId);
		} else if (typeId == EmptyBean.TYPEID && !(bean instanceof EmptyBean))
			typeId = getBean.applyAsLong(bean); // 再确认一下真正的typeId
		setBeanWithSpecialTypeId(typeId, bean);
		return bean;
	}

	public void assign(DynamicBean other) {
		setBean(other.getBean().copy());
	}

	public void assign(DynamicData other) {
		setBean(other.getData().toBean());
	}

	@Override
	public void assign(Data data) {
		assign((DynamicData)data);
	}

	public boolean isEmpty() {
		return getTypeId() == EmptyBean.TYPEID && getBean().getClass() == EmptyBean.class;
	}

	@Override
	public void reset() {
		setBeanWithSpecialTypeId(EmptyBean.TYPEID, new EmptyBean());
	}

	@Override
	public boolean negativeCheck() {
		return getBean().negativeCheck();
	}

	@Override
	public DynamicBean copy() {
		var copy = field != null ? new DynamicBean(field)
			: new DynamicBean(variableId(), getBean, createBean);
		copy.bean = getBean().copy();
		copy.typeId = getTypeId();
		return copy;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteLong(getTypeId());
		getBean().encode(bb);
	}

	@Override
	public void decode(IByteBuffer bb) {
		// 受管值的替换由 setBeanWithSpecialTypeId 写入事务日志。
		var newTypeId = bb.ReadLong();
		var newBean = getCreateBean().apply(newTypeId);
		if (newBean == null) {
			if (ByteBuffer.IGNORE_INCOMPATIBLE_FIELD || newTypeId == EmptyBean.TYPEID) {
				newTypeId = EmptyBean.TYPEID;
				newBean = new EmptyBean();
			} else
				throw new IllegalStateException("incompatible DynamicBean typeId=" + newTypeId);
		} else if (newTypeId == EmptyBean.TYPEID && !(newBean instanceof EmptyBean))
			newTypeId = getBean.applyAsLong(newBean); // 再确认一下真正的typeId
		newBean.decode(bb);
		setBeanWithSpecialTypeId(newTypeId, newBean);
	}

	@Override
	public int preAllocSize() {
		return 9 + getBean().preAllocSize(); // [9]typeId
	}

	@Override
	public void preAllocSize(int size) {
		getBean().preAllocSize(size - 1); // [1]typeId
	}

	@Override
	protected void initChildrenRootInfo(Record.RootInfo root) {
		bean.initRootInfo(root, this);
	}

	@Override
	protected void initChildrenRootInfoWithRedo(Record.RootInfo root) {
		bean.initRootInfoWithRedo(root, this);
	}

	@Override
	public @Nullable Object mapKey() {
		return mapKey;
	}

	@Override
	public void mapKey(Object mapKey) {
		this.mapKey = mapKey;
	}

	@Override
	public void followerApply(Log log) {
		var dLog = (LogDynamicValue)log;
		if (dLog.value != null) {
			dLog.checkField(field);
			typeId = dLog.specialTypeId;
			// 回放与写入使用相同的父 Bean 和内部字段号。
			dLog.value.initRootInfo(rootInfo, this);
			//noinspection deprecation
			dLog.value.variableId(VALUE_VARIABLE_ID);
			bean = dLog.value;
		} else if (dLog.logBean != null)
			bean.followerApply(dLog.logBean);
	}

	@Override
	public LogBean createLogBean(@Nullable TransactionLog finalLog) {
		return finalLog != null ? (LogDynamic)finalLog : new LogDynamic(requireLogParent(), variableId(), this);
	}

	@Override
	public int hashCode() {
		return Long.hashCode(getTypeId()) ^ getBean().hashCode();
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (o == this)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		var that = (DynamicBean)o;
		return getTypeId() == that.getTypeId() && getBean().equals(that.getBean());
	}

	@Override
	public long changeLogKey() {
		return dynamicLogKey();
	}
}
