// auto-generated

namespace Zege.Friend
{
    public partial class ModuleFriend : AbstractModule
    {
        public const int ModuleId = 2;


        public global::Zege.App App { get; }

        public ModuleFriend(global::Zege.App app)
        {
            App = app;
        }

        public override void Register()
        {
            // register protocol factory and handles
            var _reflect = new Zeze.Util.Reflect(GetType());
            App.ClientService.AddFactoryHandle(11819964782, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zege.Friend.AcceptFriend(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessAcceptFriendResponse", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessAcceptFriendResponse", Zeze.Transaction.DispatchMode.Normal),
            });
            App.ClientService.AddFactoryHandle(11834875247, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zege.Friend.AddDepartmentMember(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessAddDepartmentMemberResponse", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessAddDepartmentMemberResponse", Zeze.Transaction.DispatchMode.Normal),
            });
            App.ClientService.AddFactoryHandle(10466336201, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zege.Friend.AddFriend(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessAddFriendResponse", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessAddFriendResponse", Zeze.Transaction.DispatchMode.Normal),
            });
            App.ClientService.AddFactoryHandle(9244896130, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zege.Friend.AddManager(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessAddManagerResponse", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessAddManagerResponse", Zeze.Transaction.DispatchMode.Normal),
            });
            App.ClientService.AddFactoryHandle(12037733736, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zege.Friend.CreateDepartment(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessCreateDepartmentResponse", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessCreateDepartmentResponse", Zeze.Transaction.DispatchMode.Normal),
            });
            App.ClientService.AddFactoryHandle(12723706702, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zege.Friend.CreateGroup(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessCreateGroupResponse", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessCreateGroupResponse", Zeze.Transaction.DispatchMode.Normal),
            });
            App.ClientService.AddFactoryHandle(10185941729, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zege.Friend.DelDepartmentMember(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessDelDepartmentMemberResponse", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessDelDepartmentMemberResponse", Zeze.Transaction.DispatchMode.Normal),
            });
            App.ClientService.AddFactoryHandle(10230563447, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zege.Friend.DeleteDepartment(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessDeleteDepartmentResponse", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessDeleteDepartmentResponse", Zeze.Transaction.DispatchMode.Normal),
            });
            App.ClientService.AddFactoryHandle(9146378554, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zege.Friend.DeleteFriend(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessDeleteFriendResponse", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessDeleteFriendResponse", Zeze.Transaction.DispatchMode.Normal),
            });
            App.ClientService.AddFactoryHandle(10473353543, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zege.Friend.DeleteManager(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessDeleteManagerResponse", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessDeleteManagerResponse", Zeze.Transaction.DispatchMode.Normal),
            });
            App.ClientService.AddFactoryHandle(11349224353, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zege.Friend.DenyFriend(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessDenyFriendResponse", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessDenyFriendResponse", Zeze.Transaction.DispatchMode.Normal),
            });
            App.ClientService.AddFactoryHandle(9047838667, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zege.Friend.FriendNodeLogBeanNotify(),
                Handle = ProcessFriendNodeLogBeanNotify,
                TransactionLevel = _reflect.GetTransactionLevel("ProcessFriendNodeLogBeanNotifyp", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessFriendNodeLogBeanNotify", Zeze.Transaction.DispatchMode.Normal),
            });
            App.ClientService.AddFactoryHandle(8754780398, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zege.Friend.GetDepartmentMemberNode(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessGetDepartmentMemberNodeResponse", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessGetDepartmentMemberNodeResponse", Zeze.Transaction.DispatchMode.Normal),
            });
            App.ClientService.AddFactoryHandle(11048304875, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zege.Friend.GetDepartmentNode(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessGetDepartmentNodeResponse", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessGetDepartmentNodeResponse", Zeze.Transaction.DispatchMode.Normal),
            });
            App.ClientService.AddFactoryHandle(10833531815, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zege.Friend.GetFriendNode(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessGetFriendNodeResponse", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessGetFriendNodeResponse", Zeze.Transaction.DispatchMode.Normal),
            });
            App.ClientService.AddFactoryHandle(9917212429, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zege.Friend.GetGroupMemberNode(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessGetGroupMemberNodeResponse", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessGetGroupMemberNodeResponse", Zeze.Transaction.DispatchMode.Normal),
            });
            App.ClientService.AddFactoryHandle(11322645231, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zege.Friend.GetGroupRoot(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessGetGroupRootResponse", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessGetGroupRootResponse", Zeze.Transaction.DispatchMode.Normal),
            });
            App.ClientService.AddFactoryHandle(9253156986, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zege.Friend.GetPublicUserInfo(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessGetPublicUserInfoResponse", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessGetPublicUserInfoResponse", Zeze.Transaction.DispatchMode.Normal),
            });
            App.ClientService.AddFactoryHandle(11348545708, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zege.Friend.GetPublicUserInfos(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessGetPublicUserInfosResponse", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessGetPublicUserInfosResponse", Zeze.Transaction.DispatchMode.Normal),
            });
            App.ClientService.AddFactoryHandle(10306996889, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zege.Friend.GetPublicUserPhoto(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessGetPublicUserPhotoResponse", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessGetPublicUserPhotoResponse", Zeze.Transaction.DispatchMode.Normal),
            });
            App.ClientService.AddFactoryHandle(10376408157, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zege.Friend.MoveDepartment(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessMoveDepartmentResponse", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessMoveDepartmentResponse", Zeze.Transaction.DispatchMode.Normal),
            });
            App.ClientService.AddFactoryHandle(10767767241, new Zeze.Net.Service.ProtocolFactoryHandle()
            {
                Factory = () => new Zege.Friend.SetTopmost(),
                TransactionLevel = _reflect.GetTransactionLevel("ProcessSetTopmostResponse", Zeze.Transaction.TransactionLevel.Serializable),
                Mode = _reflect.GetDispatchMode("ProcessSetTopmostResponse", Zeze.Transaction.DispatchMode.Normal),
            });
            // register table
        }

        public override void UnRegister()
        {
            App.ClientService.Factorys.TryRemove(11819964782, out var _);
            App.ClientService.Factorys.TryRemove(11834875247, out var _);
            App.ClientService.Factorys.TryRemove(10466336201, out var _);
            App.ClientService.Factorys.TryRemove(9244896130, out var _);
            App.ClientService.Factorys.TryRemove(12037733736, out var _);
            App.ClientService.Factorys.TryRemove(12723706702, out var _);
            App.ClientService.Factorys.TryRemove(10185941729, out var _);
            App.ClientService.Factorys.TryRemove(10230563447, out var _);
            App.ClientService.Factorys.TryRemove(9146378554, out var _);
            App.ClientService.Factorys.TryRemove(10473353543, out var _);
            App.ClientService.Factorys.TryRemove(11349224353, out var _);
            App.ClientService.Factorys.TryRemove(9047838667, out var _);
            App.ClientService.Factorys.TryRemove(8754780398, out var _);
            App.ClientService.Factorys.TryRemove(11048304875, out var _);
            App.ClientService.Factorys.TryRemove(10833531815, out var _);
            App.ClientService.Factorys.TryRemove(9917212429, out var _);
            App.ClientService.Factorys.TryRemove(11322645231, out var _);
            App.ClientService.Factorys.TryRemove(9253156986, out var _);
            App.ClientService.Factorys.TryRemove(11348545708, out var _);
            App.ClientService.Factorys.TryRemove(10306996889, out var _);
            App.ClientService.Factorys.TryRemove(10376408157, out var _);
            App.ClientService.Factorys.TryRemove(10767767241, out var _);
        }
    }
}
