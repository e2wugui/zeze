package Zege.Notify;

import Zeze.Arch.ProviderUserSession;
import Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeKey;
import Zeze.Collections.LinkedMap;
import Zeze.Net.Binary;
import Zeze.Transaction.Changes;
import Zeze.Transaction.Procedure;
import Zeze.Util.OutLong;

public class ModuleNotify extends AbstractModule {
    public static final String eNotifyLinkedMapNameEndsWith = "@Zege.Notify";

    private void onChangeListener(Object key, Changes.Record r) {
        var nodeKey = (BLinkedMapNodeKey)key; // 这里带了LinkedMap#Name
        var indexOf = nodeKey.getName().lastIndexOf('@');
        var account = nodeKey.getName().substring(0, indexOf);
        var notify = new NotifyNodeLogBeanNotify();
        var encoded = App.LinkedMaps.encodeChangeListenerWithSpecialName(nodeKey.getName(), key, r);
        notify.Argument.setChangeLog(new Binary(encoded));
        App.Provider.getOnline().sendAccount(account, notify); // TODO online sender
    }

    public void Start(Zege.App ignoredApp) {
        App.LinkedMaps.NodeListeners.put(eNotifyLinkedMapNameEndsWith, this::onChangeListener);
    }

    public void Stop(Zege.App ignoredApp) {
        App.LinkedMaps.NodeListeners.remove(eNotifyLinkedMapNameEndsWith);
    }

    public LinkedMap<BNotify> getNotify(String owner) {
        return App.LinkedMaps.open(owner + eNotifyLinkedMapNameEndsWith, BNotify.class, App.ZegeConfig.NotifyCountPerNode);
    }

    @Override
    protected long ProcessGetNotifyNodeRequest(Zege.Notify.GetNotifyNode r) {
        var session = ProviderUserSession.get(r);
        var notify = getNotify(session.getAccount());
        var nodeId = new OutLong(r.Argument.getNodeId());
        var node = nodeId.value == 0 ? notify.getFirstNode(nodeId) : notify.getNode(nodeId.value);
        if (node == null)
            return errorCode(eNotifyNodeNotFound);

        r.Result.setNodeKey(new BLinkedMapNodeKey(notify.getName(), nodeId.value));
        r.Result.setNode(node);

        session.sendResponseWhileCommit(r);
        return Procedure.Success;
    }


    public static String makeNotifyId(int type, String account) {
        return type + "@" + account;
    }

    @Override
    protected long ProcessSendNotifyRequest(Zege.Notify.SendNotify r) {
        var session = ProviderUserSession.get(r);

        for (var e : r.Argument.getNotifys().entrySet()) {
            var notify = getNotify(e.getKey());
            if (null != notify)
                notify.put(makeNotifyId(BNotify.eTypeGroupCert, r.Argument.getGroup()), e.getValue());
        }

        session.sendResponseWhileCommit(r);
        return 0;
    }

    @Override
    protected long ProcessRemoveNotifyRequest(Zege.Notify.RemoveNotify r) {
        var session = ProviderUserSession.get(r);

        var n = getNotify(session.getAccount());
        n.remove(makeNotifyId(r.Argument.getType(), r.Argument.getAccount()));

        session.sendResponseWhileCommit(r);
        return 0;
    }

    // ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleNotify(Zege.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
