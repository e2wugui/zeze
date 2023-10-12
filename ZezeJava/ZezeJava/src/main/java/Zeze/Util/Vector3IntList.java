package Zeze.Util;

import java.util.Collection;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Vector3Int;
import org.jetbrains.annotations.NotNull;

/**
 * 用Vector3IntList要记得它本质就是IntList,除了下面多出一些专用方法外,都以int数组的方式操作.
 * 如果int数组长度不被3整除,则以Vector3Int为单位处理时忽略结尾多余的int值.
 */
public class Vector3IntList extends IntList {
	public static @NotNull Vector3IntList wrap(int @NotNull [] data, int count) {
		Vector3IntList o = new Vector3IntList();
		o._buffer = data;
		o._count = count > data.length ? data.length : Math.max(count, 0);
		return o;
	}

	public static @NotNull Vector3IntList wrap(int @NotNull [] data) {
		Vector3IntList o = new Vector3IntList();
		o._buffer = data;
		o._count = data.length;
		return o;
	}

	public static @NotNull Vector3IntList createSpace(int count) {
		Vector3IntList o = new Vector3IntList();
		if (count > 0) {
			o._buffer = new int[count];
			o._count = count;
		}
		return o;
	}

	public Vector3IntList() {
	}

	public Vector3IntList(int count) {
		reserveSpace(count);
	}

	public Vector3IntList(@NotNull IntList fl) {
		replace(fl);
	}

	public Vector3IntList(int @NotNull [] data) {
		replace(data);
	}

	public Vector3IntList(int @NotNull [] data, int fromIdx, int count) {
		replace(data, fromIdx, count);
	}

	public int vectorSize() {
		return _count / 3;
	}

	public int vectorCapacity() {
		return _buffer.length / 3;
	}

	public int getX(int idx) {
		return _buffer[idx * 3];
	}

	public int getY(int idx) {
		return _buffer[idx * 3 + 1];
	}

	public int getZ(int idx) {
		return _buffer[idx * 3 + 2];
	}

	public Vector3Int getVector(int idx) {
		int[] buf = _buffer;
		idx *= 3;
		return new Vector3Int(buf[idx], buf[idx + 1], buf[idx + 2]);
	}

	public void setX(int idx, int x) {
		_buffer[idx * 3] = x;
	}

	public void setY(int idx, int y) {
		_buffer[idx * 3 + 1] = y;
	}

	public void setZ(int idx, int z) {
		_buffer[idx * 3 + 2] = z;
	}

	public void set(int idx, int x, int y, int z) {
		int[] buf = _buffer;
		idx *= 3;
		buf[idx] = x;
		buf[idx + 1] = y;
		buf[idx + 2] = z;
	}

	public int addValueX(int idx, int x) {
		int[] buf = _buffer;
		idx *= 3;
		buf[idx] = x += buf[idx];
		return x;
	}

	public int addValueY(int idx, int y) {
		int[] buf = _buffer;
		idx = idx * 3 + 1;
		buf[idx] = y += buf[idx];
		return y;
	}

	public int addValueZ(int idx, int z) {
		int[] buf = _buffer;
		idx = idx * 3 + 2;
		buf[idx] = z += buf[idx];
		return z;
	}

	public void addValue(int idx, int x, int y, int z) {
		int[] buf = _buffer;
		idx *= 3;
		buf[idx] += x;
		buf[idx + 1] += y;
		buf[idx + 2] += z;
	}

	public int @NotNull [] toArrayVector(int fromIdx, int count) {
		return toArray(fromIdx * 3, count * 3);
	}

	public @NotNull Vector3IntList wrapsVector(int @NotNull [] data, int count) {
		super.wraps(data, count * 3);
		return this;
	}

	@Override
	public @NotNull Vector3IntList wraps(int @NotNull [] data) {
		super.wraps(data);
		return this;
	}

	public void shrinkVector(int count) {
		shrink(count * 3);
	}

	public void reserveVector(int count) {
		reserve(count * 3);
	}

	public void reserveSpaceVector(int count) {
		reserveSpace(count * 3);
	}

	public void resizeVector(int count) {
		resize(count * 3);
	}

	public void replaceVector(int @NotNull [] data, int fromIdx, int count) {
		replace(data, fromIdx * 3, count * 3);
	}

	public @NotNull Vector3IntList add(int x, int y, int z) {
		int n = _count;
		int nNew = n + 3;
		reserve(nNew);
		int[] buf = _buffer;
		buf[n] = x;
		buf[n + 1] = y;
		buf[n + 2] = z;
		_count = nNew;
		return this;
	}

	public @NotNull Vector3IntList add(Vector3Int v) {
		return add(v.x, v.y, v.z);
	}

	@Override
	public @NotNull Vector3IntList addAll(int[] data, int fromIdx, int count) {
		super.addAll(data, fromIdx, count);
		return this;
	}

	@Override
	public @NotNull Vector3IntList addAll(int @NotNull [] data) {
		super.addAll(data);
		return this;
	}

	@Override
	public @NotNull Vector3IntList addAll(@NotNull IntList fl) {
		super.addAll(fl);
		return this;
	}

