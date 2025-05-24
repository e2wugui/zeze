package Zeze.Transaction;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Util.Json;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class DynamicData extends Data {
	protected @NotNull Data data = EmptyBean.Data.instance;
	protected long typeId = EmptyBean.Data.TYPEID;

	protected DynamicData() {
	}

	public @NotNull Data getData() {
		return data;
	}

	public void setData(@Nullable Data data) {
		if (data == null)
			data = EmptyBean.Data.instance;
		setDataWithSpecialTypeId(toTypeId(data), data);
	}

	private void setDataWithSpecialTypeId(long specialTypeId, @NotNull Data data) {
		if (data instanceof DynamicData) // 不允许嵌套放入DynamicData,否则序列化会输出错误的数据流
			data = ((DynamicData)data).data;
		typeId = specialTypeId;
		this.data = data;
	}

	public long getTypeId() {
		return typeId;
	}

	@Override
	public long typeId() {
		return typeId;
	}

	public abstract long toTypeId(@NotNull Data data);

	public abstract Data toData(long typeId);

	public @NotNull Data newData(long typeId) {
		var data = toData(typeId);
		if (data == null) {
			if (typeId == EmptyBean.Data.TYPEID)
				data = EmptyBean.Data.instance;
			else
				throw new IllegalStateException("incompatible DynamicData typeId=" + typeId);
		} else if (typeId == EmptyBean.Data.TYPEID && !(data instanceof EmptyBean.Data))
			typeId = toTypeId(data); // 再确认一下真正的typeId
		setDataWithSpecialTypeId(typeId, data);
		return data;
	}

	public void assign(@NotNull DynamicData other) {
		setData(other.data.copy());
	}

	public void assign(@NotNull DynamicBean other) {
		setData(other.getBean().toData());
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

	public boolean isEmpty() {
		return typeId == EmptyBean.Data.TYPEID && data.getClass() == EmptyBean.Data.class;
	}

	@Override
	public void reset() {
		data = EmptyBean.Data.instance;
		typeId = EmptyBean.Data.TYPEID;
	}

	@Override
	public @NotNull DynamicData copy() {
		var copy = (DynamicData)clone();
		copy.data = data.copy();
		return copy;
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		bb.WriteLong(typeId);
		data.encode(bb);
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		var newTypeId = bb.ReadLong();
		var newData = toData(newTypeId);
		if (newData == null) {
			if (ByteBuffer.IGNORE_INCOMPATIBLE_FIELD || newTypeId == EmptyBean.Data.TYPEID) {
				newTypeId = EmptyBean.Data.TYPEID;
				newData = EmptyBean.Data.instance;
			} else
				throw new IllegalStateException("incompatible DynamicData typeId=" + newTypeId);
		} else if (newTypeId == EmptyBean.Data.TYPEID && !(newData instanceof EmptyBean.Data))
			newTypeId = toTypeId(newData); // 再确认一下真正的typeId
		newData.decode(bb);
		setDataWithSpecialTypeId(newTypeId, newData);
	}

	@Override
	public int preAllocSize() {
		return 9 + data.preAllocSize(); // [9]typeId
	}

	@Override
	public void preAllocSize(int size) {
		data.preAllocSize(size - 1); // [1]typeId
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
		var that = (DynamicData)o;
		return typeId == that.typeId && data.equals(that.data);
	}

	public static void registerJsonParser(@NotNull Class<? extends DynamicData> cls) {
		Json.instance.getClassMeta(cls).setParser((reader, classMeta, fieldMeta, obj, parent) -> {
			if (obj == null) {
				obj = classMeta.getCtor().create();
				if (obj == null)
					return null;
			}
			obj.reset();
			int p = reader.pos();
			reader.parse0(obj, classMeta);
			Data data = obj.toData(obj.typeId);
			obj.setData(data != null ? data : EmptyBean.Data.instance);
			reader.pos(p).parse0(obj, classMeta);
			return obj;
		});
	}
}
