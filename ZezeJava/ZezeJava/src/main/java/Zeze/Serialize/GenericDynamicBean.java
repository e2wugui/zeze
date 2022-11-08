package Zeze.Serialize;

public class GenericDynamicBean extends GenericBean {
	public long typeId;

	@Override
	public GenericDynamicBean decode(ByteBuffer bb) {
		typeId = bb.ReadLong();
		super.decode(bb);
		return this;
	}

	@Override
	public StringBuilder buildString(StringBuilder sb, int level) {
		return super.buildString(sb.append(typeId).append(':'), level);
	}
}
