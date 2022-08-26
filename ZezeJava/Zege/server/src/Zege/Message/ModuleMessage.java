package Zege.Message;

import Zege.Program;
import Zeze.Arch.ProviderUserSession;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.OutLong;
import Zeze.Util.TransactionLevelAnnotation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ModuleMessage extends AbstractModule {
    private static final Logger logger = LogManager.getLogger(ModuleMessage.class);

    public void Start(Zege.App app) throws Throwable {
    }

    public void Stop(Zege.App app) throws Throwable {
    }

    @Override
    @TransactionLevelAnnotation(Level=TransactionLevel.None)
    protected long ProcessSendDepartmentMessageRequest(Zege.Message.SendDepartmentMessage r) {
        var session = ProviderUserSession.get(r);

        // 参数检查
        var group = App.Zege_Friend.getGroup(r.Argument.getGroup());
        var groupRoot = group.getRoot();
        if (null == groupRoot)
            return ErrorCode(eGroupNotExist);
        if (r.Argument.getDepartmentId() > 0 && null == group.getDepartmentTreeNode(r.Argument.getDepartmentId()))
            return ErrorCode(eDepartmentNotExist);
        if (0 == r.Argument.getDepartmentId()) {
            if (group.getGroupMembers().size() > App.ZegeConfig.GroupChatLimit)
                return ErrorCode(eTooManyMembers);
        } else {
            if (group.getDepartmentMembers(r.Argument.getDepartmentId()).size() > App.ZegeConfig.GroupChatLimit)
                return ErrorCode(eTooManyMembers);
        }

        // 填充服务器保证的参数
        r.Argument.getMessage().setFrom(session.getAccount());
        r.Argument.getMessage().setGroup(r.Argument.getGroup());
        r.Argument.getMessage().setDepartmentId(r.Argument.getDepartmentId());

        // 保存消息历史。
        var departmentKey = new BDepartmentKey(r.Argument.getGroup(), r.Argument.getDepartmentId());
        var messageRoot = _tDepartementMessage.getOrAdd(departmentKey);
        var messageId = messageRoot.getNextMessageId();
        r.Argument.getMessage().setMessageId(messageId);
        _tDepartementMessages.insert(new BDepartmentMessageKey(departmentKey, messageId), r.Argument.getMessage());
        messageRoot.setNextMessageId(messageId + 1);
        // 统计消息总大小。只计算消息实体，否则需要系列化一次，比较麻烦。
        messageRoot.setMessageTotalBytes(messageRoot.getMessageTotalBytes() + r.Argument.getMessage().getSecureMessage().size());

        // 即时通知。todo 手机通知使用手机服务商消息通知服务；电脑版立即发送整个消息。
        var notify = new NotifyMessage();
        notify.Argument = r.Argument.getMessage();
        if (0 == r.Argument.getDepartmentId()) {
            // group root
            group.getGroupMembers().walk((key, member) -> {
                Program.counters.increment("GroupBroadcastMessage:" + r.Argument.getGroup() + "#" + r.Argument.getDepartmentId());
                if (!session.getAccount().equals((key)))
                    App.Provider.Online.sendAccountWhileCommit(key, notify, null);
                return true;
            });
        } else {
            // department
            group.getDepartmentMembers(r.Argument.getDepartmentId()).walk((key, member) -> {
                Program.counters.increment("GroupBroadcastMessage:" + r.Argument.getGroup() + "#" + r.Argument.getDepartmentId());
                if (!session.getAccount().equals((key)))
                    App.Provider.Online.sendAccountWhileCommit(key, notify, null);
                return true;
            });
        }
        r.Result.setMessageId(messageId);
        session.sendResponseWhileCommit(r);
        Program.counters.increment("GroupMessage:" + r.Argument.getGroup() + "#" + r.Argument.getDepartmentId());
        return Procedure.Success;
    }

    private long saveMessage(String owner, String friend, BMessage message) {
        var messageRoot = _tFriendMessage.getOrAdd(new BFriendKey(owner, friend));
        var messageId = messageRoot.getNextMessageId();
        _tFriendMessages.insert(new BFriendMessageKey(owner, friend, messageId), message);
        messageRoot.setNextMessageId(messageId + 1);
        // 统计消息总大小。只计算消息实体，否则需要系列化一次，比较麻烦。
        messageRoot.setMessageTotalBytes(messageRoot.getMessageTotalBytes() + message.getSecureMessage().size());
        return messageId;
    }

    @Override
    protected long ProcessSendMessageRequest(Zege.Message.SendMessage r) {
        var session = ProviderUserSession.get(r);

        // 参数检查
        var friends = App.Zege_Friend.getFriends(session.getAccount());
        var friend = friends.get(r.Argument.getFriend());
        if (null == friend)
            return ErrorCode(eNotYourFriend);

        // 填充服务器保证的参数
        r.Argument.getMessage().setFrom(session.getAccount());

        // 保存消息历史。
        var self = session.getAccount().equals(r.Argument.getFriend());
        r.Result.setMessageId(saveMessage(session.getAccount(), r.Argument.getFriend(), r.Argument.getMessage()));
        if (!self) // 给自己发消息时不能保存两份。
            r.Argument.getMessage().setMessageId(saveMessage(r.Argument.getFriend(), session.getAccount(), r.Argument.getMessage()));

        // 即时通知。todo 手机通知使用手机服务商消息通知服务；电脑版立即发送整个消息。
        if (!self) {
            var notify = new NotifyMessage();
            notify.Argument = r.Argument.getMessage();
            App.Provider.Online.sendAccountWhileCommit(r.Argument.getFriend(), notify, null);
        }
        session.sendResponseWhileCommit(r);
        Program.counters.increment("FriendMessage");
        return Procedure.Success;
    }

    @Override
    protected long ProcessGetFriendMessageRequest(Zege.Message.GetFriendMessage r) {
        var session = ProviderUserSession.get(r);

        // 参数检查
        var friends = App.Zege_Friend.getFriends(session.getAccount());
        var friend = friends.get(r.Argument.getFriend());
        if (null == friend)
            return ErrorCode(eNotYourFriend);

        // 准备消息范围
        var messageRoot = _tFriendMessage.getOrAdd(new BFriendKey(session.getAccount(), r.Argument.getFriend()));
        var from = new OutLong(r.Argument.getMessageIdFrom());
        var to = new OutLong(r.Argument.getMessageIdTo());
        if (!calculateMessageRange(from, to, messageRoot))
            return ErrorCode(eMessageRange);

        // 提取消息历史
        r.Result.setMessageIdHashRead(messageRoot.getMessageIdHashRead());
        for (; from.Value <= to.Value; ++from.Value) {
            var message = _tFriendMessages.get(new BFriendMessageKey(session.getAccount(), r.Argument.getFriend(), from.Value));
            if (null != message)
                r.Result.getMessages().add(message);
            else
                logger.warn("message not found. id={} owner={} friend={}", from, session.getAccount(), r.Argument.getFriend());
        }
        session.sendResponseWhileCommit(r);
        return Procedure.Success;
    }

    private boolean calculateMessageRange(OutLong from, OutLong to, BMessageRoot messageRoot) {
        if (from.Value == eGetMessageFromAboutRead)
            from.Value = Math.max(from.Value, messageRoot.getMessageIdHashRead() - App.ZegeConfig.AboutHasRead);
        else if (from.Value == eGetMessageFromAboutLast)
            from.Value = Math.max(from.Value, messageRoot.getLastMessageId() - App.ZegeConfig.AboutLast);
        else
            from.Value = Math.max(from.Value, messageRoot.getFirstMessageId());

        if (to.Value == eGetMessageToAuto || to.Value > messageRoot.getLastMessageId())
            to.Value = messageRoot.getLastMessageId();
        if (to.Value < from.Value)
            return false;
        if (to.Value - from.Value > App.ZegeConfig.MessageLimit)
            to.Value = from.Value + App.ZegeConfig.MessageLimit;
        return true;
    }

    @Override
    protected long ProcessGetGroupMessageRequest(Zege.Message.GetGroupMessage r) {
        var session = ProviderUserSession.get(r);

        // 参数检查
        var departmentKey = r.Argument.getGroupDepartment(); // mark with var name
        var group = App.Zege_Friend.getGroup(departmentKey.getGroup());
        var groupRoot = group.getRoot();
        if (null == groupRoot)
            return ErrorCode(eGroupNotExist);
        if (r.Argument.getGroupDepartment().getDepartmentId() > 0
                && null == group.getDepartmentTreeNode(r.Argument.getGroupDepartment().getDepartmentId()))
            return ErrorCode(eDepartmentNotExist);

        // 准备消息范围
        var messageRoot = _tDepartementMessage.getOrAdd(departmentKey);
        var from = new OutLong(r.Argument.getMessageIdFrom());
        var to = new OutLong(r.Argument.getMessageIdTo());
        if (!calculateMessageRange(from, to, messageRoot))
            return ErrorCode(eMessageRange);

        // 提取消息历史
        r.Result.setMessageIdHashRead(messageRoot.getMessageIdHashRead());
        for (; from.Value <= to.Value; ++from.Value) {
            var message = _tDepartementMessages.get(new BDepartmentMessageKey(departmentKey, from.Value));
            if (null != message)
                r.Result.getMessages().add(message);
            else
                logger.warn("message not found. id={} account={} group={} friend={}",
                        from, session.getAccount(), departmentKey.getGroup(), departmentKey.getDepartmentId());
        }
        session.sendResponseWhileCommit(r);
        return Procedure.Success;
    }

    @Override
    protected long ProcessSetFriendMessageHasReadRequest(Zege.Message.SetFriendMessageHasRead r) {
        var session = ProviderUserSession.get(r);

        // 参数检查
        var friends = App.Zege_Friend.getFriends(session.getAccount());
        var friend = friends.get(r.Argument.getFriend());
        if (null == friend)
            return ErrorCode(eNotYourFriend);

        // 检查消息范围
        var messageRoot = _tFriendMessage.getOrAdd(new BFriendKey(session.getAccount(), r.Argument.getFriend()));
        if (r.Argument.getMessageIdHashRead() < messageRoot.getMessageIdHashRead()
                || r.Argument.getMessageIdHashRead() > messageRoot.getLastMessageId())
            return ErrorCode(eMessageRange); // 已读消息只能推进

        messageRoot.setMessageIdHashRead(r.Argument.getMessageIdHashRead());

        // todo 广播已读消息Id给当前登录的所有客户端。
        session.sendResponseWhileCommit(r);
        return Procedure.Success;
    }

    @Override
    protected long ProcessSetGroupMessageHasReadRequest(Zege.Message.SetGroupMessageHasRead r) {
        var session = ProviderUserSession.get(r);

        // 参数检查
        var departmentKey = r.Argument.getGroupDepartment(); // mark with var name
        var group = App.Zege_Friend.getGroup(departmentKey.getGroup());
        var groupRoot = group.getRoot();
        if (null == groupRoot)
            return ErrorCode(eGroupNotExist);
        if (r.Argument.getGroupDepartment().getDepartmentId() > 0
                && null == group.getDepartmentTreeNode(r.Argument.getGroupDepartment().getDepartmentId()))
            return ErrorCode(eDepartmentNotExist);

        // 检查消息范围
        var messageRoot = _tDepartementMessage.getOrAdd(departmentKey);
        if (r.Argument.getMessageIdHashRead() < messageRoot.getMessageIdHashRead()
                || r.Argument.getMessageIdHashRead() > messageRoot.getLastMessageId())
            return ErrorCode(eMessageRange); // 已读消息只能推进

        messageRoot.setMessageIdHashRead(r.Argument.getMessageIdHashRead());

        // todo 广播已读消息Id给当前登录的所有客户端。
        session.sendResponseWhileCommit(r);
        return Procedure.Success;
    }

    // ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleMessage(Zege.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
