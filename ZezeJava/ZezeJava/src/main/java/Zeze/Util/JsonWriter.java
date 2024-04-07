package Zeze.Util;

import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static Zeze.Util.Json.*;

public final class JsonWriter {
	//@formatter:off
	private static final long DOUBLE_SIGN_MASK        = 0x8000_0000_0000_0000L;
	private static final long DOUBLE_EXP_MASK         = 0x7FF0_0000_0000_0000L;
	private static final long DOUBLE_SIGNIFICAND_MASK = 0x000F_FFFF_FFFF_FFFFL; // low 52-bit
	private static final long DOUBLE_HIDDEN_BIT       = 0x0010_0000_0000_0000L; // 1e52
	private static final int  DOUBLE_SIGNIFICAND_SIZE = 52;
	private static final int  DOUBLE_EXP_SIZE         = 64 - DOUBLE_SIGNIFICAND_SIZE - 2; // 10
	private static final int  DOUBLE_EXP_BIAS         = 0x3FF + DOUBLE_SIGNIFICAND_SIZE;  // 0x433

	public static final int FLAG_PRETTY_FORMAT = 0x1;
	public static final int FLAG_NO_QUOTE_KEY  = 0x2;
	public static final int FLAG_WRITE_NULL    = 0x4;
	public static final int FLAG_WRAP_ELEMENT  = 0x8; // need FLAG_PRETTY_FORMAT
	public static final int FLAG_ALL           = 0xf;
	public static final int FLAG_PRETTY_FORMAT_AND_WRAP_ELEMENT = FLAG_PRETTY_FORMAT | FLAG_WRAP_ELEMENT;
	//@formatter:on

	private static final byte[] LONG_MIN_BYTES = "-9223372036854775808".getBytes(StandardCharsets.ISO_8859_1);

	private static final byte[] DIGITES_LUT = { // [200]
			'0', '0', '0', '1', '0', '2', '0', '3', '0', '4', '0', '5', '0', '6', '0', '7', '0', '8', '0', '9', '1',
			'0', '1', '1', '1', '2', '1', '3', '1', '4', '1', '5', '1', '6', '1', '7', '1', '8', '1', '9', '2', '0',
			'2', '1', '2', '2', '2', '3', '2', '4', '2', '5', '2', '6', '2', '7', '2', '8', '2', '9', '3', '0', '3',
			'1', '3', '2', '3', '3', '3', '4', '3', '5', '3', '6', '3', '7', '3', '8', '3', '9', '4', '0', '4', '1',
			'4', '2', '4', '3', '4', '4', '4', '5', '4', '6', '4', '7', '4', '8', '4', '9', '5', '0', '5', '1', '5',
			'2', '5', '3', '5', '4', '5', '5', '5', '6', '5', '7', '5', '8', '5', '9', '6', '0', '6', '1', '6', '2',
			'6', '3', '6', '4', '6', '5', '6', '6', '6', '7', '6', '8', '6', '9', '7', '0', '7', '1', '7', '2', '7',
			'3', '7', '4', '7', '5', '7', '6', '7', '7', '7', '8', '7', '9', '8', '0', '8', '1', '8', '2', '8', '3',
			'8', '4', '8', '5', '8', '6', '8', '7', '8', '8', '8', '9', '9', '0', '9', '1', '9', '2', '9', '3', '9',
			'4', '9', '5', '9', '6', '9', '7', '9', '8', '9', '9'};

	private static final long[] POW10 = {1, 10, 100, 1000, 1_0000, 10_0000, 100_0000, 1000_0000, 1_0000_0000,
			10_0000_0000, 100_0000_0000L, 1000_0000_0000L, 1_0000_0000_0000L, 10_0000_0000_0000L, 100_0000_0000_0000L,
			1000_0000_0000_0000L, 1_0000_0000_0000_0000L, 10_0000_0000_0000_0000L, 100_0000_0000_0000_0000L};

	private static final long[] CACHED_POWERS_F = {0xfa8fd5a0_081c0288L, 0xbaaee17f_a23ebf76L, 0x8b16fb20_3055ac76L,
			0xcf42894a_5dce35eaL, 0x9a6bb0aa_55653b2dL, 0xe61acf03_3d1a45dfL, 0xab70fe17_c79ac6caL,
			0xff77b1fc_bebcdc4fL, 0xbe5691ef_416bd60cL, 0x8dd01fad_907ffc3cL, 0xd3515c28_31559a83L,
			0x9d71ac8f_ada6c9b5L, 0xea9c2277_23ee8bcbL, 0xaecc4991_4078536dL, 0x823c1279_5db6ce57L,
			0xc2109436_4dfb5637L, 0x9096ea6f_3848984fL, 0xd77485cb_25823ac7L, 0xa086cfcd_97bf97f4L,
			0xef340a98_172aace5L, 0xb23867fb_2a35b28eL, 0x84c8d4df_d2c63f3bL, 0xc5dd4427_1ad3cdbaL,
			0x936b9fce_bb25c996L, 0xdbac6c24_7d62a584L, 0xa3ab6658_0d5fdaf6L, 0xf3e2f893_dec3f126L,
			0xb5b5ada8_aaff80b8L, 0x87625f05_6c7c4a8bL, 0xc9bcff60_34c13053L, 0x964e858c_91ba2655L,
			0xdff97724_70297ebdL, 0xa6dfbd9f_b8e5b88fL, 0xf8a95fcf_88747d94L, 0xb9447093_8fa89bcfL,
			0x8a08f0f8_bf0f156bL, 0xcdb02555_653131b6L, 0x993fe2c6_d07b7facL, 0xe45c10c4_2a2b3b06L,
			0xaa242499_697392d3L, 0xfd87b5f2_8300ca0eL, 0xbce50864_92111aebL, 0x8cbccc09_6f5088ccL,
			0xd1b71758_e219652cL, 0x9c400000_00000000L, 0xe8d4a510_00000000L, 0xad78ebc5_ac620000L,
			0x813f3978_f8940984L, 0xc097ce7b_c90715b3L, 0x8f7e32ce_7bea5c70L, 0xd5d238a4_abe98068L,
			0x9f4f2726_179a2245L, 0xed63a231_d4c4fb27L, 0xb0de6538_8cc8ada8L, 0x83c7088e_1aab65dbL,
			0xc45d1df9_42711d9aL, 0x924d692c_a61be758L, 0xda01ee64_1a708deaL, 0xa26da399_9aef774aL,
			0xf209787b_b47d6b85L, 0xb454e4a1_79dd1877L, 0x865b8692_5b9bc5c2L, 0xc83553c5_c8965d3dL,
			0x952ab45c_fa97a0b3L, 0xde469fbd_99a05fe3L, 0xa59bc234_db398c25L, 0xf6c69a72_a3989f5cL,
			0xb7dcbf53_54e9beceL, 0x88fcf317_f22241e2L, 0xcc20ce9b_d35c78a5L, 0x98165af3_7b2153dfL,
			0xe2a0b5dc_971f303aL, 0xa8d9d153_5ce3b396L, 0xfb9b7cd9_a4a7443cL, 0xbb764c4c_a7a44410L,
			0x8bab8eef_b6409c1aL, 0xd01fef10_a657842cL, 0x9b10a4e5_e9913129L, 0xe7109bfb_a19c0c9dL,
			0xac2820d9_623bf429L, 0x80444b5e_7aa7cf85L, 0xbf21e440_03acdd2dL, 0x8e679c2f_5e44ff8fL,
			0xd433179d_9c8cb841L, 0x9e19db92_b4e31ba9L, 0xeb96bf6e_badf77d9L, 0xaf87023b_9bf0ee6bL};

