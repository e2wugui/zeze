// auto-generated @formatter:off
package Zeze.Builtin.Token;

public interface BGetTokenResReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BGetTokenRes copy();

    Zeze.Net.Binary getContext();
    long getCount();
    long getTime();
}
