// auto-generated @formatter:off
package Zeze.Builtin.Onz;

public interface BSavedCommitsReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BSavedCommits copy();

    int getState();
    Zeze.Transaction.Collections.PSet1ReadOnly<String> getOnzsReadOnly();
}
