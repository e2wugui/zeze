using DotNext.Threading;
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Net;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Xml;
using Zeze.Net;
using Zeze.Serialize;
using Zeze.Services.GlobalCacheManager;
using Zeze.Transaction;
using Zeze.Util;

namespace Zeze.Services
{
    public sealed class GlobalCacheManagerServer
    {
        public const int StateInvalid = 0;
        public const int StateShare = 1;
        public const int StateModify = 2;
        public const int StateRemoving = 3;

        public const int StateRemoved = 10; // 从容器(Cache或Global)中删除后设置的状态，最后一个状态。
        public const int StateReduceRpcTimeout = 11; // 用来表示 reduce 超时失败。不是状态。
        public const int StateReduceException = 12; // 用来表示 reduce 异常失败。不是状态。
        public const int StateReduceNetError = 13;  // 用来表示 reduce 网络失败。不是状态。
        public const int StateReduceDuplicate = 14; // 用来表示重复的 reduce。错误报告，不是状态。
        public const int StateReduceSessionNotFound = 15;
        public const int StateReduceErrorFreshAcquire = 16; // 错误码，too many try 处理机制

        public const int AcquireShareDeadLockFound = 21;
        public const int AcquireShareAlreadyIsModify = 22;
        public const int AcquireModifyDeadLockFound = 23;
        public const int AcquireErrorState = 24;
        public const int AcquireModifyAlreadyIsModify = 25;
        public const int AcquireShareFailed = 26;
        public const int AcquireModifyFailed = 27;
        public const int AcquireException = 28;
        public const int AcquireInvalidFailed = 29;
        public const int AcquireNotLogin = 30;
        public const int AcquireFreshSource = 31;

        public const int ReduceErrorState = 41;
        public const int ReduceShareAlreadyIsInvalid = 42;
        public const int ReduceShareAlreadyIsShare = 43;
        public const int ReduceInvalidAlreadyIsInvalid = 44;

        public const int CleanupErrorSecureKey = 60;
        public const int CleanupErrorGlobalCacheManagerHashIndex = 61;
        public const int CleanupErrorHasConnection = 62;

        public const int ReLoginBindSocketFail = 80;

        public const int NormalCloseUnbindFail = 100;

        public const int LoginBindSocketFail = 120;
    }
}

namespace Zeze.Services.GlobalCacheManager
{
    public sealed class GlobalKeyState : Bean
    {
        public Binary GlobalKey { get; set; }
        public int State { get; set; }
        public long ReduceTid { get; set; }

        public override void Decode(ByteBuffer bb)
        {
            GlobalKey = bb.ReadBinary();
            State = bb.ReadInt();
            ReduceTid = bb.ReadLong();
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteBinary(GlobalKey);
            bb.WriteInt(State);
            bb.WriteLong(ReduceTid);
        }

        public override string ToString()
        {
            return GlobalKey + ":" + State;
        }

        public override void ClearParameters()
        {
            GlobalKey = null;
            State = 0;
        }
    }

    public sealed class Acquire : Rpc<GlobalKeyState, GlobalKeyState>
    {
        public readonly static int ProtocolId_ = Util.FixedHash.Hash32(typeof(Acquire).FullName);

        public override int ModuleId => 0;
        public override int ProtocolId => ProtocolId_;

        public Acquire()
        {
        }

        public Acquire(Binary gkey, int state)
        {
            Argument.GlobalKey = gkey;
            Argument.State = state;
        }
    }

    public class Reduce : Rpc<GlobalKeyState, GlobalKeyState>
    {
        public readonly static int ProtocolId_ = Util.FixedHash.Hash32(typeof(Reduce).FullName);

        public override int ModuleId => 0;
        public override int ProtocolId => ProtocolId_;

        public Reduce()
        {
        }

        public Reduce(Binary gkey, int state)
        {
            Argument.GlobalKey = gkey;
            Argument.State = state;
        }
    }

    public sealed class LoginParam : Bean
    {
        public int ServerId { get; set; }

        // GlobalCacheManager 本身没有编号。
        // 启用多个进程，使用 GlobalTableKey.GetHashCode() 分配负载后，报告错误需要这个来识别哪个进程。
        // 当然识别还可以根据 ServerService 绑定的ip和port。
        // 给每个实例加配置不容易维护。
        public int GlobalCacheManagerHashIndex { get; set; }
        public bool DebugMode; // 调试模式下不检查Release Timeout,方便单步调试

        public override void Decode(ByteBuffer bb)
        {
            ServerId = bb.ReadInt();
            GlobalCacheManagerHashIndex = bb.ReadInt();
            DebugMode = bb.ReadBool();
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteInt(ServerId);
            bb.WriteInt(GlobalCacheManagerHashIndex);
            bb.WriteBool(DebugMode);
        }

