// auto-generated @formatter:off
package Zeze.Builtin.Collections.LinkedMap;

public interface BLinkedMapReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BLinkedMap copy();

    public long getHeadNodeId();
    public long getTailNodeId();
    public long getCount();
    public long getLastNodeId();
}