	private static final int[] CACHED_POWERS_E = {-1220, -1193, -1166, -1140, -1113, -1087, -1060, -1034, -1007, -980,
			-954, -927, -901, -874, -847, -821, -794, -768, -741, -715, -688, -661, -635, -608, -582, -555, -529, -502,
			-475, -449, -422, -396, -369, -343, -316, -289, -263, -236, -210, -183, -157, -130, -103, -77, -50, -24, 3,
			30, 56, 83, 109, 136, 162, 189, 216, 242, 269, 295, 322, 348, 375, 402, 428, 455, 481, 508, 534, 561, 588,
			614, 641, 667, 694, 720, 747, 774, 800, 827, 853, 880, 907, 933, 960, 986, 1013, 1039, 1066};

	private static final byte[] ESCAPE = { //@formatter:off
		//   0    1    2    3    4    5    6    7    8    9    A    B    C    D    E    F
			'u', 'u', 'u', 'u', 'u', 'u', 'u', 'u', 'b', 't', 'n', 'u', 'f', 'r', 'u', 'u', // 0x0x
			'u', 'u', 'u', 'u', 'u', 'u', 'u', 'u', 'u', 'u', 'u', 'u', 'u', 'u', 'u', 'u', // 0x1x
			 0 ,  0 , '"',  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 , // 0x2x
			 0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 , // 0x3x
			 0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 , // 0x4x
			 0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,'\\',  0 ,  0 ,  0 , // 0x5x
			 0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 , // 0x6x
			 0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 ,  0 , // 0x7x
	}; //@formatter:on

	private static final byte[] EMPTY = new byte[0];

	public interface BlockAllocator {
		@NotNull
		Block alloc(int minLen);

		default @NotNull Block alloc() {
			return alloc(4096);
		}

		default void free(@SuppressWarnings("unused") @NotNull Block block) {
		}
	}

	public static class Block {
		public byte[] buf;
		public int len; // current len of buf for non-tail
		Block next; // always non-null
	}

	private static final @NotNull ThreadLocal<JsonWriter> localWriters = ensureNotNull(
			ThreadLocal.withInitial(JsonWriter::new));

	public static @NotNull JsonWriter local() {
		return ensureNotNull(localWriters.get());
	}

	public static void removeLocal() {
		localWriters.remove();
	}

	private final @NotNull BlockAllocator allocator;
	private @NotNull Block tail; // the last block of circular linked list; head = tail.next
	private byte[] buf; // tail.buf
	private int pos; // current pos of buf
	private int size; // sum of len in all blocks except tail
	private int flags = 0x10_0000; // (depth=16) << 16
	private int tabs; // current depth

	public JsonWriter() {
		this(null);
	}

	public JsonWriter(@Nullable BlockAllocator allocator) {
		if (allocator == null) {
			allocator = minLen -> {
				Block block = new Block();
				block.buf = new byte[Math.max(minLen, 4096)];
				return block;
			};
		}
		this.allocator = allocator;
		Block block = allocator.alloc();
		block.next = block;
		tail = block;
		buf = block.buf;
	}

	public int getDepthLimit() {
		return flags >>> 16;
	}

	public @NotNull JsonWriter setDepthLimit(int depth) {
		flags = (flags & FLAG_ALL) | (depth << 16);
		return this;
	}

	public int getFlags() {
		return flags & FLAG_ALL;
	}

	public @NotNull JsonWriter setFlags(int flags) {
		this.flags = this.flags & ~FLAG_ALL | flags & FLAG_ALL;
		return this;
	}

	public @NotNull JsonWriter setFlagsAndDepthLimit(int flags, int depth) {
		this.flags = (flags & FLAG_ALL) | (depth << 16);
		return this;
	}

	public boolean isPrettyFormat() {
		return (flags & FLAG_PRETTY_FORMAT) != 0;
	}

	public @NotNull JsonWriter setPrettyFormat(boolean enable) {
		if (enable)
			flags |= FLAG_PRETTY_FORMAT;
		else
			flags &= ~FLAG_PRETTY_FORMAT;
		return this;
	}

	public boolean isNoQuoteKey() {
		return (flags & FLAG_NO_QUOTE_KEY) != 0;
	}

	public @NotNull JsonWriter setNoQuoteKey(boolean enable) {
		if (enable)
			flags |= FLAG_NO_QUOTE_KEY;
		else
			flags &= ~FLAG_NO_QUOTE_KEY;
		return this;
	}

	public boolean isWriteNull() {
		return (flags & FLAG_WRITE_NULL) != 0;
	}

	public @NotNull JsonWriter setWriteNull(boolean enable) {
		if (enable)
			flags |= FLAG_WRITE_NULL;
		else
			flags &= ~FLAG_WRITE_NULL;
		return this;
	}

	public boolean isWrapElement() {
		return (flags & FLAG_WRAP_ELEMENT) != 0;
	}

	public @NotNull JsonWriter setWrapElement(boolean enable) {
		if (enable)
			flags |= FLAG_WRAP_ELEMENT;
		else
			flags &= ~FLAG_WRAP_ELEMENT;
		return this;
	}

	public @NotNull JsonWriter clear() { // leave the empty head block
		Block head = ensureNotNull(tail.next);
		for (Block block = head.next; block != head; block = block.next)
			allocator.free(ensureNotNull(block));
		tail = head;
		head.next = head;
		buf = head.buf;
		pos = 0;
		size = 0;
		tabs = 0;
		return this;
	}

