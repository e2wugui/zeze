package Zege;

import Zege.Friend.*;
import Zege.Message.*;
import Zeze.Builtin.LinkdBase.BReportError;
import Zeze.Builtin.Provider.Dispatch;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.OutLong;

public class LinkdService extends LinkdServiceBase {
    public LinkdService(Zeze.Application zeze) throws Throwable {
        super(zeze);
    }

    private boolean ChoiceHashSend(int hash, int moduleId, Dispatch dispatch) {
        var provider = new OutLong();
        if (LinkdApp.LinkdProvider.ChoiceHashWithoutBind(moduleId, hash, provider)) {
            var providerSocket = LinkdApp.LinkdProviderService.GetSocket(provider.Value);
            if (null != providerSocket) {
                // ChoiceProviderAndBind 内部已经处理了绑定。这里只需要发送。
                return providerSocket.Send(dispatch);
            }
        }
        return false;
    }

    // 所有的群相关协议的参数的第一个变量必须都是Group: type==String，variable.id==1，
    private String DecodeGroup(Zeze.Serialize.ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            return _o_.ReadString(_t_);
        }
        throw new RuntimeException("Group Not Found.");
    }

    private boolean DispatchGroupProtocol(String group, int moduleId, Dispatch dispatch) {
        return ChoiceHashSend(Hash(group), moduleId, dispatch);
    }

    private int Hash(String group) {
        return ByteBuffer.calc_hashnr(group);
    }

    @Override
    public void DispatchUnknownProtocol(Zeze.Net.AsyncSocket so, int moduleId, int protocolId, Zeze.Serialize.ByteBuffer data) {
        var linkSession = getAuthedSession(so);
        setStableLinkSid(linkSession, so, moduleId, protocolId, data);

        var dispatch = createDispatch(linkSession, so, moduleId, protocolId, data);

        // 拦截部分协议，按协议方式转发。
        switch (moduleId) {
        case ModuleFriend.ModuleId:
            switch (protocolId) {
            case AddDepartmentMember.ProtocolId_:
            case CreateDepartment.ProtocolId_:
            case DeleteDepartment.ProtocolId_:
            case GetDepartmentMemberNode.ProtocolId_:
            case GetDepartmentNode.ProtocolId_:
            case GetGroupMemberNode.ProtocolId_:
            case GetGroupRoot.ProtocolId_:
            case MoveDepartment.ProtocolId_:
                if (DispatchGroupProtocol(DecodeGroup(data), moduleId, dispatch))
                    return; // 失败尝试继续走默认流程?
                break;
            }
            break;

        case ModuleMessage.ModuleId:
            switch (protocolId) {
            case SendDepartmentMessage.ProtocolId_:
                if (DispatchGroupProtocol(DecodeGroup(data), moduleId, dispatch))
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
        ReportError(so.getSessionId(), BReportError.FromLink, BReportError.CodeNoProvider, "no provider.");
    }
}
