package Zeze.Util;

import java.util.Collection;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Vector4;
import org.jetbrains.annotations.NotNull;

/**
 * 用Vector4List要记得它本质就是FloatList,除了下面多出一些专用方法外,都以float数组的方式操作.
 * 如果float数组长度不被4整除,则以Vector4为单位处理时忽略结尾多余的float值.
 */
public class Vector4List extends FloatList {
	public static @NotNull Vector4List wrap(float @NotNull [] data, int count) {
		Vector4List o = new Vector4List();
		o.buffer = data;
		o.count = count > data.length ? data.length : Math.max(count, 0);
		return o;
	}

	public static @NotNull Vector4List wrap(float @NotNull [] data) {
		Vector4List o = new Vector4List();
		o.buffer = data;
		o.count = data.length;
		return o;
	}

	public static @NotNull Vector4List createSpace(int count) {
		Vector4List o = new Vector4List();
		if (count > 0) {
			o.buffer = new float[count];
			o.count = count;
		}
		return o;
	}

	public Vector4List() {
	}

	public Vector4List(int count) {
		reserveSpace(count);
	}

	public Vector4List(@NotNull FloatList fl) {
		replace(fl);
	}

	public Vector4List(float @NotNull [] data) {
		replace(data);
	}

	public Vector4List(float @NotNull [] data, int fromIdx, int count) {
		replace(data, fromIdx, count);
	}

	public int vectorSize() {
		return count >> 2;
	}

	public int vectorCapacity() {
		return buffer.length >> 2;
	}

	public float getX(int idx) {
		return buffer[idx * 4];
	}

	public float getY(int idx) {
		return buffer[idx * 4 + 1];
	}

	public float getZ(int idx) {
		return buffer[idx * 4 + 2];
	}

	public float getW(int idx) {
		return buffer[idx * 4 + 3];
	}

	public Vector4 getVector(int idx) {
		float[] buf = buffer;
		idx *= 4;
		return new Vector4(buf[idx], buf[idx + 1], buf[idx + 2], buf[idx + 3]);
	}

	public void setX(int idx, float x) {
		buffer[idx * 4] = x;
	}

	public void setY(int idx, float y) {
		buffer[idx * 4 + 1] = y;
	}

	public void setZ(int idx, float z) {
		buffer[idx * 4 + 2] = z;
	}

	public void setW(int idx, float w) {
		buffer[idx * 4 + 3] = w;
	}

	public void set(int idx, float x, float y, float z, float w) {
		float[] buf = buffer;
		idx *= 4;
		buf[idx] = x;
		buf[idx + 1] = y;
		buf[idx + 2] = z;
		buf[idx + 3] = w;
	}

	public float addValueX(int idx, float x) {
		float[] buf = buffer;
		idx *= 4;
		buf[idx] = x += buf[idx];
		return x;
	}

	public float addValueY(int idx, float y) {
		float[] buf = buffer;
		idx = idx * 4 + 1;
		buf[idx] = y += buf[idx];
		return y;
	}

	public float addValueZ(int idx, float z) {
		float[] buf = buffer;
		idx = idx * 4 + 2;
		buf[idx] = z += buf[idx];
		return z;
	}

	public float addValueW(int idx, float w) {
		float[] buf = buffer;
		idx = idx * 4 + 3;
		buf[idx] = w += buf[idx];
		return w;
	}

	public void addValue(int idx, float x, float y, float z, float w) {
		float[] buf = buffer;
		idx *= 4;
		buf[idx] += x;
		buf[idx + 1] += y;
		buf[idx + 2] += z;
		buf[idx + 3] += w;
	}

	public float @NotNull [] toArrayVector(int fromIdx, int count) {
		return toArray(fromIdx * 4, count * 4);
	}

	public @NotNull Vector4List wrapsVector(float @NotNull [] data, int count) {
		super.wraps(data, count * 4);
		return this;
	}

	@Override
	public @NotNull Vector4List wraps(float @NotNull [] data) {
		super.wraps(data);
		return this;
	}

	public void shrinkVector(int count) {
		shrink(count * 4);
	}

	public void reserveVector(int count) {
		reserve(count * 4);
	}

	public void reserveSpaceVector(int count) {
		reserveSpace(count * 4);
	}

	public void resizeVector(int count) {
		resize(count * 4);
	}

	public void replaceVector(float @NotNull [] data, int fromIdx, int count) {
		replace(data, fromIdx * 4, count * 4);
	}

	public @NotNull Vector4List add(float x, float y, float z, float w) {
		int n = count;
		int nNew = n + 4;
		reserve(nNew);
		float[] buf = buffer;
		buf[n] = x;
		buf[n + 1] = y;
		buf[n + 2] = z;
		buf[n + 3] = w;
		count = nNew;
		return this;
	}

	public @NotNull Vector4List add(Vector4 v) {
		return add(v.x, v.y, v.z, v.w);
	}

	@Override
	public @NotNull Vector4List addAll(float[] data, int fromIdx, int count) {
		super.addAll(data, fromIdx, count);
		return this;
	}

	@Override
	public @NotNull Vector4List addAll(float @NotNull [] data) {
		super.addAll(data);
		return this;
	}