	public @NotNull JsonWriter free() { // can be reused by ensure()
		for (Block block = tail.next; ; block = block.next) {
			allocator.free(ensureNotNull(block));
			if (block == tail)
				break;
		}
		//noinspection ConstantConditions
		tail = null;
		buf = EMPTY;
		pos = 0;
		size = 0;
		tabs = 0;
		return this;
	}

	public int size() {
		return size + pos;
	}

	public int charSize() {
		int len = 0;
		tail.len = pos;
		for (Block block = tail.next; ; block = block.next) {
			byte[] buffer = block.buf;
			for (int i = 0, n = block.len; i < n; len++) {
				int b = buffer[i];
				if (b >= 0)
					i++;
				else if (b >= -0x20) {
					b = (b >> 4) & 1;
					i += 3 + b;
					len += b;
				} else
					i += 2;
			}
			if (block == tail)
				return len;
		}
	}

	public boolean hasWideChar() {
		tail.len = pos;
		for (Block block = tail.next; ; block = block.next) {
			byte[] buffer = block.buf;
			for (int i = 0, n = block.len; i < n; i++)
				if (buffer[i] < 0)
					return true;
			if (block == tail)
				return false;
		}
	}

	@SuppressWarnings({"null", "unused"})
	void appendBlock(int len) {
		Block block = allocator.alloc(len);
		//noinspection ConstantConditions,UnreachableCode
		if (tail != null) {
			block.next = tail.next;
			tail.next = block;
			tail.len = pos;
			size += pos;
			pos = 0;
		} else
			block.next = block;
		tail = block;
		buf = block.buf;
	}

	public void ensure(int len) {
		if (pos + len > buf.length)
			appendBlock(len);
	}

	public void writeByte(byte b) {
		ensure(1);
		buf[pos++] = b;
	}

	public void writeByteUnsafe(byte b) {
		buf[pos++] = b;
	}

	public void incTab() {
		tabs++;
	}

	public void decTab() {
		tabs--;
	}

	// ensure +2
	public void writeNewLineTabs() {
		int n = tabs;
		ensure(3 + n);
		buf[pos++] = '\n';
		for (int i = 0; i < n; i++)
			buf[pos++] = '\t';
	}

	// ensure +1
	public @NotNull JsonWriter write(@Nullable Object obj) {
		return write(Json.instance, obj);
	}

