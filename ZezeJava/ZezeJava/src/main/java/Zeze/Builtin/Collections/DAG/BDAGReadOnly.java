// auto-generated @formatter:off
package Zeze.Builtin.Collections.DAG;

public interface BDAGReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BDAG copy();

    long getNodeSum();
    long getEdgeSum();
    String getStartNode();
    String getEndNode();
}
