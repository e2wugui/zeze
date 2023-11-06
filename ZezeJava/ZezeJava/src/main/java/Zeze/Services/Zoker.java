package Zeze.Services;

import Zeze.Config;
import Zeze.Services.ZokerImpl.FileManager;
import Zeze.Services.ZokerImpl.ProcessManager;
import Zeze.Services.ZokerImpl.ZokerService;

public class Zoker extends AbstractZoker {
    private final ZokerService serverWithConnector; // Zoker是server，但它主动连接ZokerAgent
    private final FileManager fileManager;
    private final ProcessManager processManager;

    public Zoker(Config config, String baseDir) {
        serverWithConnector = new ZokerService(config);
        fileManager = new FileManager(baseDir);
        processManager = new ProcessManager(baseDir);
        RegisterProtocols(serverWithConnector);
    }

    public void start() throws Exception {
        serverWithConnector.start();
    }

    public void stop() throws Exception {
        serverWithConnector.start();
    }

    @Override
    protected long ProcessOpenFileRequest(Zeze.Builtin.Zoker.OpenFile r) throws Exception {
        var fileBin = fileManager.open(r.Argument.getFileName());
        r.Result.setOffset(fileBin.getLength());
        r.SendResult();
        return 0;
    }

    @Override
    protected long ProcessAppendFileRequest(Zeze.Builtin.Zoker.AppendFile r) throws Exception {
        fileManager.append(r.Argument.getFileName(), r.Argument.getOffset(), r.Argument.getChunk());
        r.SendResult();
        return 0;
    }

    @Override
    protected long ProcessCloseFileRequest(Zeze.Builtin.Zoker.CloseFile r) throws Exception {
        if (!fileManager.closeAndVerify(r.Argument.getFileName(), r.Argument.getMd5()))
            return eMd5Mismatch;
        r.SendResult();
        return 0;
    }

    @Override
    protected long ProcessListSerivceRequest(Zeze.Builtin.Zoker.ListSerivce r) throws Exception {
        processManager.listService(r.Result.getServices());
        r.SendResult();
        return 0;
    }

    @Override
    protected long ProcessStartServiceRequest(Zeze.Builtin.Zoker.StartService r) {
        processManager.startService(r);
        r.SendResult();
        return 0;
    }

    @Override
    protected long ProcessStopServiceRequest(Zeze.Builtin.Zoker.StopService r) throws Exception {
        processManager.stopService(r);
        r.SendResult();
        return 0;
    }
}
