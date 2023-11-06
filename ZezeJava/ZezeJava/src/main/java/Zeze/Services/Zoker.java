package Zeze.Services;

import java.io.File;
import Zeze.Builtin.Zoker.BService;
import Zeze.Services.ZokerImpl.FileManager;
import Zeze.Services.ZokerImpl.ProcessManager;

public class Zoker extends AbstractZoker {
    private final FileManager fileManager;
    private final ProcessManager processManager;

    public Zoker(String baseDir) {
        fileManager = new FileManager(baseDir);
        processManager = new ProcessManager();
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
        fileManager.close(r.Argument.getFileName(), r.Argument.getMd5());
        r.SendResult();
        return 0;
    }

    @Override
    protected long ProcessListSerivceRequest(Zeze.Builtin.Zoker.ListSerivce r) throws Exception {
        processManager.listService(fileManager.getBaseDir(), r.Result.getServices());
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
