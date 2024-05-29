package Zeze.Serialize;

import org.jetbrains.annotations.NotNull;

public class GenericDynamicBean extends GenericBean {
	public long typeId;

	@Override
	public @NotNull GenericDynamicBean decode(@NotNull IByteBuffer bb) {
		typeId = bb.ReadLong();
		super.decode(bb);
		return this;
	}

	@Override
	public @NotNull StringBuilder buildString(@NotNull StringBuilder sb, int level) {
		return super.buildString(sb.append(typeId).append(':'), level);
	}
}
