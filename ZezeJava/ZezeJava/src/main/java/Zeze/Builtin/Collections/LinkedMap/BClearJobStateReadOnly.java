// auto-generated @formatter:off
package Zeze.Builtin.Collections.LinkedMap;

public interface BClearJobStateReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BClearJobState copy();

    long getHeadNodeId();
    long getTailNodeId();
    String getLinkedMapName();
}
