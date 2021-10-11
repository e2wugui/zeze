package Zeze.Transaction.Collections;

import Zeze.Transaction.*;

public abstract class PCollection extends Bean { // 简单起见就继承了，实际上容器可以不是Bean，只不过用到了一些Bean的属性。
	protected long LogKey;

	protected PCollection(long logKey) {
		super((int)(logKey & Bean.MaxVariableId));
		LogKey = logKey;
	}

	@Override
	public void Decode(Zeze.Serialize.ByteBuffer bb) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void Encode(Zeze.Serialize.ByteBuffer bb) {
		throw new UnsupportedOperationException();
	}
}