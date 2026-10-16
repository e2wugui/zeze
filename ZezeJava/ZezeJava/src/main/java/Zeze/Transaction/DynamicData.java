package Zeze.Transaction;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Util.Json;
import org.jspecify.annotations.Nullable;

/** DynamicBean 的纯数据形态：按 typeId 持有可替换的内部 Data，用于快照与序列化。 */
public abstract class DynamicData extends Data {
	protected Data data = EmptyBean.Data.instance;
	protected long typeId = EmptyBean.Data.TYPEID;

	protected DynamicData() {
	}

	public Data getData() {
		return data;
	}

	public void setData(@Nullable Data data) {
		if (data == null)
			data = EmptyBean.Data.instance;
		setDataWithSpecialTypeId(toTypeId(data), data);
	}

	private void setDataWithSpecialTypeId(long specialTypeId, Data data) {
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

	public abstract long toTypeId(Data data);

	// 未知 typeId 时返回 null（生成侧工厂兜底），调用方须判空或走 newData 的兜底路径。
	public abstract @Nullable Data toData(long typeId);

	public Data newData(long typeId) {
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

	public void assign(DynamicData other) {
		setData(other.data.copy());
	}

	public void assign(DynamicBean other) {
		setData(other.getBean().toData());
	}

	@Override
	public void assign(Bean bean) {
		assign((DynamicBean)bean);
	}

	@Deprecated // unsupported
	@Override
	public DynamicBean toBean() {
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
	public DynamicData copy() {
		var copy = (DynamicData)clone();
		copy.data = data.copy();
		return copy;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteLong(typeId);
		data.encode(bb);
	}

	@Override
	public void decode(IByteBuffer bb) {
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

	@SuppressWarnings("ConstantConditions") // 反射构造器成功时非空，保留扩展用的兜底分支。
	public static void registerJsonParser(Class<? extends DynamicData> cls) {
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