        public override void ClearParameters()
        {
            ServerId = 0;
            GlobalCacheManagerHashIndex = 0;
            DebugMode = false;
        }
    }

    public sealed class AchillesHeelConfigFromGlobal : Bean
    {
        public int MaxNetPing;
        public int ServerProcessTime;
        public int ServerReleaseTimeout;

        public override void Decode(ByteBuffer bb)
        {
            MaxNetPing = bb.ReadInt();
            ServerProcessTime = bb.ReadInt();
            ServerReleaseTimeout = bb.ReadInt();
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteInt(MaxNetPing);
            bb.WriteInt(ServerProcessTime);
            bb.WriteInt(ServerReleaseTimeout);
        }

        public override void ClearParameters()
        {
            MaxNetPing = 0;
            ServerProcessTime = 0;
            ServerReleaseTimeout = 0;
        }
    }

    public sealed class Login : Rpc<LoginParam, AchillesHeelConfigFromGlobal>
    {
        public readonly static int ProtocolId_ = Util.FixedHash.Hash32(typeof(Login).FullName);

        public override int ModuleId => 0;
        public override int ProtocolId => ProtocolId_;

        public Login()
        {
        }

        public Login(int id)
        {
            Argument.ServerId = id;
        }
    }

    public sealed class ReLogin : Rpc<LoginParam, EmptyBean>
    {
        public readonly static int ProtocolId_ = Util.FixedHash.Hash32(typeof(ReLogin).FullName);

        public override int ModuleId => 0;
        public override int ProtocolId => ProtocolId_;

        public ReLogin()
        {
        }

        public ReLogin(int id)
        {
            Argument.ServerId = id;
        }
    }

    public sealed class NormalClose : Rpc<EmptyBean, EmptyBean>
    {
        public readonly static int ProtocolId_ = Util.FixedHash.Hash32(typeof(NormalClose).FullName);

        public override int ModuleId => 0;
        public override int ProtocolId => ProtocolId_;
    }

    public sealed class AchillesHeel : Bean
    {
        public int ServerId { get; set; } // 必须的。

        public string SecureKey { get; set; } // 安全验证
        public int GlobalCacheManagerHashIndex { get; set; } // 安全验证

        public override void Decode(ByteBuffer bb)
        {
            ServerId = bb.ReadInt();
            SecureKey = bb.ReadString();
            GlobalCacheManagerHashIndex = bb.ReadInt();
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteInt(ServerId);
            bb.WriteString(SecureKey);
            bb.WriteInt(GlobalCacheManagerHashIndex);
        }

        public override void ClearParameters()
        {
            ServerId = 0;
            SecureKey = null;
            GlobalCacheManagerHashIndex = 0;
        }
    }

    public sealed class Cleanup : Rpc<AchillesHeel, EmptyBean>
    {
        public readonly static int ProtocolId_ = Util.FixedHash.Hash32(typeof(Cleanup).FullName);

        public override int ModuleId => 0;
        public override int ProtocolId => ProtocolId_;
    }

    /// <summary>
    /// AchillesHeel!
    /// GlobalAgent 定时发送KeepAlive，只要发现GlobalCacheManager没有相应，
    ///     就释放本地从该GlobalCacheManager申请的资源。
    /// GlobalCacheManager 一定时间（大于客户端发送间隔的两倍）没有收到某个GlobalAgent的KeepAlive，
    ///     就释放该GlobalAgent拥有的资源。【关键】这样定义是否足够，有没有数据安全问题？
    /// 【问题】
    ///     a) 如果GlobalAgent发送KeepAlive的代码死了（不能正确清理本地资源的状态），
    ///     但是其他执行事务的模块还活着，此时就需要把执行事务的模块通过检查一个标志，禁止活动，
    ///     检查这个这个标志在多个GlobalCacheManager时不容易高效实现。
    ///     b) 实行事务时检查标志的代码可能也会某些原因失效，那就更复杂了。
    ///     c) 另外本地要在KeepAlive失败时自动清理，需要记录锁修改状态，并且能正确Checkpoint。
    ///     这在某些异常原因导致本地服务器死掉时很可能无法正常进行。而此时GlobalCacheManager
    ///     超时就清理还是有风险。
    ///     *) 总之，可能的情况太多，KeepAlive还是不够安全。
    ///     所以先不实现了。
    /// </summary>
    public sealed class KeepAlive : Rpc<EmptyBean, EmptyBean>
    {
        public readonly static int ProtocolId_ = Util.FixedHash.Hash32(typeof(KeepAlive).FullName);

        public override int ModuleId => 0;
        public override int ProtocolId => ProtocolId_;
    }
}
