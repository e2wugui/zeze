// auto-generated @formatter:off
package Zeze.Builtin.Collections.LinkedMap;

public interface BLinkedMapReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BLinkedMap copy();

    long getHeadNodeId();
    long getTailNodeId();
    long getCount();
    long getLastNodeId();
}
