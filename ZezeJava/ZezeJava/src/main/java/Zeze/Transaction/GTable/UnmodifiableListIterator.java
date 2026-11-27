package Zeze.Transaction.GTable;

/*
 * Copyright (C) 2010 The Guava Authors
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

import java.util.ListIterator;

/** 不支持 {@link #remove}、{@link #add} 和 {@link #set} 操作的列表迭代器基类。 */
public abstract class UnmodifiableListIterator<E> extends UnmodifiableIterator<E> implements ListIterator<E> {
	protected UnmodifiableListIterator() {}

	/**
	 * 始终抛出 {@link UnsupportedOperationException}，不修改底层数据。
	 *
	 * @throws UnsupportedOperationException 始终抛出
	 * @deprecated 不支持此操作。
	 */
	@Deprecated
	@Override
	public final void add(E e) {
		throw new UnsupportedOperationException();
	}

	/**
	 * 始终抛出 {@link UnsupportedOperationException}，不修改底层数据。
	 *
	 * @throws UnsupportedOperationException 始终抛出
	 * @deprecated 不支持此操作。
	 */
	@Deprecated
	@Override
	public final void set(E e) {
		throw new UnsupportedOperationException();
	}
}
