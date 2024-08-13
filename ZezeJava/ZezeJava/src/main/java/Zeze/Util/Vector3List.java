package Zeze.Util;

import java.util.Collection;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Vector3;
import org.jetbrains.annotations.NotNull;

/**
 * 用Vector3List要记得它本质就是FloatList,除了下面多出一些专用方法外,都以float数组的方式操作.
 * 如果float数组长度不被3整除,则以Vector3为单位处理时忽略结尾多余的float值.
 */
public class Vector3List extends FloatList {
	public static @NotNull Vector3List wrap(float @NotNull [] data, int count) {
		Vector3List o = new Vector3List();
		o.buffer = data;
		o.count = count > data.length ? data.length : Math.max(count, 0);
		return o;
	}

	public static @NotNull Vector3List wrap(float @NotNull [] data) {
		Vector3List o = new Vector3List();
		o.buffer = data;
		o.count = data.length;
		return o;
	}

	public static @NotNull Vector3List createSpace(int count) {
		Vector3List o = new Vector3List();
		if (count > 0) {
			o.buffer = new float[count];
			o.count = count;
		}
		return o;
	}

	public Vector3List() {
	}

	public Vector3List(int count) {
		reserveSpace(count);
	}

	public Vector3List(@NotNull FloatList fl) {
		replace(fl);
	}

	public Vector3List(float @NotNull [] data) {
		replace(data);
	}

	public Vector3List(float @NotNull [] data, int fromIdx, int count) {
		replace(data, fromIdx, count);
	}

	public int vectorSize() {
		return count / 3;
	}

	public int vectorCapacity() {
		return buffer.length / 3;
	}

	public float getX(int idx) {
		return buffer[idx * 3];
	}

	public float getY(int idx) {
		return buffer[idx * 3 + 1];
	}

	public float getZ(int idx) {
		return buffer[idx * 3 + 2];
	}

	public Vector3 getVector(int idx) {
		float[] buf = buffer;
		idx *= 3;
		return new Vector3(buf[idx], buf[idx + 1], buf[idx + 2]);
	}

	public void setX(int idx, float x) {
		buffer[idx * 3] = x;
	}

	public void setY(int idx, float y) {
		buffer[idx * 3 + 1] = y;
	}

	public void setZ(int idx, float z) {
		buffer[idx * 3 + 2] = z;
	}

	public void set(int idx, float x, float y, float z) {
		float[] buf = buffer;
		idx *= 3;
		buf[idx] = x;
		buf[idx + 1] = y;
		buf[idx + 2] = z;
	}

	public float addValueX(int idx, float x) {
		float[] buf = buffer;
		idx *= 3;
		buf[idx] = x += buf[idx];
		return x;
	}

	public float addValueY(int idx, float y) {
		float[] buf = buffer;
		idx = idx * 3 + 1;
		buf[idx] = y += buf[idx];
		return y;
	}

	public float addValueZ(int idx, float z) {
		float[] buf = buffer;
		idx = idx * 3 + 2;
		buf[idx] = z += buf[idx];
		return z;
	}

	public void addValue(int idx, float x, float y, float z) {
		float[] buf = buffer;
		idx *= 3;
		buf[idx] += x;
		buf[idx + 1] += y;
		buf[idx + 2] += z;
	}

	public float @NotNull [] toArrayVector(int fromIdx, int count) {
		return toArray(fromIdx * 3, count * 3);
	}

	public @NotNull Vector3List wrapsVector(float @NotNull [] data, int count) {
		super.wraps(data, count * 3);
		return this;
	}

