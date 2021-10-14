package Zeze.Services;

import Zeze.Util.BitConverter;

public class GlobalCacheManager {
    public static final int StateInvalid = 0;
    public static final int StateShare = 1;
    public static final int StateModify = 2;
    public static final int StateRemoved = -1; // 从容器(Cache或Global)中删除后设置的状态，最后一个状态。
    public static final int StateReduceRpcTimeout = -2; // 用来表示 reduce 超时失败。不是状态。
    public static final int StateReduceException = -3; // 用来表示 reduce 异常失败。不是状态。
    public static final int StateReduceNetError = -4;  // 用来表示 reduce 网络失败。不是状态。

    public static final int AcquireShareDeadLockFound = 1;
    public static final int AcquireShareAlreadyIsModify = 2;
    public static final int AcquireModifyDeadLockFound = 3;
    public static final int AcquireErrorState = 4;
    public static final int AcquireModifyAlreadyIsModify = 5;
    public static final int AcquireShareFaild = 6;
    public static final int AcquireModifyFaild = 7;

    public static final int ReduceErrorState = 11;
    public static final int ReduceShareAlreadyIsInvalid = 12;
    public static final int ReduceShareAlreadyIsShare = 13;
    public static final int ReduceInvalidAlreadyIsInvalid = 14;

    public static final int AcquireNotLogin = 20;

    public static final int CleanupErrorSecureKey = 30;
    public static final int CleanupErrorGlobalCacheManagerHashIndex = 31;
    public static final int CleanupErrorHasConnection = 32;

    public static final int ReLoginBindSocketFail = 40;

    public static final int NormalCloseUnbindFail = 50;

    public static final int LoginBindSocketFail = 60;

    public static class Param extends Zeze.Transaction.Bean {
        public GlobalTableKey GlobalTableKey; // 没有初始化，使用时注意
        public int State;

        @Override
        public void Decode(Zeze.Serialize.ByteBuffer bb) {
            if (null == GlobalTableKey)
                GlobalTableKey = new GlobalTableKey();
            GlobalTableKey.Decode(bb);
            State = bb.ReadInt();
        }

        @Override
        public void Encode(Zeze.Serialize.ByteBuffer bb) {
            GlobalTableKey.Encode(bb);
            bb.WriteInt(State);
        }

