// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

public interface BWalkReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BWalk copy();

    Zeze.Net.Binary getExclusiveStartKey();
    int getProposeLimit();
    boolean isDesc();
    Zeze.Net.Binary getPrefix();
}
