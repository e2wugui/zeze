package Zege;

import Zege.Friend.*;
import Zege.Message.*;
import Zege.User.BAccount;
import Zeze.Builtin.LinkdBase.BReportError;
import Zeze.Builtin.Provider.Dispatch;
import Zeze.Net.Rpc;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.EmptyBean;
import Zeze.Transaction.Record;
import Zeze.Util.OutLong;
import Zeze.Util.Random;

public class LinkdService extends LinkdServiceBase {
    public LinkdService(Zeze.Application zeze) throws Throwable {
        super(zeze);
    }

    private boolean ChoiceHashSend(int hash, int moduleId, Dispatch dispatch) {
        var provider = new OutLong();
        if (linkdApp.linkdProvider.choiceHashWithoutBind(moduleId, hash, provider)) {
            var providerSocket = linkdApp.linkdProviderService.GetSocket(provider.value);
            if (null != providerSocket) {
                // ChoiceProviderAndBind 内部已经处理了绑定。这里只需要发送。
                return providerSocket.Send(dispatch);
            }
        }
        return false;
    }

    public static class GroupDepartmentId extends Zeze.Transaction.Bean {
        public String Group;
        public long DepartmentId;

        @Override
        public void encode(ByteBuffer bb) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void decode(ByteBuffer bb) {
            int _t_ = bb.ReadByte();
            int _i_ = bb.ReadTagSize(_t_);
            if (_i_ == 1) {
                Group = bb.ReadString(_t_);
                _i_ += bb.ReadTagSize(_t_ = bb.ReadByte());
            }
            if (_i_ == 2) {
                DepartmentId = bb.ReadLong(_t_);
                _i_ += bb.ReadTagSize(_t_ = bb.ReadByte());
            }
            // 由于Group,DepartmentId默认值时，不会Encode任何东西，这里就不做是否存在值的验证了。
        }

        @Override
        protected void initChildrenRootInfo(Record.RootInfo root) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void resetChildrenRootInfo() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int hashCode() {
            final int _prime_ = 31;
            int _h_ = 0;
            _h_ = _h_ * _prime_ + Group.hashCode();
            _h_ = _h_ * _prime_ + Long.hashCode(DepartmentId);
            return _h_;
        }
    }

    public static class RpcGroupDepartmentId extends Rpc<GroupDepartmentId, EmptyBean> {
        @Override
        public int getModuleId() {
            return 0;
        }

        @Override
        public int getProtocolId() {
            return 0;
        }

        public RpcGroupDepartmentId() {
            Argument = new GroupDepartmentId();
            Result = EmptyBean.instance;
        }
    }

    // 所有的群相关协议的参数的第一个变量必须都是Group: type==String，variable.id==1，
    private int DecodeGroupDepartmentIdHash(Zeze.Serialize.ByteBuffer bb) {
        var rpc = new RpcGroupDepartmentId();
        rpc.decode(bb);
        return rpc.Argument.hashCode();
    }

    public static class RpcAccount extends Rpc<BAccount, EmptyBean> {
        @Override
        public int getModuleId() {
            return 0;
        }

        @Override
        public int getProtocolId() {
            return 0;
        }

        public RpcAccount() {
            Argument = new BAccount();
            Result = EmptyBean.instance;
        }
    }
    private int DecodeAccountHash(Zeze.Serialize.ByteBuffer bb) {
        var rpc = new RpcAccount();
        rpc.decode(bb);
        // 必须和Arch\LinkdProvider.java::ChoiceProviderAndBind中的ChoiceTypeHashAccount方式的hash方式一样。
        return ByteBuffer.calc_hashnr(rpc.Argument.getAccount());
    }

    @Override
    public void dispatchUnknownProtocol(Zeze.Net.AsyncSocket so, int moduleId, int protocolId, Zeze.Serialize.ByteBuffer data) {
        var linkSession = getAuthedSession(so);
        setStableLinkSid(linkSession, so, moduleId, protocolId, data);

        var dispatch = createDispatch(linkSession, so, moduleId, protocolId, data);

        // 拦截部分协议，按协议方式转发。
        switch (moduleId) {
        case ModuleFriend.ModuleId:
            switch (protocolId) {
            case CreateGroup.ProtocolId_:
                // 创建群，随机找一台服务器。
                if (ChoiceHashSend(Random.getInstance().nextInt(), moduleId, dispatch))
                    return; // 失败尝试继续走默认流程?
                break;

            case GetGroupMemberNode.ProtocolId_:
            case CreateDepartment.ProtocolId_:
            case AddDepartmentMember.ProtocolId_:
            case DeleteDepartment.ProtocolId_:
            case GetDepartmentMemberNode.ProtocolId_:
            case GetDepartmentNode.ProtocolId_:
            case GetGroupRoot.ProtocolId_:
            case MoveDepartment.ProtocolId_:
                if (ChoiceHashSend(DecodeGroupDepartmentIdHash(data), moduleId, dispatch))
                    return; // 失败尝试继续走默认流程?
                break;

            case GetPublicUserInfo.ProtocolId_:
            case GetPublicUserPhoto.ProtocolId_:
                if (ChoiceHashSend(DecodeAccountHash(data), moduleId, dispatch))
                    return; // 失败尝试继续走默认流程?
                break;
            }
            break;

        case ModuleMessage.ModuleId:
            switch (protocolId) {
            case SendDepartmentMessage.ProtocolId_:
                if (ChoiceHashSend(DecodeGroupDepartmentIdHash(data), moduleId, dispatch))
                    return; // 失败尝试继续走默认流程?
                break;
            }
            break;
        }
        // default dispatch
        if (findSend(linkSession, moduleId, dispatch))
            return;
        if (choiceBindSend(so, moduleId, dispatch))
            return;
        reportError(so.getSessionId(), BReportError.FromLink, BReportError.CodeNoProvider, "no provider.");
    }
}
