package Zeze.Transaction;

import java.util.function.LongFunction;
import java.util.function.ToLongFunction;
import Zeze.Serialize.ByteBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DynamicBeanData extends Data {
	@NotNull Data data;
	long typeId;
	transient final @NotNull ToLongFunction<Data> getData;
	transient final @NotNull LongFunction<Data> createData;

	public DynamicBeanData(int variableId, @NotNull ToLongFunction<Data> get, @NotNull LongFunction<Data> create) {
		super(variableId);
		data = new EmptyBean.Data();
		typeId = EmptyBean.Data.TYPEID;
		getData = get;
		createData = create;
	}

	public final @NotNull Data getBean() {
		return data;
	}

	public final void setBean(@Nullable Data data) {
		if (data == null)
			data = new EmptyBean.Data();
		setBeanWithSpecialTypeId(getData.applyAsLong(data), data);
	}

	private void setBeanWithSpecialTypeId(long specialTypeId, @NotNull Data data) {
		typeId = specialTypeId;
		this.data = data;
	}

	public long getTypeId() {
		return typeId;
	}

	@Override
	public long typeId() {
		return getTypeId();
	}

	public final @NotNull ToLongFunction<Data> getGetData() {
		return getData;
	}

	public final @NotNull LongFunction<Data> getCreateData() {
		return createData;
	}

	public @NotNull Data newData(long typeId) {
		var data = createData.apply(typeId);
		if (data == null) {
			if (typeId == EmptyBean.Data.TYPEID)
				data = new EmptyBean.Data();
			else
				throw new IllegalStateException("incompatible DynamicBeanData typeId=" + typeId);
		} else if (typeId == EmptyBean.Data.TYPEID && !(data instanceof EmptyBean.Data))
			typeId = getData.applyAsLong(data); // 再确认一下真正的typeId
		setBeanWithSpecialTypeId(typeId, data);
		return data;
	}

	public final void assign(@NotNull DynamicBeanData other) {
		setBean(other.getBean().copy());
	}

	public final void assign(@NotNull DynamicBean other) {
		setBean(other.getBean().toData());
	}

	@Override
	public void assign(@NotNull Bean bean) {
		assign((DynamicBean)bean);
	}

	@Deprecated // unsupported
	@Override
	public @NotNull DynamicBean toBean() {
		throw new UnsupportedOperationException();
	}

	public final boolean isEmpty() {
		return typeId == EmptyBean.Data.TYPEID && data.getClass() == EmptyBean.Data.class;
	}

	public final void reset() {
		data = new EmptyBean.Data();
		typeId = EmptyBean.Data.TYPEID;
	}

	@Override
	public @NotNull DynamicBeanData copy() {
		var copy = new DynamicBeanData(variableId(), getData, createData);
		copy.data = getBean().copy();
		copy.typeId = getTypeId();
		return copy;
	}

	@Override
	public void decode(@NotNull ByteBuffer bb) {
		var typeId = bb.ReadLong();
		var real = createData.apply(typeId);
		if (real == null) {
			if (ByteBuffer.IGNORE_INCOMPATIBLE_FIELD || typeId == EmptyBean.Data.TYPEID) {
				typeId = EmptyBean.Data.TYPEID;
				real = new EmptyBean.Data();
			} else
				throw new IllegalStateException("incompatible DynamicBeanData typeId=" + typeId);
		} else if (typeId == EmptyBean.Data.TYPEID && !(real instanceof EmptyBean.Data))
			typeId = getData.applyAsLong(real); // 再确认一下真正的typeId
		real.decode(bb);
		setBeanWithSpecialTypeId(typeId, real);
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
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
		return Long.hashCode(typeId) ^ data.hashCode();
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (o == this)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		var that = (DynamicBeanData)o;
		return typeId == that.typeId && data.equals(that.data);
	}
}