	// ensure +1
	public @NotNull JsonWriter write(@NotNull Json json, @Nullable Object obj) {
		if (obj == null) {
			ensure(5);
			buf[pos++] = 'n';
			buf[pos++] = 'u';
			buf[pos++] = 'l';
			buf[pos++] = 'l';
			return this;
		}
		boolean noQuote = (flags & FLAG_NO_QUOTE_KEY) != 0;
		boolean wrapArray = (flags & FLAG_PRETTY_FORMAT_AND_WRAP_ELEMENT) == FLAG_PRETTY_FORMAT_AND_WRAP_ELEMENT;
		Class<?> klass = obj.getClass();
		switch (ClassMeta.getType(klass)) {
		case TYPE_WRAP_FLAG + TYPE_BOOLEAN:
			if ((Boolean)obj) {
				ensure(5);
				buf[pos++] = 't';
				buf[pos++] = 'r';
				buf[pos++] = 'u';
			} else {
				ensure(6);
				buf[pos++] = 'f';
				buf[pos++] = 'a';
				buf[pos++] = 'l';
				buf[pos++] = 's';
			}
			buf[pos++] = 'e';
			break;
		case TYPE_WRAP_FLAG + TYPE_BYTE:
			ensure(5);
			write(((Byte)obj).byteValue());
			break;
		case TYPE_WRAP_FLAG + TYPE_SHORT:
			ensure(7);
			write(((Short)obj).shortValue());
			break;
		case TYPE_WRAP_FLAG + TYPE_CHAR:
			ensure(6);
			write(((Character)obj).charValue());
			break;
		case TYPE_WRAP_FLAG + TYPE_INT:
			ensure(12);
			write(((Integer)obj).intValue());
			break;
		case TYPE_WRAP_FLAG + TYPE_LONG:
			ensure(21);
			write(((Long)obj).longValue());
			break;
		case TYPE_WRAP_FLAG + TYPE_FLOAT:
			ensure(26);
			write(((Float)obj).floatValue());
			break;
		case TYPE_WRAP_FLAG + TYPE_DOUBLE:
			ensure(26);
			write(((Double)obj).doubleValue());
			break;
		case TYPE_STRING:
			String s = (String)obj;
			ensure(s.length() * 6 + 3); // "xxxxxx"
			write(s, false);
			break;
		case TYPE_POS:
			ensure(12);
			write(((Pos)obj).pos);
			break;
		case TYPE_OBJECT:
		case TYPE_CUSTOM:
			if (tabs >= getDepthLimit()) {
				ensure(14);
				buf[pos++] = '"';
				buf[pos++] = '!';
				buf[pos++] = 'O';
				buf[pos++] = 'V';
				buf[pos++] = 'E';
				buf[pos++] = 'R';
				buf[pos++] = 'D';
				buf[pos++] = 'E';
				buf[pos++] = 'P';
				buf[pos++] = 'T';
				buf[pos++] = 'H';
				buf[pos++] = '!';
				buf[pos++] = '"';
				break;
			}
			ClassMeta<?> classMeta = json.getClassMeta(klass);
			Writer<?> writer = classMeta.writer;
			if (writer != null) {
				writer.write0(this, classMeta, obj);
				break;
			}
			boolean comma = false;
			if (obj instanceof Collection) {
				ensure(1);
				buf[pos++] = '[';
				if (wrapArray) {
					tabs++;
					for (Object o : (Collection<?>)obj) {
						if (comma)
							buf[pos++] = ',';
						writeNewLineTabs();
						write(json, o);
						comma = true;
					}
					tabs--;
					if (comma)
						writeNewLineTabs();
					else
						ensure(2);
				} else {
					for (Object o : (Collection<?>)obj) {
						if (comma)
							buf[pos++] = ',';
						write(json, o);
						comma = true;
					}
					ensure(2);
				}
				buf[pos++] = ']';
				break;
			}
			if (klass.isArray()) {
				ensure(1);
				buf[pos++] = '[';
				if (wrapArray) {
					tabs++;
					for (int i = 0, n = Array.getLength(obj); i < n; i++) {
						if (comma)
							buf[pos++] = ',';
						writeNewLineTabs();
						write(json, Array.get(obj, i));
						comma = true;
					}
					tabs--;
					if (comma)
						writeNewLineTabs();
					else
						ensure(2);
				} else {
					for (int i = 0, n = Array.getLength(obj); i < n; i++) {
						if (comma)
							buf[pos++] = ',';
						write(json, Array.get(obj, i));
						comma = true;
					}
					ensure(2);
				}
				buf[pos++] = ']';
				break;
			}
			if (obj instanceof Map) {
				ensure(1);
				buf[pos++] = '{';
				if ((flags & FLAG_PRETTY_FORMAT) == 0) {
					for (Entry<?, ?> e : ((Map<?, ?>)obj).entrySet()) {
						Object value = e.getValue();
						if (value == null && (flags & FLAG_WRITE_NULL) == 0)
							continue;
						if (comma)
							buf[pos++] = ',';
						Object k = e.getKey();
						if (k == null || Json.ClassMeta.isInKeyReaderMap(k.getClass())) {
							s = String.valueOf(k);
							ensure(s.length() * 6 + 3); // "xxxxxx":
							write(s, noQuote && s.indexOf(':') < 0);
						} else {
							byte[] keyStr = new JsonWriter().setFlags(FLAG_NO_QUOTE_KEY).write(json, k).toBytes();
							ensure(keyStr.length * 6 + 3); // "xxxxxx":
							write(keyStr, false);
						}
						buf[pos++] = ':';
						write(json, value);
						comma = true;
					}
					ensure(2);
				} else {
					for (Entry<?, ?> e : ((Map<?, ?>)obj).entrySet()) {
						Object value = e.getValue();
						if (value == null && (flags & FLAG_WRITE_NULL) == 0)
							continue;
						if (comma)
							buf[pos++] = ',';
						else {
							tabs++;
							comma = true;
						}
						writeNewLineTabs();
						Object k = e.getKey();
						if (k == null || Json.ClassMeta.isInKeyReaderMap(k.getClass())) {
							s = String.valueOf(k);
							ensure(s.length() * 6 + 4); // "xxxxxx":_
							write(s, noQuote && s.indexOf(':') < 0);
						} else {
							byte[] keyStr = new JsonWriter().setFlags(FLAG_NO_QUOTE_KEY).write(json, k).toBytes();
							ensure(keyStr.length * 6 + 4); // "xxxxxx":_
							write(keyStr, false);
						}
						buf[pos++] = ':';
						buf[pos++] = ' ';
						write(json, value);
					}
					if (comma) {
						tabs--;
						writeNewLineTabs();
					} else
						ensure(2);
				}
				buf[pos++] = '}';
				break;
			}
			ensure(1);
			buf[pos++] = '{';
			tabs++;
			boolean prettyFormat = (flags & FLAG_PRETTY_FORMAT) != 0;
			boolean writeNull = (flags & FLAG_WRITE_NULL) != 0;
			for (FieldMeta fieldMeta : classMeta.fieldMetas) {
				Object subObj = null;
				int type = fieldMeta.type;
				long offset = fieldMeta.offset;
				if (type > TYPE_DOUBLE && (subObj = unsafe.getObject(obj, offset)) == null && !writeNull)
					continue;
				byte[] name = fieldMeta.name;
				int posBegin = pos;
				if (comma)
					buf[pos++] = ',';
				if (!prettyFormat) {
					ensure(name.length + 3); // "xxxxxx":
					write(name, noQuote);
					buf[pos++] = ':';
				} else {
					writeNewLineTabs();
					ensure(name.length + 4); // "xxxxxx":_
					write(name, noQuote);
					buf[pos++] = ':';
					buf[pos++] = ' ';
				}
				switch (type) {
				case TYPE_BOOLEAN:
					if (unsafe.getBoolean(obj, offset)) {
						ensure(5);
						buf[pos++] = 't';
						buf[pos++] = 'r';
						buf[pos++] = 'u';
					} else {
						ensure(6);
						buf[pos++] = 'f';
						buf[pos++] = 'a';
						buf[pos++] = 'l';
						buf[pos++] = 's';
					}
					buf[pos++] = 'e';
					break;
				case TYPE_BYTE:
					ensure(5);
					write(unsafe.getByte(obj, offset));
					break;
				case TYPE_SHORT:
					ensure(7);
					write(unsafe.getShort(obj, offset));
					break;
				case TYPE_CHAR:
					ensure(7);
					write(unsafe.getChar(obj, offset));
					break;
				case TYPE_INT:
					ensure(12);
					write(unsafe.getInt(obj, offset));
					break;
				case TYPE_LONG:
					ensure(21);
					write(unsafe.getLong(obj, offset));
					break;
				case TYPE_FLOAT:
					ensure(26);
					write(unsafe.getFloat(obj, offset));
					break;
				case TYPE_DOUBLE:
					ensure(26);
					write(unsafe.getDouble(obj, offset));
					break;
				case TYPE_STRING:
					if (subObj == null) {
						ensure(5);
						buf[pos++] = 'n';
						buf[pos++] = 'u';
						buf[pos++] = 'l';
						buf[pos++] = 'l';
						break;
					}
					s = (String)subObj;
					ensure(s.length() * 6 + 3); // "xxxxxx",
					write(s, false);
					break;
				case TYPE_POS:
					pos = posBegin;
					continue;
				default:
					write(json, subObj);
					break;
				}
				comma = true;
			}
			tabs--;
			if ((flags & FLAG_PRETTY_FORMAT) != 0 && comma)
				writeNewLineTabs();
			else
				ensure(2);
			buf[pos++] = '}';
		}
		return this;
	}

	public byte[] toBytes() {
		Block block = tail.next;
		if (block == tail)
			return Arrays.copyOf(buf, pos);
		tail.len = pos;
		int p = 0;
		for (; ; block = block.next) {
			p += block.len;
			if (block == tail)
				break;
		}
		byte[] res = new byte[p];
		p = 0;
		for (block = tail.next; ; block = block.next) {
			System.arraycopy(block.buf, 0, res, p, block.len);
			p += block.len;
			if (block == tail)
				return res;
		}
	}

	public char[] toChars() {
		char[] res = new char[charSize()];
		int p = 0;
		tail.len = pos;
		for (Block block = tail.next; ; block = block.next) {
			byte[] buffer = block.buf;
			for (int i = 0, n = block.len; i < n; ) {
				int b = buffer[i];
				if (b >= 0) { // 0xxx xxxx
					res[p++] = (char)b;
					i++;
				} else if (b >= -0x20) {
					if (b >= -0x10) { // 1111 0xxx  10xx xxxx  10xx xxxx  10xx xxxx
						b = (b << 18) + (buffer[i + 1] << 12) + (buffer[i + 2] << 6) + buffer[i + 3]
								+ ((0x10 << 18) + (0x80 << 12) + (0x80 << 6) + 0x80 - 0x10000);
						res[p++] = (char)(0xd800 + ((b >> 10) & 0x3ff));
						res[p++] = (char)(0xdc00 + (b & 0x3ff));
						i += 4;
					} else { // 1110 xxxx  10xx xxxx  10xx xxxx
						res[p++] = (char)((b << 12) + (buffer[i + 1] << 6) + buffer[i + 2]
								+ ((0x20 << 12) + (0x80 << 6) + 0x80));
						i += 3;
					}
				} else { // 110x xxxx  10xx xxxx
					res[p++] = (char)((b << 6) + buffer[i + 1] + ((0x40 << 6) + 0x80));
					i += 2;
				}
			}
			if (block == tail)
				return res;
		}
	}

