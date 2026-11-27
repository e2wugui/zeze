package Zeze.Transaction.GTable;

/*
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Map;
import java.util.Map.Entry;
import org.jspecify.annotations.Nullable;

/**
 * 将所有调用转发至底层 {@link Map.Entry} 委托对象的装饰器基类。
 * <p>默认直接转发至 {@link #delegate()}；若子类覆写 {@link #getKey()} 或 {@link #getValue()}，
 * 可按需覆写 {@link #equals}、{@link #hashCode}、{@link #toString} 并调用对应的 {@code standard*} 方法。
 */
public abstract class ForwardingMapEntry<K extends @Nullable Object, V extends @Nullable Object> extends ForwardingObject implements Map.Entry<K, V> {
	protected ForwardingMapEntry() {}

	@Override
	protected abstract Entry<K, V> delegate();

	@Override
	public K getKey() {
		return delegate().getKey();
	}

	@Override
	public V getValue() {
		return delegate().getValue();
	}

	@Override
	public V setValue(V value) {
		return delegate().setValue(value);
	}

	@SuppressWarnings("EqualsDoesntCheckParameterClass")
	@Override
	public boolean equals(@Nullable Object object) {
		return delegate().equals(object);
	}

	@Override
	public int hashCode() {
		return delegate().hashCode();
	}

	/** 基于 {@link #getKey()} 和 {@link #getValue()} 的标准 {@link #equals(Object)} 实现。 */
	protected boolean standardEquals(@Nullable Object object) {
		if (object instanceof Entry<?, ?> that) {
			return Utils.equal(this.getKey(), that.getKey())
					&& Utils.equal(this.getValue(), that.getValue());
		}
		return false;
	}

	/** 基于 {@link #getKey()} 和 {@link #getValue()} 的标准 {@link #hashCode()} 实现。 */
	protected int standardHashCode() {
		K k = getKey();
		V v = getValue();
		return ((k == null) ? 0 : k.hashCode()) ^ ((v == null) ? 0 : v.hashCode());
	}

	/** 基于 {@link #getKey()} 和 {@link #getValue()} 的标准 {@link #toString()} 实现。 */
	protected String standardToString() {
		return getKey() + "=" + getValue();
	}
}