	@Override
	public @NotNull Vector3List wraps(float @NotNull [] data) {
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

	public void replaceVector(float @NotNull [] data, int fromIdx, int count) {
		replace(data, fromIdx * 3, count * 3);
	}

	public @NotNull Vector3List add(float x, float y, float z) {
		int n = count;
		int nNew = n + 3;
		reserve(nNew);
		float[] buf = buffer;
		buf[n] = x;
		buf[n + 1] = y;
		buf[n + 2] = z;
		count = nNew;
		return this;
	}

	public @NotNull Vector3List add(Vector3 v) {
		return add(v.x, v.y, v.z);
	}

	@Override
	public @NotNull Vector3List addAll(float[] data, int fromIdx, int count) {
		super.addAll(data, fromIdx, count);
		return this;
	}

	@Override
	public @NotNull Vector3List addAll(float @NotNull [] data) {
		super.addAll(data);
		return this;
	}

	@Override
	public @NotNull Vector3List addAll(@NotNull FloatList fl) {
		super.addAll(fl);
		return this;
	}

	@Override
	public @NotNull Vector3List addAll(@NotNull Collection<Float> c) {
		super.addAll(c);
		return this;
	}

	public @NotNull Vector3List addAllVector(@NotNull Collection<Vector3> c) {
		int n = count;
		reserve(n + c.size() * 3);
		float[] buf = buffer;
		for (Vector3 v : c) {
			buf[n++] = v.x;
			buf[n++] = v.y;
			buf[n++] = v.z;
		}
		count = n;
		return this;
	}

	public void addAllToVector(@NotNull Collection<Vector3> c) {
		float[] buf = buffer;
		for (int i = 0, n = count - 2; i < n; i += 3)
			c.add(new Vector3(buf[i], buf[i + 1], buf[i + 2]));
	}

	public @NotNull Vector3List insertVector(int fromIdx, float x, float y, float z) {
		fromIdx *= 3;
		int n = count;
		if (fromIdx < 0)
			fromIdx = 0;
		if (fromIdx >= n)
			return add(x, y, z);
		reserve(n + 3);
		float[] buf = buffer;
		System.arraycopy(buf, fromIdx, buf, fromIdx + 3, n - fromIdx);
		buf[fromIdx] = x;
		buf[fromIdx + 1] = y;
		buf[fromIdx + 2] = z;
		count = n + 3;
		return this;
	}

	@Override
	public @NotNull Vector3List insert(int fromIdx, float @NotNull [] data, int idx, int count) {
		super.insert(fromIdx, data, idx, count);
		return this;
	}

	@Override
	public @NotNull Vector3List insert(int fromIdx, float @NotNull [] data) {
		super.insert(fromIdx, data);
		return this;
	}

	@Override
	public @NotNull Vector3List insert(int fromIdx, @NotNull FloatList fl) {
		super.insert(fromIdx, fl);
		return this;
	}

	public @NotNull Vector3List removeVector(int idx) {
		idx *= 3;
		int lastIdx = count - 3;
		if (idx < 0 || idx > lastIdx)
			return this;
		count = lastIdx;
		if (idx != lastIdx)
			System.arraycopy(buffer, idx + 3, buffer, idx, lastIdx - idx);
		return this;
	}

	public @NotNull Vector3List removeAndExchangeLastVector(int idx) {
		idx *= 3;
		int lastIdx = count - 3;
		if (idx >= 0 && idx <= lastIdx) {
			float[] buf = buffer;
			count = lastIdx;
			buf[idx] = buf[lastIdx];
			buf[idx + 1] = buf[lastIdx + 1];
			buf[idx + 2] = buf[lastIdx + 2];
		}
		return this;
	}

	public @NotNull Vector3List eraseVector(int fromIdx, int toIdx) {
		super.erase(fromIdx * 3, toIdx * 3);
		return this;
	}

	public @NotNull Vector3List eraseFrontVector(int count) {
		super.eraseFront(count * 3);
		return this;
	}

	public int indexOfVector(float x, float y, float z) {
		return indexOfVector(x, y, z, 0);
	}

	public int indexOfVector(float x, float y, float z, int fromIdx) {
		float[] buf = buffer;
		for (int i = fromIdx * 3, n = count - 2; i < n; i += 3) {
			if (buf[i] == x && buf[i + 1] == y && buf[i + 2] == z)
				return i / 3;
		}
		return -1;
	}

	@SuppressWarnings("MethodDoesntCallSuperMethod")
	@Override
	public @NotNull Vector3List clone() {
		return new Vector3List(this);
	}

	public interface Vector3Consumer {
		void accept(float x, float y, float z);
	}

	public void foreach(@NotNull Vector3Consumer consumer) {
		float[] buf = buffer;
		for (int i = 0, n = count - 2; i < n; i += 3)
			consumer.accept(buf[i], buf[i + 1], buf[i + 2]);
	}

	public interface Vector3Predicate {
		boolean test(float x, float y, float z);
	}

	public boolean foreachPred(@NotNull Vector3Predicate predicate) {
		float[] buf = buffer;
		for (int i = 0, n = count - 2; i < n; i += 3) {
			if (!predicate.test(buf[i], buf[i + 1], buf[i + 2]))
				return false;
		}
		return true;
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		int n = count / 3;
		bb.WriteUInt(n);
		if (n > 0)
			bb.WriteFloats(buffer, 0, n * 3);
	}

	@Override
	public void encode(@NotNull ByteBuffer bb, int n) {
		int count = this.count / 3;
		if (count != n)
			throw new java.util.ConcurrentModificationException(String.valueOf(count));
		if (n > 0)
			bb.WriteFloats(buffer, 0, n * 3);
	}

	@Override
	public void decode(@NotNull IByteBuffer bb, int n) {
		super.decode(bb, n * 3);
	}
}
