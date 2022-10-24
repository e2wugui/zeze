// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

public interface BAccountReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BAccount copy();

    public String getName();
    public Zeze.Transaction.Collections.PList1ReadOnly<Long> getRolesReadOnly();
    public long getLastLoginRoleId();
    public long getLastLoginVersion();
}
