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

	public @NotNull GenericBean decode(@NotNull ByteBuffer bb) {
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

	public static @NotNull Object decodeField(@NotNull ByteBuffer bb, int type) {
		switch (type) {
		case ByteBuffer.INTEGER:
			var vl = bb.ReadLong();
			int vi = (int)vl;
			return vi == vl ? Integer.valueOf(vi) : Long.valueOf(vl);
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
		if (level >= 0) {
			level += INDENT;
			sb.append('\n');
		}
		for (var e : fields.entrySet()) {
			if (level > 0)
				sb.append(Str.indent(level));
			sb.append(e.getKey()).append(':').append(' ');
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
						level += INDENT;
						for (var o : list) {
							sb.append('\n');
							if (level > 0)
								sb.append(Str.indent(level));
							buildString(sb, level, o);
						}
						sb.append('\n');
						level -= INDENT;
						if (level > 0)
							sb.append(Str.indent(level));
					}
				}
				sb.append(']');
			} else if (v instanceof Map) {
				var map = (Map<?, ?>)v;
				sb.append('{');
				if (!map.isEmpty()) {
					level += INDENT;
					for (var e2 : map.entrySet()) {
						sb.append('\n');
						if (level > 0)
							sb.append(Str.indent(level));
						var k2 = e2.getKey();
						var v2 = e2.getValue();
						buildString(sb, -1, k2);
						sb.append(':').append(' ');
						buildString(sb, level, v2);
					}
					sb.append('\n');
					level -= INDENT;
					if (level > 0)
						sb.append(Str.indent(level));
				}
				sb.append('}');
			} else if (v instanceof GenericBean)
				((GenericBean)v).buildString(sb, level);
			else
				sb.append(v);
			sb.append('\n');
		}
		if (level > 0)
			sb.append(Str.indent(level - INDENT));
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
	}
}
