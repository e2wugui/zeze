// auto-generated @formatter:off
package Zeze.Builtin.Provider;

// gs to link
public interface BModuleReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BModule copy();

    int getChoiceType();
    int getConfigType();
}