        @Override
        protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return GlobalTableKey.toString() + ":" + State;
        }
    }

    public static class Acquire extends Zeze.Net.Rpc<Param, Param>
    {
        public final static int ProtocolId_ = Zeze.Transaction.Bean.Hash16(Acquire.class.getName());

        @Override
        public int getModuleId() {
        	return 0;
        }

        @Override
        public int getProtocolId() {
        	return ProtocolId_;
        }

        public Acquire() {
        	Argument = new Param();
        	Result = new Param();
        }

        public Acquire(GlobalTableKey gkey, int state) {
        	Argument = new Param();
        	Result = new Param();
            Argument.GlobalTableKey = gkey;
            Argument.State = state;
        }
    }

    public static class Reduce extends Zeze.Net.Rpc<Param, Param> {
        public final static int ProtocolId_ = Zeze.Transaction.Bean.Hash16(Reduce.class.getName());

        @Override
        public int getModuleId() {
        	return 0;
        }
        
        @Override
        public int getProtocolId() {
        	return ProtocolId_;
        }

        public Reduce() {
        	Argument = new Param();
        	Result = new Param();
        }

        public Reduce(GlobalTableKey gkey, int state) {
        	Argument = new Param();
        	Result = new Param();
            Argument.GlobalTableKey = gkey;
            Argument.State = state;
        }
    }

    public static class LoginParam extends Zeze.Transaction.Bean {
        public int ServerId;

        // GlobalCacheManager 本身没有编号。
        // 启用多个进程，使用 GlobalTableKey.GetHashCode() 分配负载后，报告错误需要这个来识别哪个进程。
        // 当然识别还可以根据 ServerService 绑定的ip和port。
        // 给每个实例加配置不容易维护。
        public int GlobalCacheManagerHashIndex;

        @Override
        public void Decode(Zeze.Serialize.ByteBuffer bb) {
            ServerId = bb.ReadInt();
            GlobalCacheManagerHashIndex = bb.ReadInt();
        }

        @Override
        public void Encode(Zeze.Serialize.ByteBuffer bb) {
            bb.WriteInt(ServerId);
            bb.WriteInt(GlobalCacheManagerHashIndex);
        }

        @Override
        protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
            throw new UnsupportedOperationException();
        }
    }

    public static class Login extends Zeze.Net.Rpc<LoginParam, Zeze.Transaction.EmptyBean> {
        public final static int ProtocolId_ = Zeze.Transaction.Bean.Hash16(Login.class.getName());

        @Override
        public int getModuleId() {
        	return 0;
        }

        @Override
        public int getProtocolId() {
        	return ProtocolId_;
        }

        public Login() {
        	Argument = new LoginParam();
        	Result = new Zeze.Transaction.EmptyBean();
        }

        public Login(int id) {
        	Argument = new LoginParam();
        	Result = new Zeze.Transaction.EmptyBean();

        	Argument.ServerId = id;
        }
    }

    public static class ReLogin extends Zeze.Net.Rpc<LoginParam, Zeze.Transaction.EmptyBean> {
        public final static int ProtocolId_ = Zeze.Transaction.Bean.Hash16(ReLogin.class.getName());

        @Override
        public int getModuleId() {
        	return 0;
        }

        @Override
        public int getProtocolId() {
        	return ProtocolId_;
        }

        public ReLogin() {
        	Argument = new LoginParam();
        	Result = new Zeze.Transaction.EmptyBean();
        }

        public ReLogin(int id) {
        	Argument = new LoginParam();
        	Result = new Zeze.Transaction.EmptyBean();
            Argument.ServerId = id;
        }
    }

    public static class NormalClose extends Zeze.Net.Rpc<Zeze.Transaction.EmptyBean, Zeze.Transaction.EmptyBean> {
        public final static int ProtocolId_ = Zeze.Transaction.Bean.Hash16(NormalClose.class.getName());

        @Override
        public int getModuleId() {
        	return 0;
        }
        
        @Override
        public int getProtocolId() {
        	return ProtocolId_;
        }
        
        public NormalClose() {
        	Argument = new Zeze.Transaction.EmptyBean();
        	Result = new Zeze.Transaction.EmptyBean();
        }
    }

    public static class AchillesHeel extends Zeze.Transaction.Bean {
        public int AutoKeyLocalId; // 必须的。

        public String SecureKey; // 安全验证
        public int GlobalCacheManagerHashIndex; // 安全验证

        @Override
        public void Decode(Zeze.Serialize.ByteBuffer bb) {
            AutoKeyLocalId = bb.ReadInt();
            SecureKey = bb.ReadString();
            GlobalCacheManagerHashIndex = bb.ReadInt();
        }

        @Override
        public void Encode(Zeze.Serialize.ByteBuffer bb) {
            bb.WriteInt(AutoKeyLocalId);
            bb.WriteString(SecureKey);
            bb.WriteInt(GlobalCacheManagerHashIndex);
        }

        @Override
        protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
            throw new UnsupportedOperationException();
        }
    }

    public static class Cleanup extends Zeze.Net.Rpc<AchillesHeel, Zeze.Transaction.EmptyBean> {
        public final static int ProtocolId_ = Zeze.Transaction.Bean.Hash16(Cleanup.class.getName());

        @Override
        public int getModuleId() {
        	return 0;
        }
        
        @Override
        public int getProtocolId() {
        	return ProtocolId_;
        }
        
        public Cleanup() {
        	Argument = new AchillesHeel();
        	Result = new Zeze.Transaction.EmptyBean();
        }
    }

    public static class GlobalTableKey implements Comparable<GlobalTableKey>, Zeze.Serialize.Serializable {
        public String TableName;
        public byte[] Key;

        public GlobalTableKey() {
        }

        public GlobalTableKey(String tableName, Zeze.Serialize.ByteBuffer key) {
        	this(tableName, key.Copy());
        }

        public GlobalTableKey(String tableName, byte[] key) {
            TableName = tableName;
            Key = key;
        }

        @Override
        public int compareTo(GlobalTableKey other) {
            int c = this.TableName.compareTo(other.TableName);
            if (c != 0)
                return c;

            return Zeze.Serialize.ByteBuffer.Compare(Key, other.Key);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;

            if (obj instanceof GlobalTableKey) {
            	var another = (GlobalTableKey)obj;
                return TableName.equals(another.TableName) && Zeze.Serialize.ByteBuffer.Equals(Key, another.Key);
            }
            return false;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 17;
            result = prime * result + Zeze.Serialize.ByteBuffer.calc_hashnr(TableName);
            result = prime * result + Zeze.Serialize.ByteBuffer.calc_hashnr(Key, 0, Key.length);
            return result;
        }

        @Override
        public String toString() {
            return TableName + ":" + BitConverter.toString(Key, 0, Key.length);
        }

        public void Decode(Zeze.Serialize.ByteBuffer bb) {
            TableName = bb.ReadString();
            Key = bb.ReadBytes();
        }

        public void Encode(Zeze.Serialize.ByteBuffer bb) {
            bb.WriteString(TableName);
            bb.WriteBytes(Key);
        }
    }
}
