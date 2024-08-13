package Zeze.Serialize;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collection;
import java.util.function.Supplier;
import Zeze.Net.Binary;
import Zeze.Transaction.DynamicBean;
import Zeze.Transaction.DynamicData;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IByteBuffer {
	boolean IGNORE_INCOMPATIBLE_FIELD = false; // 不忽略兼容字段则会抛异常

	// 只能增加新的类型定义，增加时记得同步 SkipUnknownField
	int INTEGER = 0; // byte,short,int,long,bool
	int FLOAT = 1; // float
	int DOUBLE = 2; // double
	int BYTES = 3; // binary,string
	int LIST = 4; // list,set
	int MAP = 5; // map
	int BEAN = 6; // bean
	int DYNAMIC = 7; // dynamic
	int VECTOR2 = 8; // float{x,y}
	int VECTOR2INT = 9; // int{x,y}
	int VECTOR3 = 10; // float{x,y,z}
	int VECTOR3INT = 11; // int{x,y,z}
	int VECTOR4 = 12; // float{x,y,z,w} Quaternion

	int TAG_SHIFT = 4;
	int TAG_MASK = (1 << TAG_SHIFT) - 1;
	int ID_MASK = 0xff - TAG_MASK;

	int getReadIndex();

	void setReadIndex(int ri);

	int getWriteIndex();

	int capacity();

	int size();

	boolean isEmpty();

	void FreeInternalBuffer();

	byte @NotNull [] Copy();

	void Reset();

	default void throwEnsureReadException(int size) {
		throw new IllegalStateException("ensureRead " + getReadIndex() + '+' + size + " > " + getWriteIndex());
	}

	void ensureRead(int size);

	boolean ReadBool();

	byte ReadByte();

	int ReadInt4();

	void ReadInt4s(int @NotNull [] buf, int offset, int length);

	long ReadLong8();

	void ReadLong8s(long @NotNull [] buf, int offset, int length);

	// 返回值应被看作是无符号32位整数
	default int ReadUInt() {
		return (int)ReadULong();
	}

	void SkipUInt();

	// 返回值应被看作是无符号64位整数
	default long ReadULong() {
		int b = ReadByte();
		switch ((b >> 4) & 0xf) {
		//@formatter:off
		case  0: case  1: case  2: case  3: case 4: case 5: case 6: case 7: return b;
		case  8: case  9: case 10: case 11: return ((b & 0x3f) <<  8) + ReadLong1();
		case 12: case 13:                   return ((b & 0x1f) << 16) + ReadLong2BE();
		case 14:                            return ((b & 0x0f) << 24) + ReadLong3BE();
		default:
			//noinspection EnhancedSwitchMigration
			switch (b & 0xf) {
			case  0: case  1: case  2: case  3: case 4: case 5: case 6: case 7:
				return ((long)(b & 7) << 32) + ReadLong4BE();
			case  8: case  9: case 10: case 11: return ((long)(b & 3) << 40) + ReadLong5BE();
			case 12: case 13:                   return ((long)(b & 1) << 48) + ReadLong6BE();
			case 14:                            return ReadLong7BE();
			default:                            return ReadLong8BE();
			}
		//@formatter:on
		}
	}

	default void SkipULong() {
		int b = ReadByte();
		switch ((b >> 4) & 0xf) {
		//@formatter:off
		case  0: case  1: case  2: case  3: case 4: case 5: case 6: case 7: return;
		case  8: case  9: case 10: case 11: Skip(1); return;
		case 12: case 13:                   Skip(2); return;
		case 14:                            Skip(3); return;
		default:
			switch (b & 0xf) {
			case  0: case  1: case  2: case  3: case 4: case 5: case 6: case 7: Skip(4); return;
			case  8: case  9: case 10: case 11: Skip(5); return;
			case 12: case 13:                   Skip(6); return;
			case 14:                            Skip(7); return;
			default:                            Skip(8);
			}
		//@formatter:on
		}
	}

	default long ReadLong1() {
		return ReadByte() & 0xffL;
	}

	long ReadLong2BE();

	long ReadLong3BE();

	long ReadLong4BE();

	long ReadLong5BE();

	long ReadLong6BE();

	long ReadLong7BE();

	long ReadLong8BE();

	default long ReadLong() {
		int b = ReadByte();
		switch ((b >> 3) & 0x1f) {
		//@formatter:off
		case 0x00: case 0x01: case 0x02: case 0x03: case 0x04: case 0x05: case 0x06: case 0x07:
		case 0x18: case 0x19: case 0x1a: case 0x1b: case 0x1c: case 0x1d: case 0x1e: case 0x1f: return b;
		case 0x08: case 0x09: case 0x0a: case 0x0b: return ((b - 0x40 ) <<  8) + ReadLong1();
		case 0x14: case 0x15: case 0x16: case 0x17: return ((b + 0x40 ) <<  8) + ReadLong1();
		case 0x0c: case 0x0d:                       return ((b - 0x60 ) << 16) + ReadLong2BE();
		case 0x12: case 0x13:                       return ((b + 0x60 ) << 16) + ReadLong2BE();
		case 0x0e:                                  return ((b - 0x70L) << 24) + ReadLong3BE();
		case 0x11:                                  return ((b + 0x70L) << 24) + ReadLong3BE();
		case 0x0f:
			//noinspection EnhancedSwitchMigration
			switch (b & 7) {
			case 0: case 1: case 2: case 3: return ((long)(b - 0x78) << 32) + ReadLong4BE();
			case 4: case 5:                 return ((long)(b - 0x7c) << 40) + ReadLong5BE();
			case 6:                         return ReadLong6BE();
			default: long r = ReadLong7BE(); return r < 0x80_0000_0000_0000L ?
					r : ((r - 0x80_0000_0000_0000L) << 8) + ReadLong1();
			}
		default: // 0x10
			//noinspection EnhancedSwitchMigration
			switch (b & 7) {
			case 4: case 5: case 6: case 7: return ((long)(b + 0x78) << 32) + ReadLong4BE();
			case 2: case 3:                 return ((long)(b + 0x7c) << 40) + ReadLong5BE();
			case 1:                         return 0xffff_0000_0000_0000L + ReadLong6BE();
			default: long r = ReadLong7BE(); return r >= 0x80_0000_0000_0000L ?
					0xff00_0000_0000_0000L + r : ((r + 0x80_0000_0000_0000L) << 8) + ReadLong1();
			}
		//@formatter:on
		}
	}

	default void SkipLong() {
		int b = ReadByte();
		switch ((b >> 3) & 0x1f) {
		//@formatter:off
		case 0x00: case 0x01: case 0x02: case 0x03: case 0x04: case 0x05: case 0x06: case 0x07:
		case 0x18: case 0x19: case 0x1a: case 0x1b: case 0x1c: case 0x1d: case 0x1e: case 0x1f: return;
		case 0x08: case 0x09: case 0x0a: case 0x0b:
		case 0x14: case 0x15: case 0x16: case 0x17: Skip(1); return;
		case 0x0c: case 0x0d: case 0x12: case 0x13: Skip(2); return;
		case 0x0e: case 0x11:                       Skip(3); return;
		case 0x0f:
			switch (b & 7) {
			case 0: case 1: case 2: case 3: Skip(4); return;
			case 4: case 5:                 Skip(5); return;
			case 6:                         Skip(6); return;
			default: Skip(6 + (ReadByte() >>> 31)); return;
			}
		default: // 0x10
			switch (b & 7) {
			case 4: case 5: case 6: case 7: Skip(4); return;
			case 2: case 3:                 Skip(5); return;
			case 1:                         Skip(6); return;
			default: Skip(7 - (ReadByte() >>> 31));
			}
		//@formatter:on
		}
	}

	default int ReadInt() {
		return (int)ReadLong();
	}

	float ReadFloat();

	void ReadFloats(float @NotNull [] buf, int offset, int length);

	double ReadDouble();

	void ReadDoubles(double @NotNull [] buf, int offset, int length);

	@NotNull String ReadString();

	default @NotNull Binary ReadBinary() {
		var bytes = ReadBytes();
		return bytes.length > 0 ? new Binary(bytes) : Binary.Empty;
	}

	byte @NotNull [] ReadBytes();

	void Skip(int n);

	@NotNull ByteBuffer ReadByteBuffer();

	@SuppressWarnings("unchecked")
	default <T extends java.io.Serializable> T ReadJavaObject() {
		var bb = ReadByteBuffer();
		try (var bs = new ByteArrayInputStream(bb.Bytes, bb.ReadIndex, bb.size());
			 var os = new ObjectInputStream(bs)) {
			return (T)os.readObject();
		} catch (IOException | ClassNotFoundException e) {
			throw Task.forceThrow(e);
		}
	}

	default void SkipBytes() {
		int n = ReadUInt();
		if (n < 0) {
			throw new IllegalStateException("invalid length for SkipBytes: " + n
					+ " at " + getReadIndex() + '/' + getWriteIndex());
		}
		Skip(n);
	}

	default void SkipBytes4() {
		int n = ReadInt4();
		if (n < 0) {
			throw new IllegalStateException("invalid length for SkipBytes4: " + n
					+ " at " + getReadIndex() + '/' + getWriteIndex());
		}
		Skip(n);
	}

	default <T extends Serializable> void decode(@NotNull Collection<T> c, @NotNull Supplier<T> factory) {
		for (int n = ReadUInt(); n > 0; n--) {
			T v = factory.get();
			v.decode(this);
			c.add(v);
		}
	}

	default int ReadTagSize(int tagByte) {
		int deltaId = (tagByte & ID_MASK) >> TAG_SHIFT;
		return deltaId < 0xf ? deltaId : 0xf + ReadUInt();
	}

	default boolean ReadBool(int tag) {
		int type = tag & TAG_MASK;
		if (type == INTEGER)
			return ReadLong() != 0;
		if (type == FLOAT)
			return ReadFloat() != 0;
		if (type == DOUBLE)
			return ReadDouble() != 0;
		if (IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(tag);
			return false;
		}
		throw new IllegalStateException("can not ReadBool for type=" + type
				+ " at " + getReadIndex() + '/' + getWriteIndex());
	}

	default byte ReadByte(int tag) {
		int type = tag & TAG_MASK;
		if (type == INTEGER)
			return (byte)ReadLong();
		if (type == FLOAT)
			return (byte)ReadFloat();
		if (type == DOUBLE)
			return (byte)ReadDouble();
		if (IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(tag);
			return 0;
		}
		throw new IllegalStateException("can not ReadByte for type=" + type
				+ " at " + getReadIndex() + '/' + getWriteIndex());
	}

	default short ReadShort(int tag) {
		int type = tag & TAG_MASK;
		if (type == INTEGER)
			return (short)ReadLong();
		if (type == FLOAT)
			return (short)ReadFloat();
		if (type == DOUBLE)
			return (short)ReadDouble();
		if (IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(tag);
			return 0;
		}
		throw new IllegalStateException("can not ReadShort for type=" + type
				+ " at " + getReadIndex() + '/' + getWriteIndex());
	}

	default int ReadInt(int tag) {
		int type = tag & TAG_MASK;
		if (type == INTEGER)
			return (int)ReadLong();
		if (type == FLOAT)
			return (int)ReadFloat();
		if (type == DOUBLE)
			return (int)ReadDouble();
		if (IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(tag);
			return 0;
		}
		throw new IllegalStateException("can not ReadInt for type=" + type
				+ " at " + getReadIndex() + '/' + getWriteIndex());
	}

	default long ReadLong(int tag) {
		int type = tag & TAG_MASK;
		if (type == INTEGER)
			return ReadLong();
		if (type == FLOAT)
			return (long)ReadFloat();
		if (type == DOUBLE)
			return (long)ReadDouble();
		if (IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(tag);
			return 0;
		}
		throw new IllegalStateException("can not ReadLong for type=" + type
				+ " at " + getReadIndex() + '/' + getWriteIndex());
	}

	default float ReadFloat(int tag) {
		int type = tag & TAG_MASK;
		if (type == FLOAT)
			return ReadFloat();
		if (type == DOUBLE)
			return (float)ReadDouble();
		if (type == INTEGER)
			return ReadLong();
		if (IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(tag);
			return 0;
		}
		throw new IllegalStateException("can not ReadFloat for type=" + type
				+ " at " + getReadIndex() + '/' + getWriteIndex());
	}

	default double ReadDouble(int tag) {
		int type = tag & TAG_MASK;
		if (type == DOUBLE)
			return ReadDouble();
		if (type == FLOAT)
			return ReadFloat();
		if (type == INTEGER)
			return ReadLong();
		if (IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(tag);
			return 0;
		}
		throw new IllegalStateException("can not ReadDouble for type=" + type
				+ " at " + getReadIndex() + '/' + getWriteIndex());
	}

	default @NotNull Binary ReadBinary(int tag) {
		int type = tag & TAG_MASK;
		if (type == BYTES)
			return ReadBinary();
		if (IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(tag);
			return Binary.Empty;
		}
		throw new IllegalStateException("can not ReadBinary for type=" + type
				+ " at " + getReadIndex() + '/' + getWriteIndex());
	}

	default @NotNull String ReadString(int tag) {
		int type = tag & TAG_MASK;
		if (type == BYTES)
			return ReadString();
		if (IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(tag);
			return "";
		}
		throw new IllegalStateException("can not ReadString for type=" + type
				+ " at " + getReadIndex() + '/' + getWriteIndex());
	}

	@NotNull Vector2 ReadVector2();

	@NotNull Vector3 ReadVector3();

	@NotNull Vector4 ReadVector4();

	@NotNull Quaternion ReadQuaternion();

	@Nullable ByteBuffer readUnknownField(int idx, int tag, @Nullable ByteBuffer unknown);

	default byte @Nullable [] readAllUnknownFields(int idx, int tag, @Nullable ByteBuffer unknown) {
		while (tag != 0) {
			unknown = readUnknownField(idx, tag, unknown);
			idx += ReadTagSize(tag = ReadByte());
		}
		return unknown != null ? unknown.CopyIf() : null;
	}

	default @NotNull Vector2Int ReadVector2Int() {
		int x = ReadInt();
		int y = ReadInt();
		return new Vector2Int(x, y);
	}

	default @NotNull Vector3Int ReadVector3Int() {
		int x = ReadInt();
		int y = ReadInt();
		int z = ReadInt();
		return new Vector3Int(x, y, z);
	}

	default @NotNull Vector2 ReadVector2(int tag) {
		int type = tag & TAG_MASK;
		if (type == VECTOR2)
			return ReadVector2();
		if (type == VECTOR3)
			return ReadVector3();
		if (type == VECTOR4)
			return ReadVector4();
		if (type == VECTOR2INT)
			return new Vector2(ReadVector2Int());
		if (type == VECTOR3INT)
			return new Vector3(ReadVector3Int());
		if (type == FLOAT)
			return new Vector2(ReadFloat(), 0);
		if (type == DOUBLE)
			return new Vector2((float)ReadDouble(), 0);
		if (type == INTEGER)
			return new Vector2(ReadLong(), 0);
		if (IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(tag);
			return Vector2.ZERO;
		}
		throw new IllegalStateException("can not ReadVector2 for type=" + type
				+ " at " + getReadIndex() + '/' + getWriteIndex());
	}

	default @NotNull Vector3 ReadVector3(int tag) {
		int type = tag & TAG_MASK;
		if (type == VECTOR3)
			return ReadVector3();
		if (type == VECTOR2)
			return new Vector3(ReadVector2());
		if (type == VECTOR4)
			return ReadVector4();
		if (type == VECTOR3INT)
			return new Vector3(ReadVector3Int());
		if (type == VECTOR2INT)
			return new Vector3(ReadVector2Int());
		if (type == FLOAT)
			return new Vector3(ReadFloat(), 0, 0);
		if (type == DOUBLE)
			return new Vector3((float)ReadDouble(), 0, 0);
		if (type == INTEGER)
			return new Vector3(ReadLong(), 0, 0);
		if (IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(tag);
			return Vector3.ZERO;
		}
		throw new IllegalStateException("can not ReadVector3 for type=" + type
				+ " at " + getReadIndex() + '/' + getWriteIndex());
	}

	default @NotNull Vector4 ReadVector4(int tag) {
		int type = tag & TAG_MASK;
		if (type == VECTOR4)
			return ReadVector4();
		if (type == VECTOR3)
			return new Vector4(ReadVector3());
		if (type == VECTOR2)
			return new Vector4(ReadVector2());
		if (type == VECTOR3INT)
			return new Vector4(ReadVector3Int());
		if (type == VECTOR2INT)
			return new Vector4(ReadVector2Int());
		if (type == FLOAT)
			return new Vector4(ReadFloat(), 0, 0, 0);
		if (type == DOUBLE)
			return new Vector4((float)ReadDouble(), 0, 0, 0);
		if (type == INTEGER)
			return new Vector4(ReadLong(), 0, 0, 0);
		if (IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(tag);
			return Vector4.ZERO;
		}
		throw new IllegalStateException("can not ReadVector4 for type=" + type
				+ " at " + getReadIndex() + '/' + getWriteIndex());
	}

	default @NotNull Quaternion ReadQuaternion(int tag) {
		int type = tag & TAG_MASK;
		if (type == VECTOR4)
			return ReadQuaternion();
		if (type == VECTOR3)
			return new Quaternion(ReadVector3());
		if (type == VECTOR2)
			return new Quaternion(ReadVector2());
		if (type == VECTOR3INT)
			return new Quaternion(ReadVector3Int());
		if (type == VECTOR2INT)
			return new Quaternion(ReadVector2Int());
		if (type == FLOAT)
			return new Quaternion(ReadFloat(), 0, 0, 0);
		if (type == DOUBLE)
			return new Quaternion((float)ReadDouble(), 0, 0, 0);
		if (type == INTEGER)
			return new Quaternion(ReadLong(), 0, 0, 0);
		if (IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(tag);
			return Quaternion.ZERO;
		}
		throw new IllegalStateException("can not ReadQuaternion for type=" + type
				+ " at " + getReadIndex() + '/' + getWriteIndex());
	}

	default @NotNull Vector2Int ReadVector2Int(int tag) {
		int type = tag & TAG_MASK;
		if (type == VECTOR2INT)
			return ReadVector2Int();
		if (type == VECTOR3INT)
			return ReadVector3Int();
		if (type == VECTOR2)
			return new Vector2Int(ReadVector2());
		if (type == VECTOR3)
			return new Vector3Int(ReadVector3());
		if (type == VECTOR4)
			return new Vector3Int(ReadVector4());
		if (type == INTEGER)
			return new Vector2Int(ReadInt(), 0);
		if (type == FLOAT)
			return new Vector2Int((int)ReadFloat(), 0);
		if (type == DOUBLE)
			return new Vector2Int((int)ReadDouble(), 0);
		if (IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(tag);
			return Vector2Int.ZERO;
		}
		throw new IllegalStateException("can not ReadVector2Int for type=" + type
				+ " at " + getReadIndex() + '/' + getWriteIndex());
	}

	default @NotNull Vector3Int ReadVector3Int(int tag) {
		int type = tag & TAG_MASK;
		if (type == VECTOR3INT)
			return ReadVector3Int();
		if (type == VECTOR2INT)
			return new Vector3Int(ReadVector2Int());
		if (type == VECTOR3)
			return new Vector3Int(ReadVector3());
		if (type == VECTOR2)
			return new Vector3Int(ReadVector2());
		if (type == VECTOR4)
			return new Vector3Int(ReadVector4());
		if (type == INTEGER)
			return new Vector3Int(ReadInt(), 0, 0);
		if (type == FLOAT)
			return new Vector3Int((int)ReadFloat(), 0, 0);
		if (type == DOUBLE)
			return new Vector3Int((int)ReadDouble(), 0, 0);
		if (IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(tag);
			return Vector3Int.ZERO;
		}
		throw new IllegalStateException("can not ReadVector3Int for type=" + type
				+ " at " + getReadIndex() + '/' + getWriteIndex());
	}

	default <T extends Serializable> @NotNull T ReadBean(@NotNull T bean, int tag) {
		int type = tag & TAG_MASK;
		if (type == BEAN)
			bean.decode(this);
		else if (type == DYNAMIC) {
			SkipLong();
			bean.decode(this);
		} else if (IGNORE_INCOMPATIBLE_FIELD)
			SkipUnknownField(tag);
		else {
			throw new IllegalStateException("can not ReadBean(" + bean.getClass().getName() + ") for type=" + type
					+ " at " + getReadIndex() + '/' + getWriteIndex());
		}
		return bean;
	}

	default @NotNull DynamicBean ReadDynamic(@NotNull DynamicBean dynBean, int tag) {
		int type = tag & TAG_MASK;
		if (type == DYNAMIC)
			dynBean.decode(this);
		else if (type == BEAN)
			dynBean.newBean(0).decode(this);
		else if (IGNORE_INCOMPATIBLE_FIELD)
			SkipUnknownField(tag);
		else {
			throw new IllegalStateException("can not ReadDynamic for type=" + type
					+ " at " + getReadIndex() + '/' + getWriteIndex());
		}
		return dynBean;
	}

	default @NotNull DynamicData ReadDynamic(@NotNull DynamicData dynBean, int tag) {
		int type = tag & TAG_MASK;
		if (type == DYNAMIC) {
			dynBean.decode(this);
			return dynBean;
		}
		if (type == BEAN) {
			var bean = dynBean.toData(0);
			if (bean != null) {
				bean.decode(this);
				return dynBean;
			}
		}
		if (IGNORE_INCOMPATIBLE_FIELD) {
			SkipUnknownField(tag);
			return dynBean;
		}
		throw new IllegalStateException("can not ReadDynamic for type=" + type
				+ " at " + getReadIndex() + '/' + getWriteIndex());
	}

	default void SkipUnknownFieldOrThrow(int tag, @NotNull String curType) {
		if (IGNORE_INCOMPATIBLE_FIELD)
			SkipUnknownField(tag);
		else {
			throw new IllegalStateException("can not read " + curType + " for type=" + (tag & TAG_MASK)
					+ " at " + getReadIndex() + '/' + getWriteIndex());
		}
	}

	default void SkipUnknownField(int tag, int count) {
		while (--count >= 0)
			SkipUnknownField(tag);
	}

	default void SkipUnknownField(int type1, int type2, int count) {
		type1 |= 0x10; // ensure high bits not zero
		type2 |= 0x10; // ensure high bits not zero
		while (--count >= 0) {
			SkipUnknownField(type1);
			SkipUnknownField(type2);
		}
	}

	default void skipField(int type) {
		SkipUnknownField(type | 0x10); // ensure high bits not zero
	}

	default void SkipUnknownField(int tag) {
		int type = tag & TAG_MASK;
		switch (type) {
		case INTEGER:
			SkipLong();
			return;
		case FLOAT:
			if (tag == FLOAT) // high bits == 0
				return;
			Skip(4);
			return;
		case DOUBLE:
		case VECTOR2:
			Skip(8);
			return;
		case VECTOR2INT:
			SkipLong();
			SkipLong();
			return;
		case VECTOR3:
			Skip(12);
			return;
		case VECTOR3INT:
			SkipLong();
			SkipLong();
			SkipLong();
			return;
		case VECTOR4:
			Skip(16);
			return;
		case BYTES:
			SkipBytes();
			return;
		case LIST:
			int t = ReadByte();
			SkipUnknownField(t, ReadTagSize(t));
			return;
		case MAP:
			t = ReadByte();
			SkipUnknownField(t >> TAG_SHIFT, t, ReadUInt());
			return;
		case DYNAMIC:
			SkipLong();
			//noinspection fallthrough
		case BEAN:
			while ((t = ReadByte()) != 0) {
				if ((t & ID_MASK) == 0xf0)
					SkipUInt();
				SkipUnknownField(t);
			}
			return;
		default:
			throw new IllegalStateException("SkipUnknownField: type=" + type
					+ " at " + getReadIndex() + '/' + getWriteIndex());
		}
	}

	default void skipAllUnknownFields(int tag) {
		while (tag != 0) {
			SkipUnknownField(tag);
			ReadTagSize(tag = ReadByte());
		}
	}

	default long readUnknownIndex() {
		return isEmpty() ? Long.MAX_VALUE : ReadUInt();
	}
}
