package Zeze.Util;

import java.util.Collection;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Vector2Int;
import org.jetbrains.annotations.NotNull;

/**
 * 用Vector2IntList要记得它本质就是IntList,除了下面多出一些专用方法外,都以int数组的方式操作.
 * 如果int数组长度不被2整除,则以Vector2Int为单位处理时忽略结尾多余的int值.
 */
public class Vector2IntList extends IntList {
	public static @NotNull Vector2IntList wrap(int @NotNull [] data, int count) {
		Vector2IntList o = new Vector2IntList();
		o.buffer = data;
		o.count = count > data.length ? data.length : Math.max(count, 0);
		return o;
	}

	public static @NotNull Vector2IntList wrap(int @NotNull [] data) {
		Vector2IntList o = new Vector2IntList();
		o.buffer = data;
		o.count = data.length;
		return o;
	}

	public static @NotNull Vector2IntList createSpace(int count) {
		Vector2IntList o = new Vector2IntList();
		if (count > 0) {
			o.buffer = new int[count];
			o.count = count;
		}
		return o;
	}

	public Vector2IntList() {
	}

	public Vector2IntList(int count) {
		reserveSpace(count);
	}

	public Vector2IntList(@NotNull IntList fl) {
		replace(fl);
	}

	public Vector2IntList(int @NotNull [] data) {
		replace(data);
	}

	public Vector2IntList(int @NotNull [] data, int fromIdx, int count) {
		replace(data, fromIdx, count);
	}

	public int vectorSize() {
		return count >> 1;
	}

	public int vectorCapacity() {
		return buffer.length >> 1;
	}

	public int getX(int idx) {
		return buffer[idx * 2];
	}

	public int getY(int idx) {
		return buffer[idx * 2 + 1];
	}

	public Vector2Int getVector(int idx) {
		int[] buf = buffer;
		idx *= 2;
		return new Vector2Int(buf[idx], buf[idx + 1]);
	}

	public void setX(int idx, int x) {
		buffer[idx * 2] = x;
	}

	public void setY(int idx, int y) {
		buffer[idx * 2 + 1] = y;
	}

	public void set(int idx, int x, int y) {
		int[] buf = buffer;
		idx *= 2;
		buf[idx] = x;
		buf[idx + 1] = y;
	}

	public int addValueX(int idx, int x) {
		int[] buf = buffer;
		idx *= 2;
		buf[idx] = x += buf[idx];
		return x;
	}

	public int addValueY(int idx, int y) {
		int[] buf = buffer;
		idx = idx * 2 + 1;
		buf[idx] = y += buf[idx];
		return y;
	}

	public void addValue(int idx, int x, int y) {
		int[] buf = buffer;
		idx *= 2;
		buf[idx] += x;
		buf[idx + 1] += y;
	}

	public int @NotNull [] toArrayVector(int fromIdx, int count) {
		return toArray(fromIdx * 2, count * 2);
	}

	public @NotNull Vector2IntList wrapsVector(int @NotNull [] data, int count) {
		super.wraps(data, count * 2);
		return this;
	}

	@Override
	public @NotNull Vector2IntList wraps(int @NotNull [] data) {
		super.wraps(data);
		return this;
	}

	public void shrinkVector(int count) {
		shrink(count * 2);
	}

	public void reserveVector(int count) {
		reserve(count * 2);
	}

	public void reserveSpaceVector(int count) {
		reserveSpace(count * 2);
	}

	public void resizeVector(int count) {
		resize(count * 2);
	}

	public void replaceVector(int @NotNull [] data, int fromIdx, int count) {
		replace(data, fromIdx * 2, count * 2);
	}

	public @NotNull Vector2IntList add(int x, int y) {
		int n = count;
		int nNew = n + 2;
		reserve(nNew);
		int[] buf = buffer;
		buf[n] = x;
		buf[n + 1] = y;
		count = nNew;
		return this;
	}

	public @NotNull Vector2IntList add(Vector2Int v) {
		return add(v.x, v.y);
	}

	@Override
	public @NotNull Vector2IntList addAll(int[] data, int fromIdx, int count) {
		super.addAll(data, fromIdx, count);
		return this;
	}

	@Override
	public @NotNull Vector2IntList addAll(int @NotNull [] data) {
		super.addAll(data);
		return this;
	}

	@Override
	public @NotNull Vector2IntList addAll(@NotNull IntList fl) {
		super.addAll(fl);
		return this;
	}

	@Override
	public @NotNull Vector2IntList addAll(@NotNull Collection<Integer> c) {
		super.addAll(c);
		return this;
	}

