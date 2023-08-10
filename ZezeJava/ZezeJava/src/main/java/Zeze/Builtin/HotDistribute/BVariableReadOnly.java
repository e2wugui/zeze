// auto-generated @formatter:off
package Zeze.Builtin.HotDistribute;

public interface BVariableReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BVariable copy();

    int getId();
    String getName();
    String getType();
    String getKey();
    String getValue();
}
