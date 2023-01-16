// auto-generated @formatter:off
package Zeze.Builtin.Collections.LinkedMap;

public interface BClearJobStateReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BClearJobState copy();

    public long getHeadNodeId();
    public long getTailNodeId();
    public String getLinkedMapName();
}
