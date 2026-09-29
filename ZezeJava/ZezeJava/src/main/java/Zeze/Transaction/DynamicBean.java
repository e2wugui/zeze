package Zeze.Transaction;

import java.util.function.LongFunction;
import java.util.function.ToLongFunction;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Collections.CollOne;
import Zeze.Transaction.Collections.Collection;
import Zeze.Transaction.Collections.LogBean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** 动态类型 Bean：运行时按 typeId 持有可替换的内部 bean，替换以 LogDynamic 日志参与事务与序列化。 */
public final class DynamicBean extends Bean implements DynamicBeanReadOnly {
	@NotNull Bean bean = new EmptyBean();
	long typeId = EmptyBean.TYPEID;
	private transient final @NotNull ToLongFunction<Bean> getBean;
	private transient final @NotNull LongFunction<Bean> createBean;
	private transient Object mapKey;

	public DynamicBean(int variableId, @NotNull ToLongFunction<Bean> get, @NotNull LongFunction<Bean> create) {
		super(variableId);
		getBean = get;
		createBean = create;
	}

	@Override
	public @NotNull Bean getBean() {
		if (!isManaged())
			return bean;
		var txn = Transaction.getCurrentVerifyRead(this);
		if (txn == null)
			return bean;
		var log = (LogDynamic)txn.getLog(dynamicLogKey());
		//noinspection DataFlowIssue
		return log != null ? log.value : bean;
	}

	public void setBean(@Nullable Bean bean) {
		if (bean == null)
			bean = new EmptyBean();
		setBeanWithSpecialTypeId(getBean.applyAsLong(bean), bean);
	}

	@SuppressWarnings("deprecation")
	private void setBeanWithSpecialTypeId(long specialTypeId, @NotNull Bean bean) {
		if (bean instanceof DynamicBean) // 不允许嵌套放入DynamicBean,否则序列化会输出错误的数据流
			bean = ((DynamicBean)bean).getBean();
		if (bean instanceof Collection) {
			if (bean instanceof CollOne)
				bean = ((CollOne<?>)bean).getValue();
			else
				throw new IllegalStateException("can not set Collection Bean into DynamicBean");
		}
		if (!isManaged()) {
			typeId = specialTypeId;
			this.bean = bean;
			return;
		}
		// 先写校验后挂接：verifyWrite抛IllegalStateException（不在事务中/记录不受控）时，
		// 新bean已受管却从未入日志（redo-only外的脏归属），此后复用必抛HasManagedException。
		var txn = Transaction.getCurrentVerifyWrite(this);
		bean.initRootInfoWithRedo(rootInfo, this);
		bean.variableId(1); // 只有一个变量
		var log = (LogDynamic)txn.logGetOrAdd(dynamicLogKey(), this::createLogBean);
		log.setValue(specialTypeId, bean);
	}

	@Override
	public long getTypeId() {
		if (!isManaged())
			return typeId;
		var txn = Transaction.getCurrentVerifyRead(this);
		if (txn == null)
			return typeId;
		// 不能独立设置，总是设置Bean时一起Commit，所以这里访问Bean的Log。
		var log = (LogDynamic)txn.getLog(dynamicLogKey());
		return log != null ? log.specialTypeId : typeId;
	}

	/**
	 * 事务日志键。字段形态（宿主bean的变量）=宿主objectId+varId；集合元素形态（parent为
	 * Collection）所有元素共用同一parent且variableId不参与唯一性（用户工厂元素恒为生成
	 * varId、decode工厂元素恒为0），宿主公式在同组元素间完全碰撞，改用自身objectId：
	 * objectId按{@link #OBJECT_ID_STEP}(4096)步长自增、低12位保留给varId，自身键落在
	 * 自己号段的0槽位，与任何"宿主objectId+varId"键（宿主号段内非0槽位）互不重叠。
	 */
	@SuppressWarnings("DataFlowIssue")
	long dynamicLogKey() {
		return parent() instanceof Collection ? objectId() : parent().objectId() + variableId();
	}

	@Override
	public long typeId() {
		return getTypeId();
	}

	public @NotNull ToLongFunction<Bean> getGetBean() {
		return getBean;
	}

	public @NotNull LongFunction<Bean> getCreateBean() {
		return createBean;
	}

	public @NotNull Bean newBean(long typeId) {
		var bean = createBean.apply(typeId);
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

	public void assign(@NotNull DynamicBean other) {
		setBean(other.getBean().copy());
	}

	public void assign(@NotNull DynamicData other) {
		setBean(other.getData().toBean());
	}

	@Override
	public void assign(@NotNull Data data) {
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
	public @NotNull DynamicBean copy() {
		var copy = new DynamicBean(variableId(), getBean, createBean);
		copy.bean = getBean().copy();
		copy.typeId = getTypeId();
		return copy;
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		bb.WriteLong(getTypeId());
		getBean().encode(bb);
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		// 由于可能在事务中执行，这里仅修改Bean
		// TypeId 在 Bean 提交时才修改，但是要在事务中读到最新值，参见 TypeId 的 getter 实现。
		var newTypeId = bb.ReadLong();
		var newBean = createBean.apply(newTypeId);
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
	protected void initChildrenRootInfo(@NotNull Record.RootInfo root) {
		bean.initRootInfo(root, this);
	}

	@Override
	protected void initChildrenRootInfoWithRedo(@NotNull Record.RootInfo root) {
		bean.initRootInfoWithRedo(root, this);
	}

	@Override
	public @NotNull Object mapKey() {
		return mapKey;
	}

	@Override
	public void mapKey(@NotNull Object mapKey) {
		this.mapKey = mapKey;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void followerApply(@NotNull Log log) {
		var dLog = (LogDynamic)log;
		if (dLog.value != null) {
			typeId = dLog.specialTypeId;
			// dLog.value是decode反射新建的非受管bean，必须对齐写路径setBeanWithSpecialTypeId的
			// 初始化集（initRootInfo+variableId(1)），与C-4(CollOne)/PMap2等全部同类实现一致。
			// rootInfo为null（非受管）时initRootInfo内部保持子bean非受管，安全。
			dLog.value.initRootInfo(rootInfo, this);
			dLog.value.variableId(1); // 只有一个变量
			bean = dLog.value;
		} else if (dLog.logBean != null)
			bean.followerApply(dLog.logBean);
	}

	@Override
	public @NotNull LogBean createLogBean() {
		return new LogDynamic(parent(), variableId(), this);
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
}
