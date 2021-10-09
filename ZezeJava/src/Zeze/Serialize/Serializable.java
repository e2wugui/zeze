package Zeze.Serialize;

import Zeze.*;

public interface Serializable {
	public void Decode(ByteBuffer bb);
	public void Encode(ByteBuffer bb);
}