	@Override
	public @NotNull Vector4List addAll(@NotNull FloatList fl) {
		super.addAll(fl);
		return this;
	}

	@Override
	public @NotNull Vector4List addAll(@NotNull Collection<Float> c) {
		super.addAll(c);
		return this;
	}

	public @NotNull Vector4List addAllVector(@NotNull Collection<Vector4> c) {
		int n = count;
		reserve(n + c.size() * 4);
		float[] buf = buffer;
		for (Vector4 v : c) {
			buf[n++] = v.x;
			buf[n++] = v.y;
			buf[n++] = v.z;
			buf[n++] = v.w;
		}
		count = n;
		return this;
	}

	public void addAllToVector(@NotNull Collection<Vector4> c) {
		float[] buf = buffer;
		for (int i = 0, n = count - 3; i < n; i += 4)
			c.add(new Vector4(buf[i], buf[i + 1], buf[i + 2], buf[i + 3]));
	}

	public @NotNull Vector4List insertVector(int fromIdx, float x, float y, float z, float w) {
		fromIdx *= 4;
		int n = count;
		if (fromIdx < 0)
			fromIdx = 0;
		if (fromIdx >= n)
			return add(x, y, z, w);
		reserve(n + 4);
		float[] buf = buffer;
		System.arraycopy(buf, fromIdx, buf, fromIdx + 4, n - fromIdx);
		buf[fromIdx] = x;
		buf[fromIdx + 1] = y;
		buf[fromIdx + 2] = z;
		buf[fromIdx + 3] = w;
		count = n + 4;
		return this;
	}

	@Override
	public @NotNull Vector4List insert(int fromIdx, float @NotNull [] data, int idx, int count) {
		super.insert(fromIdx, data, idx, count);
		return this;
	}

	@Override
	public @NotNull Vector4List insert(int fromIdx, float @NotNull [] data) {
		super.insert(fromIdx, data);
		return this;
	}

	@Override
	public @NotNull Vector4List insert(int fromIdx, @NotNull FloatList fl) {
		super.insert(fromIdx, fl);
		return this;
	}

	public @NotNull Vector4List removeVector(int idx) {
		idx *= 4;
		int lastIdx = count - 4;
		if (idx < 0 || idx > lastIdx)
			return this;
		count = lastIdx;
		if (idx != lastIdx)
			System.arraycopy(buffer, idx + 4, buffer, idx, lastIdx - idx);
		return this;
	}

	public @NotNull Vector4List removeAndExchangeLastVector(int idx) {
		idx *= 4;
		int lastIdx = count - 4;
		if (idx >= 0 && idx <= lastIdx) {
			float[] buf = buffer;
			count = lastIdx;
			buf[idx] = buf[lastIdx];
			buf[idx + 1] = buf[lastIdx + 1];
			buf[idx + 2] = buf[lastIdx + 2];
			buf[idx + 3] = buf[lastIdx + 3];
		}
		return this;
	}

	public @NotNull Vector4List eraseVector(int fromIdx, int toIdx) {
		super.erase(fromIdx * 4, toIdx * 4);
		return this;
	}

	public @NotNull Vector4List eraseFrontVector(int count) {
		super.eraseFront(count * 4);
		return this;
	}

	public int indexOfVector(float x, float y, float z, float w) {
		return indexOfVector(x, y, z, w, 0);
	}

	public int indexOfVector(float x, float y, float z, float w, int fromIdx) {
		float[] buf = buffer;
		for (int i = fromIdx * 4, n = count - 3; i < n; i += 4) {
			if (buf[i] == x && buf[i + 1] == y && buf[i + 2] == z && buf[i + 3] == w)
				return i >> 2;
		}
		return -1;
	}

	@SuppressWarnings("MethodDoesntCallSuperMethod")
	@Override
	public @NotNull Vector4List clone() {
		return new Vector4List(this);
	}

	public interface Vector4Consumer {
		void accept(float x, float y, float z, float w);
	}

	public void foreach(@NotNull Vector4Consumer consumer) {
		float[] buf = buffer;
		for (int i = 0, n = count - 3; i < n; i += 4)
			consumer.accept(buf[i], buf[i + 1], buf[i + 2], buf[i + 3]);
	}

	public interface Vector4Predicate {
		boolean test(float x, float y, float z, float w);
	}

	public boolean foreachPred(@NotNull Vector4Predicate predicate) {
		float[] buf = buffer;
		for (int i = 0, n = count - 3; i < n; i += 4) {
			if (!predicate.test(buf[i], buf[i + 1], buf[i + 2], buf[i + 3]))
				return false;
		}
		return true;
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		int n = count >> 2;
		bb.WriteUInt(n);
		if (n > 0)
			bb.WriteFloats(buffer, 0, n * 4);
	}

	@Override
	public void encode(@NotNull ByteBuffer bb, int n) {
		int count = this.count >> 2;
		if (count != n)
			throw new java.util.ConcurrentModificationException(String.valueOf(count));
		if (n > 0)
			bb.WriteFloats(buffer, 0, n * 4);
	}

	@Override
	public void decode(@NotNull IByteBuffer bb, int n) {
		super.decode(bb, n * 4);
	}
}
