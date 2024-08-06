package Zeze.Serialize;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import Zeze.Net.Binary;
import Zeze.Util.BitConverter;
import Zeze.Util.Str;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GenericBean {
	public static final int INDENT = 4;

	public final Map<Integer, Object> fields = new TreeMap<>(); // key=0 for parent bean

	public interface Walker {
		/**
		 * 以回调方式遍历Bean的序列化数据. 如果需要忽略当前字段,需要调用bb.skipField(type)
		 *
		 * @param varId 字段id. 从1开始按顺序遍历, 但最后有可能出现0表示父类型(此时type一定是Bean类型)
		 * @param type  {@link ByteBuffer#INTEGER}等类型枚举. Bean类型可以递归调用walk
		 * @param bb    当前的bb,如果要继续walk则需要bb.ReadIndex移过当前字段
		 * @return 是否继续walk
		 */
		boolean handleField(int varId, int type, @NotNull IByteBuffer bb);
	}

	/**
	 * @return 如果全部遍历完则返回true; 如果walker中途返回了false则立即返回false
	 */
	public static boolean walk(@NotNull IByteBuffer bb, @NotNull Walker walker) {
		for (int id = 0; ; ) {
			int tag = bb.ReadByte() & 0xff;
			if (tag < 0x10) {
				if (tag == 1 && !walker.handleField(0, ByteBuffer.BEAN, bb))
					return false;
				if (tag <= 1)
					return true;
				throw new IllegalStateException("invalid tag=" + tag);
			}
			id += bb.ReadTagSize(tag);
			if (!walker.handleField(id, tag & ByteBuffer.TAG_MASK, bb))
				return false;
		}
	}

	public @NotNull GenericBean decode(@NotNull IByteBuffer bb) {
		fields.clear();
		for (int id = 0; ; ) {
			int tag = bb.ReadByte() & 0xff;
			if (tag < 0x10) {
				if (tag == 1)
					fields.put(0, decodeField(bb, ByteBuffer.BEAN));
				if (tag <= 1)
					return this;
				throw new IllegalStateException("invalid tag=" + tag);
			}
			id += bb.ReadTagSize(tag);
			fields.put(id, decodeField(bb, tag & ByteBuffer.TAG_MASK));
		}
	}

	public static @NotNull Object decodeField(@NotNull IByteBuffer bb, int type) {
		switch (type) {
		case ByteBuffer.INTEGER:
			var vl = bb.ReadLong();
			int vi = (int)vl;
			return vi == vl ? (Object)vi : (Object)vl;
		case ByteBuffer.FLOAT:
			return bb.ReadFloat();
		case ByteBuffer.DOUBLE:
			return bb.ReadDouble();
		case ByteBuffer.BYTES:
			var b = bb.ReadBytes();
			return likeString(b) ? new String(b, StandardCharsets.UTF_8) : b;
		case ByteBuffer.LIST:
			int t = bb.ReadByte();
			int n = bb.ReadTagSize(t);
			t &= ByteBuffer.TAG_MASK;
			var list = new ArrayList<>(Math.min(n, 0x10000));
			for (int i = 0; i < n; i++)
				list.add(decodeField(bb, t));
			return list;
		case ByteBuffer.MAP:
			t = bb.ReadByte();
			n = bb.ReadUInt();
			int tk = (t >> 4) & ByteBuffer.TAG_MASK;
			t &= ByteBuffer.TAG_MASK;
			var map = new HashMap<>(Math.min((int)Math.ceil(n / 0.75f), 0x10000));
			for (int i = 0; i < n; i++) {
				Object k = decodeField(bb, tk);
				map.put(k, decodeField(bb, t));
			}
			return map;
		case ByteBuffer.BEAN:
			return new GenericBean().decode(bb);
		case ByteBuffer.DYNAMIC:
			return new GenericDynamicBean().decode(bb);
		case ByteBuffer.VECTOR2:
			return bb.ReadVector2();
		case ByteBuffer.VECTOR2INT:
			return bb.ReadVector2Int();
		case ByteBuffer.VECTOR3:
			return bb.ReadVector3();
		case ByteBuffer.VECTOR3INT:
			return bb.ReadVector3Int();
		case ByteBuffer.VECTOR4:
			return bb.ReadVector4();
		default:
			throw new IllegalStateException("invalid type=" + type);
		}
	}

	// 判断b是否合法的UTF-8字符串
	public static boolean likeString(byte @NotNull [] b) {
		for (int i = 0, n = b.length; i < n; i++) {
			int c = b[i] & 0xff;
			if (c < 0x20)
				return false;
			if (c < 0x7f)
				continue;
			if (c < 0xc2)
				return false;
			if (c < 0xe0) {
				if (++i >= n)
					return false;
				int d = b[i] & 0xff;
				if (d < 0x80 || d >= 0xc0)
					return false;
			} else if (c < 0xf0) {
				if (i + 2 >= n)
					return false;
				int d = b[++i] & 0xff;
				if (c == 0xe0) {
					if (d < 0xa0 || d >= 0xc0)
						return false;
				} else if (c == 0xed) {
					if (d < 0x80 || d >= 0xa0)
						return false;
				} else if (d < 0x80 || d >= 0xc0)
					return false;
				d = b[++i] & 0xff;
				if (d < 0x80 || d >= 0xc0)
					return false;
			} else if (c < 0xf5) {
				if (i + 3 >= n)
					return false;
				int d = b[++i] & 0xff;
				if (c == 0xf0) {
					if (d < 0x90 || d >= 0xc0)
						return false;
				} else if (c == 0xf4) {
					if (d < 0x80 || d >= 0xa0)
						return false;
				} else if (d < 0x80 || d >= 0xc0)
					return false;
				d = b[++i] & 0xff;
				if (d < 0x80 || d >= 0xc0)
					return false;
				d = b[++i] & 0xff;
				if (d < 0x80 || d >= 0xc0)
					return false;
			} else
				return false;
		}
		return true;
	}

	public @NotNull StringBuilder buildString(@NotNull StringBuilder sb) {
		return buildString(sb, 0);
	}

	public @NotNull StringBuilder buildString(@NotNull StringBuilder sb, int level) {
		sb.append('{');
		String i1, i2;
		if (level >= 0) {
			i1 = Str.indent(level + INDENT);
			i2 = Str.indent(level + INDENT * 2);
			sb.append('\n');
		} else
			i1 = i2 = "";
		for (var e : fields.entrySet()) {
			sb.append(i1).append(e.getKey()).append(": ");
			var v = e.getValue();
			if (v instanceof Number)
				sb.append(v);
			else if (v instanceof String)
				sb.append('"').append(v).append('"');
			else if (v instanceof byte[])
				sb.append(BitConverter.toStringWithLimit((byte[])v, 16, 4));
			else if (v instanceof List) {
				var list = (List<?>)v;
				sb.append('[');
				if (!list.isEmpty()) {
					if (list.get(0) instanceof Number && list.size() <= 16) {
						for (var n : list)
							sb.append(' ').append(n).append(',');
						sb.setCharAt(sb.length() - 1, ' ');
					} else {
						for (var o : list) {
							sb.append('\n').append(i2);
							buildString(sb, level + INDENT * 2, o);
						}
						sb.append('\n').append(i1);
					}
				}
				sb.append(']');
			} else if (v instanceof Map) {
				var map = (Map<?, ?>)v;
				sb.append('{');
				if (!map.isEmpty()) {
					for (var e2 : map.entrySet()) {
						sb.append('\n').append(i2);
						var k2 = e2.getKey();
						var v2 = e2.getValue();
						buildString(sb, -1, k2);
						sb.append(": ");
						buildString(sb, level + INDENT * 2, v2);
					}
					sb.append('\n').append(i1);
				}
				sb.append('}');
			} else if (v instanceof GenericBean)
				((GenericBean)v).buildString(sb, level + INDENT);
			else
				sb.append(v);
			sb.append('\n');
		}
		if (level > 0)
			sb.append(Str.indent(level));
		sb.append('}');
		return sb;
	}

	private static void buildString(@NotNull StringBuilder sb, int level, @Nullable Object o) {
		if (o instanceof String)
			sb.append('"').append(o).append('"');
		else if (o instanceof byte[])
			sb.append(BitConverter.toStringWithLimit((byte[])o, 16, 4));
		else if (o instanceof GenericBean)
			((GenericBean)o).buildString(sb, level);
		else
			sb.append(o);
	}

	@Override
	public @NotNull String toString() {
		return buildString(new StringBuilder()).toString();
	}

	public static void main(String[] args) {
		var b = new Zeze.Builtin.TestRocks.BValue();
		b.setBool(true);
		b.setInt(12);
		b.setFloat(12.34f);
		b.setString("test");
		b.setBinary(new Binary(new byte[]{1, 2}));
		b.getSetInt().add(34);
		b.getSetBeankey().add(new Zeze.Builtin.TestRocks.BeanKey(56, "abc"));
		b.getMapInt().put(78, 90);
		b.getMapBean().put(123, new Zeze.Builtin.TestRocks.BValue());
		b.setBeankey(new Zeze.Builtin.TestRocks.BeanKey(456, "字符串"));
		var bb = ByteBuffer.Allocate(16);
		b.encode(bb);
		System.out.println(new GenericBean().decode(bb));
		bb.ReadIndex = 0;
		walk(bb, (varId, type, bb1) -> {
			System.out.println("varId=" + varId + ", type=" + type + ", readIndex=" + bb1.getReadIndex());
			if (type == ByteBuffer.BEAN) {
				walk(bb1, (varId2, type2, bb2) -> {
					System.out.println("  varId=" + varId2 + ", type=" + type2 + ", readIndex=" + bb2.getReadIndex());
					bb2.skipField(type2);
					return true;
				});
			} else
				bb1.skipField(type);
			return true;
		});
		System.out.println(bb.size());
	}
}
