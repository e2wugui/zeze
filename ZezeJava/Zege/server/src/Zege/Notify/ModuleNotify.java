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
        App.Provider.online.sendAccount(account, notify, null); // TODO online sender
    }

    public void Start(Zege.App app) throws Throwable {
        App.LinkedMaps.NodeListeners.put(eNotifyLinkedMapNameEndsWith, this::onChangeListener);
    }

    public void Stop(Zege.App app) throws Throwable {
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

    // ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleNotify(Zege.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