	@Override
	public @NotNull String toString() {
		if (BYTE_STRING) { // for JDK9+
			byte[] bytes;
			int i, n;
			if (tail == tail.next) {
				bytes = buf;
				n = pos;
			} else {
				bytes = toBytes();
				n = bytes.length;
			}
			for (i = 0; i < n; i++)
				if (bytes[i] < 0)
					break;
			if (i == n) {
				try {
					String str = ensureNotNull((String)unsafe.allocateInstance(String.class));
					unsafe.putObject(str, STRING_VALUE_OFFSET, bytes == buf ? Arrays.copyOf(bytes, n) : bytes);
					return str;
				} catch (InstantiationException ignored) {
				}
			}
			return new String(bytes, 0, n, StandardCharsets.UTF_8);
		}
		char[] chars = toChars();
		try {
			String str = ensureNotNull((String)unsafe.allocateInstance(String.class));
			unsafe.putObject(str, STRING_VALUE_OFFSET, chars);
			return str;
		} catch (InstantiationException e) {
			return new String(chars);
		}
	}

	public static long umulHigh(long a, long b) { // for JDK8-
		long a1 = a >> 32;
		long a2 = a & 0xffff_ffffL;
		long b1 = b >> 32;
		long b2 = b & 0xffff_ffffL;
		long c2 = a2 * b2;
		long t = a1 * b2 + (c2 >>> 32);
		long c1 = t & 0xffff_ffffL;
		long c0 = t >> 32;
		c1 += a2 * b1;
		long mh = a1 * b1 + c0 + (c1 >> 32);
		mh += (b & (a >> 63));
		mh += (a & (b >> 63));
		return mh;
	}

	public static long umulHigh9(long a, long b) { // for JDK9+
		long r = Math.multiplyHigh(a, b);
		r += (b & (a >> 63));
		r += (a & (b >> 63));
		return r;
	}

	public static long umulHigh18(long a, long b) { // for JDK18+
		// return Math.unsignedMultiplyHigh(a, b);
		return umulHigh9(a, b); //TEMP: for JDK11 compatibility
	}

	void grisuRound(final int len, final long delta, long rest, final long tenKappa, final long mpf) {
		while (Long.compareUnsigned(rest, mpf) < 0 && Long.compareUnsigned(delta - rest, tenKappa) >= 0
				&& (Long.compareUnsigned(rest + tenKappa, mpf) < 0 || // closer
				Long.compareUnsigned(mpf - rest, rest + tenKappa - mpf) > 0)) {
			buf[pos + len - 1]--;
			rest += tenKappa;
		}
	}

