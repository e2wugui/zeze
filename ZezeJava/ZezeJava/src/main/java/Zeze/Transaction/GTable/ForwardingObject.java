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

import java.io.Serializable;

/**
 * 装饰器模式抽象基类，子类覆写 {@link #delegate()} 返回被装饰对象。
 * <p>为保持 {@code equals} 对称性，本类不转发 {@code equals} 与 {@code hashCode}，仅转发 {@code toString}。
 */
public abstract class ForwardingObject {

	protected ForwardingObject() {}

	/** 返回被转发调用的底层委托对象。 */
	protected abstract Object delegate();

	/** 转发至委托对象的 {@code toString} 方法。 */
	@Override
	public String toString() {
		return delegate().toString();
	}
}