	@Override
	public @NotNull Vector3IntList addAll(@NotNull Collection<Integer> c) {
		super.addAll(c);
		return this;
	}

	public @NotNull Vector3IntList addAllVector(@NotNull Collection<Vector3Int> c) {
		int n = _count;
		reserve(n + c.size() * 3);
		int[] buf = _buffer;
		for (Vector3Int v : c) {
			buf[n++] = v.x;
			buf[n++] = v.y;
			buf[n++] = v.z;
		}
		_count = n;
		return this;
	}

	public @NotNull void addAllToVector(@NotNull Collection<Vector3Int> c) {
		int[] buf = _buffer;
		for (int i = 0, n = _count - 2; i < n; i += 3)
			c.add(new Vector3Int(buf[i], buf[i + 1], buf[i + 2]));
	}

	public @NotNull Vector3IntList insertVector(int fromIdx, int x, int y, int z) {
		fromIdx *= 3;
		int n = _count;
		if (fromIdx < 0)
			fromIdx = 0;
		if (fromIdx >= n)
			return add(x, y, z);
		reserve(n + 3);
		int[] buf = _buffer;
		System.arraycopy(buf, fromIdx, buf, fromIdx + 3, n - fromIdx);
		buf[fromIdx] = x;
		buf[fromIdx + 1] = y;
		buf[fromIdx + 2] = z;
		_count = n + 3;
		return this;
	}

	@Override
	public @NotNull Vector3IntList insert(int fromIdx, int @NotNull [] data, int idx, int count) {
		super.insert(fromIdx, data, idx, count);
		return this;
	}

	@Override
	public @NotNull Vector3IntList insert(int fromIdx, int @NotNull [] data) {
		super.insert(fromIdx, data);
		return this;
	}

	@Override
	public @NotNull Vector3IntList insert(int fromIdx, @NotNull IntList fl) {
		super.insert(fromIdx, fl);
		return this;
	}

	public @NotNull Vector3IntList removeVector(int idx) {
		idx *= 3;
		int lastIdx = _count - 3;
		if (idx < 0 || idx > lastIdx)
			return this;
		_count = lastIdx;
		if (idx != lastIdx)
			System.arraycopy(_buffer, idx + 3, _buffer, idx, lastIdx - idx);
		return this;
	}

	public @NotNull Vector3IntList removeAndExchangeLastVector(int idx) {
		idx *= 3;
		int lastIdx = _count - 3;
		if (idx >= 0 && idx <= lastIdx) {
			int[] buf = _buffer;
			_count = lastIdx;
			buf[idx] = buf[lastIdx];
			buf[idx + 1] = buf[lastIdx + 1];
			buf[idx + 2] = buf[lastIdx + 2];
		}
		return this;
	}

	public @NotNull Vector3IntList eraseVector(int fromIdx, int toIdx) {
		super.erase(fromIdx * 3, toIdx * 3);
		return this;
	}

	public @NotNull Vector3IntList eraseFrontVector(int count) {
		super.eraseFront(count * 3);
		return this;
	}

	public int indexOfVector(int x, int y, int z) {
		return indexOfVector(x, y, z, 0);
	}

	public int indexOfVector(int x, int y, int z, int fromIdx) {
		int[] buf = _buffer;
		for (int i = fromIdx * 3, n = _count - 2; i < n; i += 3) {
			if (buf[i] == x && buf[i + 1] == y && buf[i + 2] == z)
				return i / 3;
		}
		return -1;
	}

	@SuppressWarnings("MethodDoesntCallSuperMethod")
	@Override
	public @NotNull Vector3IntList clone() {
		return new Vector3IntList(this);
	}

	public interface Vector3IntConsumer {
		void accept(int x, int y, int z);
	}

	public void foreach(@NotNull Vector3IntConsumer consumer) {
		int[] buf = _buffer;
		for (int i = 0, n = _count - 2; i < n; i += 3)
			consumer.accept(buf[i], buf[i + 1], buf[i + 2]);
	}

	public interface Vector3IntPredicate {
		boolean test(int x, int y, int z);
	}

	public boolean foreachPred(@NotNull Vector3IntPredicate predicate) {
		int[] buf = _buffer;
		for (int i = 0, n = _count - 2; i < n; i += 3) {
			if (!predicate.test(buf[i], buf[i + 1], buf[i + 2]))
				return false;
		}
		return true;
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		int n = _count / 3;
		bb.WriteUInt(n);
		if (n > 0) {
			n *= 3;
			int[] buf = _buffer;
			for (int i = 0; i < n; i++)
				bb.WriteInt(buf[i]);
		}
	}

	@Override
	public void encode(@NotNull ByteBuffer bb, int n) {
		int count = _count / 3;
		if (count != n)
			throw new java.util.ConcurrentModificationException(String.valueOf(count));
		if (n > 0) {
			n *= 3;
			int[] buf = _buffer;
			for (int i = 0; i < n; i++)
				bb.WriteInt(buf[i]);
		}
	}

	@Override
	public void decode(@NotNull ByteBuffer bb, int n) {
		super.decode(bb, n * 3);
	}
}
