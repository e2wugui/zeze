// auto-generated @formatter:off
package Zeze.Builtin.Provider;

public interface BLoadReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BLoad copy();

    int getOnline();
    int getProposeMaxOnline();
    int getOnlineNew();
    int getOverload();
    int getMaxOnlineNew();
}