	void grisu2(long f, final int maxDecimalPlaces) { // f = Double.doubleToRawLongBits(d)
		// {f,e}.reset(d)
		int e = (int)(f >>> DOUBLE_SIGNIFICAND_SIZE); // [0,0x3ff]
		f &= DOUBLE_SIGNIFICAND_MASK; // [0,1e52)
		if (e != 0) {
			f += DOUBLE_HIDDEN_BIT; // [1e52,1e53)
			e -= DOUBLE_EXP_BIAS; // [-0x432,-0x34]
		} else
			e = 1 - DOUBLE_EXP_BIAS; // -0x432 (1+DOUBLE_MIN_EXP)
		// {f,e}.normalizedBoundaries(mf/me, pf/pe);
		int pe = e - 1; // [-0x433,-0x35]
		long pf = (f << 1) + 1; // [1,1e54)
		//{ pf/e.normalizeBoundary(); // pf <<= Long.numberOfLeadingZeros(pf << DOUBLE_EXP_SIZE) + DOUBLE_EXP_SIZE;
		while ((pf & (DOUBLE_HIDDEN_BIT << 1)) == 0) { // max loop count: 53
			pe--;
			pf <<= 1;
		}
		pe -= DOUBLE_EXP_SIZE; // [-0x432-0x35-0xa=-0x3f3,-0x35-0xa=-0x3f]
		pf <<= DOUBLE_EXP_SIZE; // highest bit == 1
		//}
		long mf;
		final int me;
		if (f == DOUBLE_HIDDEN_BIT) {
			mf = (f << 2) - 1;
			me = e - 2;
		} else {
			mf = (f << 1) - 1;
			me = e - 1;
		}
		mf <<= me - pe;
		//}
		f <<= Long.numberOfLeadingZeros(f); // f.normalize(), highest bit == 1

		// getCachedPower(pe)
		// int k = static_cast<int>(ceil((-61 - e) * 0.30102999566398114)) + 374;
		final double dk = (-61 - pe) * 0.30102999566398114 + 347; // dk must be positive, so can do ceiling in positive
		int kk = (int)dk;
		if (dk - kk > 0)
			kk++;
		final int idx = (kk >> 3) + 1;
		kk = 348 - (idx << 3); // decimal exponent no need lookup table

		final long cmkf = CACHED_POWERS_F[idx]; // highest bit == 1
		if (javaVersion >= 18) { // for JDK18+
			f = umulHigh18(f, cmkf);
			pf = umulHigh18(pf, cmkf);
			mf = umulHigh18(mf, cmkf);
		} else if (javaVersion >= 9) { // for JDK9+
			f = umulHigh9(f, cmkf);
			pf = umulHigh9(pf, cmkf);
			mf = umulHigh9(mf, cmkf);
		} else { // for JDK8-
			f = umulHigh(f, cmkf);
			pf = umulHigh(pf, cmkf);
			mf = umulHigh(mf, cmkf);
		}
		e = -(pe + CACHED_POWERS_E[idx] + 64);
		long delta = pf-- - mf - 2;

		// digitGen(f, pf, e, delta)
		final long ff = 1L << e;
		int p1 = (int)(pf >>> e) & 0x7fff_ffff;
		long p2 = pf & (ff - 1), tmp;
		pf -= f;
		int len = 0, kappa, v; // kappa in [0, 9]
		// simple pure C++ implementation was faster than __builtin_clz version in this situation. @formatter:off
			 if (p1 <          10) kappa = 1;
		else if (p1 <         100) kappa = 2;
		else if (p1 <        1000) kappa = 3;
		else if (p1 <      1_0000) kappa = 4;
		else if (p1 <     10_0000) kappa = 5;
		else if (p1 <    100_0000) kappa = 6;
		else if (p1 <   1000_0000) kappa = 7;
		else if (p1 < 1_0000_0000) kappa = 8;
		else kappa = 9; // will not reach 10 digits: if (p1 < 10_0000_0000) kappa = 9; kappa = 10;
		// @formatter:on
		do {
			switch (kappa) { //@formatter:off
				case 1:  v = p1;               p1  =           0; break;
				case 2:  v = p1 /          10; p1 %=          10; break;
				case 3:  v = p1 /         100; p1 %=         100; break;
				case 4:  v = p1 /        1000; p1 %=        1000; break;
				case 5:  v = p1 /      1_0000; p1 %=      1_0000; break;
				case 6:  v = p1 /     10_0000; p1 %=     10_0000; break;
				case 7:  v = p1 /    100_0000; p1 %=    100_0000; break;
				case 8:  v = p1 /   1000_0000; p1 %=   1000_0000; break;
				case 9:  v = p1 / 1_0000_0000; p1 %= 1_0000_0000; break;
				default: continue;
			} //@formatter:on
			if ((v | len) != 0)
				buf[pos + len++] = (byte)('0' + v);
		} while (Long.compareUnsigned((tmp = ((long)p1 << e) + p2), delta) > 0 && --kappa > 0);
		if (kappa != 0) {
			kk += --kappa;
			grisuRound(len, delta, tmp, POW10[kappa] << e, pf);
		} else { // kappa == 0
			for (; ; ) {
				p2 *= 10;
				delta *= 10;
				final int b = (int)(p2 >>> e);
				if ((b | len) != 0)
					buf[pos + len++] = (byte)('0' + b);
				p2 &= ff - 1;
				kappa++;
				if (Long.compareUnsigned(p2, delta) < 0) {
					kk -= kappa;
					grisuRound(len, delta, p2, ff, kappa < 19 ? POW10[kappa] * pf : 0);
					break;
				}
			}
		}

		// prettify(len, maxDecimalPlaces)
		int k = len + kk; // 10^(k-1) <= v < 10^k
		if (k <= 21) {
			if (kk >= 0) { // 1234e7 -> 12340000000
				for (int i = len; i < k; i++)
					buf[pos + i] = '0';
				buf[pos + k] = '.';
				buf[pos + k + 1] = '0';
				pos += k + 2;
				return;
			}
			if (k > 0) { // 1234e-2 -> 12.34
				System.arraycopy(buf, pos + k, buf, pos + k + 1, len - k);
				buf[pos + k] = '.';
				if (kk + maxDecimalPlaces < 0) {
					// when maxDecimalPlaces = 2, 1.2345 -> 1.23, 1.102 -> 1.1
					// remove extra trailing zeros (at least one) after truncation.
					for (int i = k + maxDecimalPlaces; i > k + 1; i--)
						if (buf[pos + i] != '0') {
							pos += i + 1;
							return;
						}
					pos += k + 2; // reserve one zero
					return;
				}
				pos += len + 1;
				return;
			}
		}
		if (-6 < k && k <= 0) { // 1234e-6 -> 0.001234
			final int offset = 2 - k;
			System.arraycopy(buf, pos, buf, pos + offset, len);
			buf[pos] = '0';
			buf[pos + 1] = '.';
			for (int i = 2; i < offset; i++)
				buf[pos + i] = '0';
			if (len - k > maxDecimalPlaces) {
				// when maxDecimalPlaces = 2, 0.123 -> 0.12, 0.102 -> 0.1
				// remove extra trailing zeros (at least one) after truncation
				for (int i = maxDecimalPlaces + 1; i > 2; i--)
					if (buf[pos + i] != '0') {
						pos += i + 1;
						return;
					}
				pos += 3; // reserve one zero
				return;
			}
			pos += len + offset;
			return;
		}
		if (k < -maxDecimalPlaces) { // truncate to zero
			buf[pos++] = '0';
			buf[pos++] = '.';
			buf[pos++] = '0';
			return;
		}
		if (len == 1) { // 1e30
			buf[pos + 1] = 'e';
			pos += 2;
		} else { // 1234e30 -> 1.234e33
			System.arraycopy(buf, pos + 1, buf, pos + 2, len - 1);
			buf[pos + 1] = '.';
			buf[pos + len + 1] = 'e';
			pos += len + 2;
		}
		// writeExponent(--k)
		if (--k < 0) {
			buf[pos++] = '-';
			k = -k;
		}
		if (k < 10) {
			buf[pos++] = (byte)('0' + k);
			return;
		}
		if (k >= 100) {
			buf[pos++] = (byte)('0' + k / 100);
			k %= 100;
		}
		buf[pos++] = (byte)('0' + k / 10);
		buf[pos++] = (byte)('0' + k % 10);
	}

	public void write(final double d, final int maxDecimalPlaces) {
		long u = Double.doubleToRawLongBits(d);
		if ((u & (DOUBLE_EXP_MASK | DOUBLE_SIGNIFICAND_MASK)) == 0) { // d == 0
			if ((u & DOUBLE_SIGN_MASK) != 0) // d < 0
				buf[pos++] = '-';
			buf[pos++] = '0';
			buf[pos++] = '.';
			buf[pos++] = '0';
			return;
		}
		if ((u & DOUBLE_EXP_MASK) == DOUBLE_EXP_MASK) { // !Double.isFinite()
			if ((u & DOUBLE_SIGNIFICAND_MASK) == 0) { // Double.isInfinite()
				if ((u & DOUBLE_SIGN_MASK) != 0) // d < 0
					buf[pos++] = '-';
				buf[pos++] = 'I';
				buf[pos++] = 'n';
				buf[pos++] = 'f';
				buf[pos++] = 'i';
				buf[pos++] = 'n';
				buf[pos++] = 'i';
				buf[pos++] = 't';
				buf[pos++] = 'y';
			} else {
				buf[pos++] = 'N';
				buf[pos++] = 'a';
				buf[pos++] = 'N';
			}
			return;
		}
		if ((u & DOUBLE_SIGN_MASK) != 0) { // d < 0
			buf[pos++] = '-';
			u &= (DOUBLE_EXP_MASK | DOUBLE_SIGNIFICAND_MASK); // d = Math.abs(d)
		}
		grisu2(u, maxDecimalPlaces);
	}

	public void write(final double d) {
		write(d, 324);
	}

