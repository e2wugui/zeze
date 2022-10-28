// auto-generated @formatter:off
package Zeze.Builtin.Collections.DAG;

public interface BDAGReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BDAG copy();

    public long getNodeSum();
    public long getEdgeSum();
    public String getStartNode();
    public String getEndNode();
}
