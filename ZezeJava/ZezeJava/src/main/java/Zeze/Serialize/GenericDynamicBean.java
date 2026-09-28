package Zeze.Serialize;

import org.jetbrains.annotations.NotNull;

/** 带 typeId 前缀的动态 Bean 编码载体：解码时先读 typeId 再交由 GenericBean 解码各变量。 */
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