	public void write(int value) {
		if (value < 0) {
			if (value == Integer.MIN_VALUE) {
				write((long)value);
				return;
			}
			buf[pos++] = '-';
			value = -value;
		}
		if (value < 1_0000) {
			if (value < 10)
				buf[pos++] = (byte)('0' + value);
			else {
				final int d1 = (value / 100) << 1;
				final int d2 = (value % 100) << 1;
				if (value >= 100) {
					if (value >= 1000)
						buf[pos++] = DIGITES_LUT[d1];
					buf[pos++] = DIGITES_LUT[d1 + 1];
				}
				buf[pos++] = DIGITES_LUT[d2];
				buf[pos++] = DIGITES_LUT[d2 + 1];
			}
		} else if (value < 1_0000_0000) { // value = bbbb_cccc
			final int b = value / 1_0000;
			final int c = value % 1_0000;
			final int d3 = (c / 100) << 1;
			final int d4 = (c % 100) << 1;
			if (value < 10_0000)
				buf[pos++] = (byte)('0' + b);
			else {
				final int d1 = (b / 100) << 1;
				final int d2 = (b % 100) << 1;
				if (value >= 100_0000) {
					if (value >= 1000_0000)
						buf[pos++] = DIGITES_LUT[d1];
					buf[pos++] = DIGITES_LUT[d1 + 1];
				}
				buf[pos++] = DIGITES_LUT[d2];
				buf[pos++] = DIGITES_LUT[d2 + 1];
			}
			buf[pos++] = DIGITES_LUT[d3];
			buf[pos++] = DIGITES_LUT[d3 + 1];
			buf[pos++] = DIGITES_LUT[d4];
			buf[pos++] = DIGITES_LUT[d4 + 1];
		} else { // value = aa_bbbb_cccc in decimal
			final int a = value / 1_0000_0000; // [1,21]
			value %= 1_0000_0000;
			final int b = value / 1_0000;
			final int c = value % 1_0000;
			final int d1 = (b / 100) << 1;
			final int d2 = (b % 100) << 1;
			final int d3 = (c / 100) << 1;
			final int d4 = (c % 100) << 1;
			if (a < 10)
				buf[pos++] = (byte)('0' + a);
			else {
				final int i = a << 1;
				buf[pos++] = DIGITES_LUT[i];
				buf[pos++] = DIGITES_LUT[i + 1];
			}
			buf[pos++] = DIGITES_LUT[d1];
			buf[pos++] = DIGITES_LUT[d1 + 1];
			buf[pos++] = DIGITES_LUT[d2];
			buf[pos++] = DIGITES_LUT[d2 + 1];
			buf[pos++] = DIGITES_LUT[d3];
			buf[pos++] = DIGITES_LUT[d3 + 1];
			buf[pos++] = DIGITES_LUT[d4];
			buf[pos++] = DIGITES_LUT[d4 + 1];
		}
	}

	public void write(long value) { // 7FFF_FFFF_FFFF_FFFF = 922_3372_0368_5477_5807
		if (value < 0) {
			if (value == Long.MIN_VALUE) {
				System.arraycopy(LONG_MIN_BYTES, 0, buf, pos, 20);
				pos += 20;
				return;
			}
			buf[pos++] = '-';
			value = -value;
		}
		if (value < 1_0000_0000) {
			int v = (int)value;
			if (v < 1_0000) {
				if (v < 10)
					buf[pos++] = (byte)('0' + v);
				else {
					final int d1 = (v / 100) << 1;
					final int d2 = (v % 100) << 1;
					if (v >= 100) {
						if (v >= 1000)
							buf[pos++] = DIGITES_LUT[d1];
						buf[pos++] = DIGITES_LUT[d1 + 1];
					}
					buf[pos++] = DIGITES_LUT[d2];
					buf[pos++] = DIGITES_LUT[d2 + 1];
				}
			} else { // value = bbbb_cccc
				final int b = v / 1_0000;
				final int c = v % 1_0000;
				final int d3 = (c / 100) << 1;
				final int d4 = (c % 100) << 1;
				if (v < 10_0000)
					buf[pos++] = (byte)('0' + b);
				else {
					final int d1 = (b / 100) << 1;
					final int d2 = (b % 100) << 1;
					if (v >= 100_0000) {
						if (v >= 1000_0000)
							buf[pos++] = DIGITES_LUT[d1];
						buf[pos++] = DIGITES_LUT[d1 + 1];
					}
					buf[pos++] = DIGITES_LUT[d2];
					buf[pos++] = DIGITES_LUT[d2 + 1];
				}
				buf[pos++] = DIGITES_LUT[d3];
				buf[pos++] = DIGITES_LUT[d3 + 1];
				buf[pos++] = DIGITES_LUT[d4];
				buf[pos++] = DIGITES_LUT[d4 + 1];
			}
		} else if (value < 1_0000_0000_0000_0000L) {
			final int v0 = (int)(value / 1_0000_0000);
			final int v1 = (int)(value % 1_0000_0000);
			final int b1 = v1 / 1_0000;
			final int c1 = v1 % 1_0000;
			final int d5 = (b1 / 100) << 1;
			final int d6 = (b1 % 100) << 1;
			final int d7 = (c1 / 100) << 1;
			final int d8 = (c1 % 100) << 1;
			if (value < 10_0000_0000L)
				buf[pos++] = (byte)('0' + v0);
			else {
				final int d4 = (v0 % 100) << 1;
				if (value >= 100_0000_0000L) {
					final int v2 = v0 / 100;
					final int d3 = (v2 % 100) << 1;
					if (value >= 1000_0000_0000L) {
						if (value >= 1_0000_0000_0000L) {
							final int v3 = v2 / 100;
							final int d2 = (v3 % 100) << 1;
							if (value >= 10_0000_0000_0000L) {
								if (value >= 100_0000_0000_0000L) {
									final int d1 = (v3 / 100 % 100) << 1;
									if (value >= 1000_0000_0000_0000L)
										buf[pos++] = DIGITES_LUT[d1];
									buf[pos++] = DIGITES_LUT[d1 + 1];
								}
								buf[pos++] = DIGITES_LUT[d2];
							}
							buf[pos++] = DIGITES_LUT[d2 + 1];
						}
						buf[pos++] = DIGITES_LUT[d3];
					}
					buf[pos++] = DIGITES_LUT[d3 + 1];
				}
				buf[pos++] = DIGITES_LUT[d4];
				buf[pos++] = DIGITES_LUT[d4 + 1];
			}
			buf[pos++] = DIGITES_LUT[d5];
			buf[pos++] = DIGITES_LUT[d5 + 1];
			buf[pos++] = DIGITES_LUT[d6];
			buf[pos++] = DIGITES_LUT[d6 + 1];
			buf[pos++] = DIGITES_LUT[d7];
			buf[pos++] = DIGITES_LUT[d7 + 1];
			buf[pos++] = DIGITES_LUT[d8];
			buf[pos++] = DIGITES_LUT[d8 + 1];
		} else {
			final int a = (int)(value / 1_0000_0000_0000_0000L); // [1,922]
			value %= 1_0000_0000_0000_0000L;
			final int v0 = (int)(value / 1_0000_0000);
			final int v1 = (int)(value % 1_0000_0000);
			final int b0 = v0 / 1_0000;
			final int c0 = v0 % 1_0000;
			final int d1 = (b0 / 100) << 1;
			final int d2 = (b0 % 100) << 1;
			final int d3 = (c0 / 100) << 1;
			final int d4 = (c0 % 100) << 1;
			final int b1 = v1 / 1_0000;
			final int c1 = v1 % 1_0000;
			final int d5 = (b1 / 100) << 1;
			final int d6 = (b1 % 100) << 1;
			final int d7 = (c1 / 100) << 1;
			final int d8 = (c1 % 100) << 1;
			if (a < 10)
				buf[pos++] = (byte)('0' + a);
			else if (a < 100) {
				final int i = a << 1;
				buf[pos++] = DIGITES_LUT[i];
				buf[pos++] = DIGITES_LUT[i + 1];
			} else {
				buf[pos++] = (byte)('0' + a / 100);
				final int i = (a % 100) << 1;
				buf[pos++] = DIGITES_LUT[i];
				buf[pos++] = DIGITES_LUT[i + 1];
			}
			buf[pos++] = DIGITES_LUT[d1];
			buf[pos++] = DIGITES_LUT[d1 + 1];
			buf[pos++] = DIGITES_LUT[d2];
			buf[pos++] = DIGITES_LUT[d2 + 1];
			buf[pos++] = DIGITES_LUT[d3];
			buf[pos++] = DIGITES_LUT[d3 + 1];
			buf[pos++] = DIGITES_LUT[d4];
			buf[pos++] = DIGITES_LUT[d4 + 1];
			buf[pos++] = DIGITES_LUT[d5];
			buf[pos++] = DIGITES_LUT[d5 + 1];
			buf[pos++] = DIGITES_LUT[d6];
			buf[pos++] = DIGITES_LUT[d6 + 1];
			buf[pos++] = DIGITES_LUT[d7];
			buf[pos++] = DIGITES_LUT[d7 + 1];
			buf[pos++] = DIGITES_LUT[d8];
			buf[pos++] = DIGITES_LUT[d8 + 1];
		}
	}

