package Zeze.Transaction;

import java.util.function.LongFunction;
import java.util.function.ToLongFunction;
import Zeze.Serialize.ByteBuffer;

public class DynamicBeanData extends Data {
	Data bean;
	long typeId;
	transient final ToLongFunction<Data> getData;
	transient final LongFunction<Data> createData;

	public DynamicBeanData(int variableId, ToLongFunction<Data> get, LongFunction<Data> create) {
		super(variableId);
		bean = new EmptyBean.Data();
		typeId = EmptyBean.Data.TYPEID;
		getData = get;
		createData = create;
	}

	public final Data getBean() {
		return bean;
	}

	public final void setBean(Data value) {
		if (value == null)
			throw new IllegalArgumentException("null value");
		typeId = getData.applyAsLong(value);
		bean = value;
	}

	public long getTypeId() {
		return typeId;
	}

	@Override
	public long typeId() {
		return getTypeId();
	}

	public final ToLongFunction<Data> getGetData() {
		return getData;
	}

	public final LongFunction<Data> getCreateData() {
		return createData;
	}

	public final void assign(DynamicBeanData other) {
		setBean(other.getBean().copy());
	}

	public final void assign(DynamicBean other) {
		var data = createData.apply(other.typeId());
		data.assign(other.getBean());
		setBean(data);
	}

	@Override
	public void assign(Bean o) {
		assign((DynamicBean)o);
	}

	@Override
	public DynamicBean toBean() {
		throw new UnsupportedOperationException();
	}

	public final boolean isEmpty() {
		return typeId == EmptyBean.TYPEID && bean.getClass() == EmptyBean.Data.class;
	}

	@Override
	public DynamicBeanData copy() {
		var copy = new DynamicBeanData(variableId(), getData, createData);
		copy.bean = getBean().copy();
		copy.typeId = getTypeId();
		return copy;
	}

	private void setBeanWithSpecialTypeId(long specialTypeId, Data bean) {
		typeId = specialTypeId;
		this.bean = bean;
	}

	@Override
	public void decode(ByteBuffer bb) {
		// 由于可能在事务中执行，这里仅修改Bean
		// TypeId 在 Bean 提交时才修改，但是要在事务中读到最新值，参见 TypeId 的 getter 实现。
		long typeId = bb.ReadLong();
		var real = createData.apply(typeId);
		if (real != null) {
			real.decode(bb);
			setBeanWithSpecialTypeId(typeId, real);
		} else {
			bb.SkipUnknownField(ByteBuffer.BEAN);
			setBeanWithSpecialTypeId(EmptyBean.TYPEID, new EmptyBean.Data());
		}
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteLong(getTypeId());
		getBean().encode(bb);
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
	public int hashCode() {
		return Long.hashCode(typeId) ^ bean.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		var that = (DynamicBeanData)o;
		return typeId == that.typeId && bean.equals(that.bean);
	}
}
