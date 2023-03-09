// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

// 下面是Master跟Dbh2Manager通讯的协议，也包装在MasterAgent中
public class CreateBucket extends Zeze.Net.Rpc<Zeze.Builtin.Dbh2.BBucketMetaDaTa, Zeze.Transaction.EmptyBeanDaTa> {
    public static final int ModuleId_ = 11027;
    public static final int ProtocolId_ = -572178079; // 3722789217
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47364327162209

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    @Override
    public long getTypeId() {
        return TypeId_;
    }

    public CreateBucket() {
        Argument = new Zeze.Builtin.Dbh2.BBucketMetaDaTa();
        Result = Zeze.Transaction.EmptyBeanDaTa.instance;
    }

    public CreateBucket(Zeze.Builtin.Dbh2.BBucketMetaDaTa arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBeanDaTa.instance;
    }
}