	public static int num2Hex(int n) {
		return n + '0' + (((9 - n) >> 31) & ('A' - '9' - 1));
	}

	public void write(final byte[] str, final boolean noQuote) {
		write(str, 0, str.length, noQuote);
	}

	public void write(final byte[] str, int p, int n, final boolean noQuote) {
		if (!noQuote)
			buf[pos++] = '"';
		int i = p, q = p + n, c;
		for (byte b; i < q; ) {
			if ((c = str[i++]) >= 0 && (b = ESCAPE[c]) != 0) {
				if ((n = i - p - 1) > 0) {
					System.arraycopy(str, p, buf, pos, n);
					pos += n;
				}
				p = i;
				buf[pos++] = '\\';
				buf[pos++] = b;
				if (b == 'u') {
					buf[pos++] = '0';
					buf[pos++] = '0';
					buf[pos++] = (byte)('0' + (c >> 4));
					buf[pos++] = (byte)num2Hex(c & 0xf);
				}
			}
		}
		if ((n = i - p) > 0) {
			System.arraycopy(str, p, buf, pos, n);
			pos += n;
		}
		if (!noQuote)
			buf[pos++] = '"';
	}

	public void write(final @NotNull String str, final boolean noQuote) {
		if (!noQuote)
			buf[pos++] = '"';
		if (BYTE_STRING && unsafe.getByte(str, STRING_CODE_OFFSET) == 0) // for JDK9+
			writeLatin1((byte[])unsafe.getObject(str, STRING_VALUE_OFFSET));
		else // for JDK8-
			write8(str);
		if (!noQuote)
			buf[pos++] = '"';
	}

	private void writeLatin1(final byte @NotNull [] str) {
		for (int c : str) {
			if (c >= 0) {
				byte b = ESCAPE[c];
				if (b == 0)
					buf[pos++] = (byte)c; // 0xxx xxxx
				else {
					buf[pos++] = '\\';
					buf[pos++] = b;
					if (b == 'u') {
						buf[pos++] = '0';
						buf[pos++] = '0';
						buf[pos++] = (byte)('0' + (c >> 4));
						buf[pos++] = (byte)num2Hex(c & 0xf);
					}
				}
			} else {
				buf[pos++] = (byte)(0xc0 + ((c >> 6) & 3)); // 110x xxxx  10xx xxxx
				buf[pos++] = (byte)(0x80 + (c & 0x3f));
			}
		}
	}

	private void write8(final @NotNull String str) {
		for (int i = 0, n = str.length(), d; i < n; i++) {
			int c = str.charAt(i);
			if (c < 0x80) {
				byte b = ESCAPE[c];
				if (b == 0)
					buf[pos++] = (byte)c; // 0xxx xxxx
				else {
					buf[pos++] = '\\';
					buf[pos++] = b;
					if (b == 'u') {
						buf[pos++] = '0';
						buf[pos++] = '0';
						buf[pos++] = (byte)('0' + (c >> 4));
						buf[pos++] = (byte)num2Hex(c & 0xf);
					}
				}
			} else {
				if (c < 0x800)
					buf[pos++] = (byte)(0xc0 + (c >> 6)); // 110x xxxx  10xx xxxx
				else {
					if ((c & 0xfc00) == 0xd800 && i + 1 < n && ((d = str.charAt(i + 1)) & 0xfc00) == 0xdc00) { // UTF-16 surrogate
						c = (c << 10) + d + (0x10000 - (0xd800 << 10) - 0xdc00);
						i++;
						buf[pos++] = (byte)(0xf0 + (c >> 18)); // 1111 0xxx  10xx xxxx  10xx xxxx  10xx xxxx
						buf[pos++] = (byte)(0x80 + ((c >> 12) & 0x3f));
					} else
						buf[pos++] = (byte)(0xe0 + (c >> 12)); // 1110 xxxx  10xx xxxx  10xx xxxx
					buf[pos++] = (byte)(0x80 + ((c >> 6) & 0x3f));
				}
				buf[pos++] = (byte)(0x80 + (c & 0x3f));
			}
		}
	}
}
