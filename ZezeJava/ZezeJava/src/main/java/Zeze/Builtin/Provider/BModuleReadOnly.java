// auto-generated @formatter:off
package Zeze.Builtin.Provider;

// gs to link
public interface BModuleReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BModule copy();

    public int getChoiceType();
    public int getConfigType();
    public int getSubscribeType();
}
