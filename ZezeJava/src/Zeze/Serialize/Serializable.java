package Zeze.Serialize;

public interface Serializable {
	public void Decode(ByteBuffer bb);
	public void Encode(ByteBuffer bb);
}