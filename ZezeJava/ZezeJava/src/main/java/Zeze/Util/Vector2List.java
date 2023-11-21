package Zeze.Util;

import java.util.Collection;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Vector2;
import org.jetbrains.annotations.NotNull;

/**
 * 用Vector2List要记得它本质就是FloatList,除了下面多出一些专用方法外,都以float数组的方式操作.
 * 如果float数组长度不被2整除,则以Vector2为单位处理时忽略结尾多余的float值.
 */
public class Vector2List extends FloatList {
	public static @NotNull Vector2List wrap(float @NotNull [] data, int count) {
		Vector2List o = new Vector2List();
		o.buffer = data;
		o.count = count > data.length ? data.length : Math.max(count, 0);
		return o;
	}

	public static @NotNull Vector2List wrap(float @NotNull [] data) {
		Vector2List o = new Vector2List();
		o.buffer = data;
		o.count = data.length;
		return o;
	}

	public static @NotNull Vector2List createSpace(int count) {
		Vector2List o = new Vector2List();
		if (count > 0) {
			o.buffer = new float[count];
			o.count = count;
		}
		return o;
	}

	public Vector2List() {
	}

	public Vector2List(int count) {
		reserveSpace(count);
	}

	public Vector2List(@NotNull FloatList fl) {
		replace(fl);
	}

	public Vector2List(float @NotNull [] data) {
		replace(data);
	}

	public Vector2List(float @NotNull [] data, int fromIdx, int count) {
		replace(data, fromIdx, count);
	}

	public int vectorSize() {
		return count >> 1;
	}

	public int vectorCapacity() {
		return buffer.length >> 1;
	}

	public float getX(int idx) {
		return buffer[idx * 2];
	}

	public float getY(int idx) {
		return buffer[idx * 2 + 1];
	}

	public Vector2 getVector(int idx) {
		float[] buf = buffer;
		idx *= 2;
		return new Vector2(buf[idx], buf[idx + 1]);
	}

	public void setX(int idx, float x) {
		buffer[idx * 2] = x;
	}

	public void setY(int idx, float y) {
		buffer[idx * 2 + 1] = y;
	}

	public void set(int idx, float x, float y) {
		float[] buf = buffer;
		idx *= 2;
		buf[idx] = x;
		buf[idx + 1] = y;
	}

	public float addValueX(int idx, float x) {
		float[] buf = buffer;
		idx *= 2;
		buf[idx] = x += buf[idx];
		return x;
	}

	public float addValueY(int idx, float y) {
		float[] buf = buffer;
		idx = idx * 2 + 1;
		buf[idx] = y += buf[idx];
		return y;
	}

	public void addValue(int idx, float x, float y) {
		float[] buf = buffer;
		idx *= 2;
		buf[idx] += x;
		buf[idx + 1] += y;
	}

	public float @NotNull [] toArrayVector(int fromIdx, int count) {
		return toArray(fromIdx * 2, count * 2);
	}

	public @NotNull Vector2List wrapsVector(float @NotNull [] data, int count) {
		super.wraps(data, count * 2);
		return this;
	}