	public @NotNull Vector2IntList addAllVector(@NotNull Collection<Vector2Int> c) {
		int n = count;
		reserve(n + c.size() * 2);
		int[] buf = buffer;
		for (Vector2Int v : c) {
			buf[n++] = v.x;
			buf[n++] = v.y;
		}
		count = n;
		return this;
	}

	public @NotNull void addAllToVector(@NotNull Collection<Vector2Int> c) {
		int[] buf = buffer;
		for (int i = 0, n = count - 1; i < n; i += 2)
			c.add(new Vector2Int(buf[i], buf[i + 1]));
	}

	public @NotNull Vector2IntList insertVector(int fromIdx, int x, int y) {
		fromIdx *= 2;
		int n = count;
		if (fromIdx < 0)
			fromIdx = 0;
		if (fromIdx >= n)
			return add(x, y);
		reserve(n + 2);
		int[] buf = buffer;
		System.arraycopy(buf, fromIdx, buf, fromIdx + 2, n - fromIdx);
		buf[fromIdx] = x;
		buf[fromIdx + 1] = y;
		count = n + 2;
		return this;
	}

	@Override
	public @NotNull Vector2IntList insert(int fromIdx, int @NotNull [] data, int idx, int count) {
		super.insert(fromIdx, data, idx, count);
		return this;
	}

	@Override
	public @NotNull Vector2IntList insert(int fromIdx, int @NotNull [] data) {
		super.insert(fromIdx, data);
		return this;
	}

	@Override
	public @NotNull Vector2IntList insert(int fromIdx, @NotNull IntList fl) {
		super.insert(fromIdx, fl);
		return this;
	}

	public @NotNull Vector2IntList removeVector(int idx) {
		idx *= 2;
		int lastIdx = count - 2;
		if (idx < 0 || idx > lastIdx)
			return this;
		count = lastIdx;
		if (idx != lastIdx)
			System.arraycopy(buffer, idx + 2, buffer, idx, lastIdx - idx);
		return this;
	}

	public @NotNull Vector2IntList removeAndExchangeLastVector(int idx) {
		idx *= 2;
		int lastIdx = count - 2;
		if (idx >= 0 && idx <= lastIdx) {
			int[] buf = buffer;
			count = lastIdx;
			buf[idx] = buf[lastIdx];
			buf[idx + 1] = buf[lastIdx + 1];
		}
		return this;
	}

	public @NotNull Vector2IntList eraseVector(int fromIdx, int toIdx) {
		super.erase(fromIdx * 2, toIdx * 2);
		return this;
	}

	public @NotNull Vector2IntList eraseFrontVector(int count) {
		super.eraseFront(count * 2);
		return this;
	}

	public int indexOfVector(int x, int y, int z) {
		return indexOfVector(x, y, z, 0);
	}

	public int indexOfVector(int x, int y, int z, int fromIdx) {
		int[] buf = buffer;
		for (int i = fromIdx * 2, n = count - 1; i < n; i += 2) {
			if (buf[i] == x && buf[i + 1] == y && buf[i + 2] == z)
				return i >> 1;
		}
		return -1;
	}

	@SuppressWarnings("MethodDoesntCallSuperMethod")
	@Override
	public @NotNull Vector2IntList clone() {
		return new Vector2IntList(this);
	}

	public interface Vector2IntConsumer {
		void accept(int x, int y);
	}

	public void foreach(@NotNull Vector2IntConsumer consumer) {
		int[] buf = buffer;
		for (int i = 0, n = count - 1; i < n; i += 2)
			consumer.accept(buf[i], buf[i + 1]);
	}

	public interface Vector2IntPredicate {
		boolean test(int x, int y);
	}

	public boolean foreachPred(@NotNull Vector2IntPredicate predicate) {
		int[] buf = buffer;
		for (int i = 0, n = count - 1; i < n; i += 2) {
			if (!predicate.test(buf[i], buf[i + 1]))
				return false;
		}
		return true;
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		int n = count >> 1;
		bb.WriteUInt(n);
		if (n > 0) {
			n *= 2;
			int[] buf = buffer;
			for (int i = 0; i < n; i++)
				bb.WriteInt(buf[i]);
		}
	}

	@Override
	public void encode(@NotNull ByteBuffer bb, int n) {
		int count = this.count >> 1;
		if (count != n)
			throw new java.util.ConcurrentModificationException(String.valueOf(count));
		if (n > 0) {
			n *= 2;
			int[] buf = buffer;
			for (int i = 0; i < n; i++)
				bb.WriteInt(buf[i]);
		}
	}

	@Override
	public void decode(@NotNull ByteBuffer bb, int n) {
		super.decode(bb, n * 2);
	}
}
