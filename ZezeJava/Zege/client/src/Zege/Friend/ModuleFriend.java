package Zege.Friend;

public class ModuleFriend extends AbstractModule {
    public void Start(Zege.App app) throws Throwable {
    }

    public void Stop(Zege.App app) throws Throwable {
    }

    public BFriendNode getFriendNode(long nodeId) {
        var req = new GetFriendNode();
        req.Argument.setNodeId(nodeId);
        req.SendForWait(App.Connector.GetReadySocket()).await();
        return req.Result;
    }

    // ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleFriend(Zege.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