	@Override
	public @NotNull Vector2List wraps(float @NotNull [] data) {
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

	public void replaceVector(float @NotNull [] data, int fromIdx, int count) {
		replace(data, fromIdx * 2, count * 2);
	}

	public @NotNull Vector2List add(float x, float y) {
		int n = count;
		int nNew = n + 2;
		reserve(nNew);
		float[] buf = buffer;
		buf[n] = x;
		buf[n + 1] = y;
		count = nNew;
		return this;
	}

	public @NotNull Vector2List add(Vector2 v) {
		return add(v.x, v.y);
	}

	@Override
	public @NotNull Vector2List addAll(float[] data, int fromIdx, int count) {
		super.addAll(data, fromIdx, count);
		return this;
	}

	@Override
	public @NotNull Vector2List addAll(float @NotNull [] data) {
		super.addAll(data);
		return this;
	}

	@Override
	public @NotNull Vector2List addAll(@NotNull FloatList fl) {
		super.addAll(fl);
		return this;
	}

	@Override
	public @NotNull Vector2List addAll(@NotNull Collection<Float> c) {
		super.addAll(c);
		return this;
	}

	public @NotNull Vector2List addAllVector(@NotNull Collection<Vector2> c) {
		int n = count;
		reserve(n + c.size() * 2);
		float[] buf = buffer;
		for (Vector2 v : c) {
			buf[n++] = v.x;
			buf[n++] = v.y;
		}
		count = n;
		return this;
	}

	public @NotNull void addAllToVector(@NotNull Collection<Vector2> c) {
		float[] buf = buffer;
		for (int i = 0, n = count - 1; i < n; i += 2)
			c.add(new Vector2(buf[i], buf[i + 1]));
	}

	public @NotNull Vector2List insertVector(int fromIdx, float x, float y) {
		fromIdx *= 2;
		int n = count;
		if (fromIdx < 0)
			fromIdx = 0;
		if (fromIdx >= n)
			return add(x, y);
		reserve(n + 2);
		float[] buf = buffer;
		System.arraycopy(buf, fromIdx, buf, fromIdx + 2, n - fromIdx);
		buf[fromIdx] = x;
		buf[fromIdx + 1] = y;
		count = n + 2;
		return this;
	}

	@Override
	public @NotNull Vector2List insert(int fromIdx, float @NotNull [] data, int idx, int count) {
		super.insert(fromIdx, data, idx, count);
		return this;
	}

	@Override
	public @NotNull Vector2List insert(int fromIdx, float @NotNull [] data) {
		super.insert(fromIdx, data);
		return this;
	}

	@Override
	public @NotNull Vector2List insert(int fromIdx, @NotNull FloatList fl) {
		super.insert(fromIdx, fl);
		return this;
	}

	public @NotNull Vector2List removeVector(int idx) {
		idx *= 2;
		int lastIdx = count - 2;
		if (idx < 0 || idx > lastIdx)
			return this;
		count = lastIdx;
		if (idx != lastIdx)
			System.arraycopy(buffer, idx + 2, buffer, idx, lastIdx - idx);
		return this;
	}

	public @NotNull Vector2List removeAndExchangeLastVector(int idx) {
		idx *= 2;
		int lastIdx = count - 2;
		if (idx >= 0 && idx <= lastIdx) {
			float[] buf = buffer;
			count = lastIdx;
			buf[idx] = buf[lastIdx];
			buf[idx + 1] = buf[lastIdx + 1];
		}
		return this;
	}

	public @NotNull Vector2List eraseVector(int fromIdx, int toIdx) {
		super.erase(fromIdx * 2, toIdx * 2);
		return this;
	}

	public @NotNull Vector2List eraseFrontVector(int count) {
		super.eraseFront(count * 2);
		return this;
	}

	public int indexOfVector(float x, float y) {
		return indexOfVector(x, y, 0);
	}

	public int indexOfVector(float x, float y, int fromIdx) {
		float[] buf = buffer;
		for (int i = fromIdx * 2, n = count - 1; i < n; i += 2) {
			if (buf[i] == x && buf[i + 1] == y)
				return i >> 1;
		}
		return -1;
	}

	@SuppressWarnings("MethodDoesntCallSuperMethod")
	@Override
	public @NotNull Vector2List clone() {
		return new Vector2List(this);
	}

	public interface Vector2Consumer {
		void accept(float x, float y);
	}

	public void foreach(@NotNull Vector2Consumer consumer) {
		float[] buf = buffer;
		for (int i = 0, n = count - 1; i < n; i += 2)
			consumer.accept(buf[i], buf[i + 1]);
	}

	public interface Vector2Predicate {
		boolean test(float x, float y);
	}

	public boolean foreachPred(@NotNull Vector2Predicate predicate) {
		float[] buf = buffer;
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
		if (n > 0)
			bb.WriteFloats(buffer, 0, n * 2);
	}

	@Override
	public void encode(@NotNull ByteBuffer bb, int n) {
		int count = this.count >> 1;
		if (count != n)
			throw new java.util.ConcurrentModificationException(String.valueOf(count));
		if (n > 0)
			bb.WriteFloats(buffer, 0, n * 2);
	}

	@Override
	public void decode(@NotNull IByteBuffer bb, int n) {
		super.decode(bb, n * 2);
	}
}
