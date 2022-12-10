// auto-generated @formatter:off
package Zeze.Builtin.ServiceManagerWithRaft;

public interface BServiceInfosReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BServiceInfos copy();

    public String getServiceName();
    public Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.ServiceManagerWithRaft.BServiceInfo, Zeze.Builtin.ServiceManagerWithRaft.BServiceInfoReadOnly> getServiceInfoListSortedByIdentityReadOnly();
    public long getSerialId();
}
