package Zeze.Raft;

import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import org.jetbrains.annotations.NotNull;

/**
 * 代理协议的结果参数。
 */
public class ProxyResult implements Serializable {
	private Binary data = Binary.Empty;

	public void setData(Binary data) {
		this.data = data;
	}

	public Binary getData() {
		return data;
	}

	public <T extends Serializable> void decode(T result) {
		result.decode(ByteBuffer.Wrap(data));
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		bb.WriteBinary(data);
	}

	@Override
	public void decode(@NotNull ByteBuffer bb) {
		data = bb.ReadBinary();
	}
}
