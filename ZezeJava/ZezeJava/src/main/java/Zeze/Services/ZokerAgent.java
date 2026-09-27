package Zeze.Services;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Zoker.AppendFile;
import Zeze.Builtin.Zoker.BListServiceResult;
import Zeze.Builtin.Zoker.BService;
import Zeze.Builtin.Zoker.CloseFile;
import Zeze.Builtin.Zoker.CommitService;
import Zeze.Builtin.Zoker.ListService;
import Zeze.Builtin.Zoker.OpenFile;
import Zeze.Builtin.Zoker.StartService;
import Zeze.Builtin.Zoker.StopService;
import Zeze.Config;
import Zeze.IModule;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Services.ZokerImpl.ZokerAgentService;
import org.jetbrains.annotations.NotNull;

public class ZokerAgent extends AbstractZokerAgent {
    private final ZokerAgentService clientWithAcceptor; // Zoker是server，但是Zoker主动连接client。
    private final ConcurrentHashMap<String, AsyncSocket> zokers = new ConcurrentHashMap<>();

    public ZokerAgent(Config config) {
        clientWithAcceptor = new ZokerAgentService(this, config);
        RegisterProtocols(clientWithAcceptor);
    }

    public void start() throws Exception {
        clientWithAcceptor.start();
    }

    public void stop() throws Exception {
        clientWithAcceptor.stop();
    }

    public ConcurrentHashMap<String, AsyncSocket> zokers() {
        return zokers;
    }

    /**
     * GE-C03（FND21）：注册表存活校验+死条目接管。注册条目的唯一常规出口是旧连接
     * OnSocketClose 的 remove；从连接死亡（{@code isClosed} 已置位）到该回调被执行存在窗口
     * （半开连接可达 keepalive 检查周期，KeepCheckPeriod/KeepRecvTimeout 未配置时更长），
     * 期间 daemon 重连的 Register 被 putIfAbsent 恒拒 eDuplicateZoker——zokerName 被死条目
     * 锁死，manager 侧 getZoker 恒抛且无任何重注册/接管路径。修复：putIfAbsent 冲突时检查
     * 现存 socket——已死则 CAS 接管（replace 失败=并发注册已改写条目，重读重试：新主人已死
     * 可再接管、活着则是真重复，循环必收敛）；仍活着才是真重复。接管成功后旧连接迟到的
     * close 回调由 {@link ZokerAgentService#OnSocketClose} 的条件移除兜底，不会误摘继承者条目。
     */
    @Override
    protected long ProcessRegisterRequest(Zeze.Builtin.Zoker.Register r) {
        var sender = r.getSender();
        var zokerName = r.Argument.getZokerName();
        while (true) {
            var old = zokers.putIfAbsent(zokerName, sender);
            if (null == old)
                break; // 空位直接注册
            if (!old.isClosed())
                return eDuplicateZoker; // 现存 socket 活着：真重复
            if (zokers.replace(zokerName, old, sender))
                break; // 现存 socket 已死：接管
            // CAS 失败：并发注册已改写条目——重读评估
        }
        // GE-C03(FND22)：同 socket 换名注册——旧名条目条件摘除（值仍是本 socket 才摘）。
        // userState 单值只记末名，OnSocketClose 按它条件移除也只摘一个：换名前的条目永久滞留
        // （值指向已关闭 socket），无认证 acceptor 上单连接 Register 洪泛=无界内存增长。
        // 条件移除（remove(prev, sender)）防误摘：旧名若已被他方接管（本 socket 曾死过、
        // FND21 的 CAS replace），值不是本 socket，不摘继承者。摘旧在装新成功之后：与
        // FND21 的"userState 前极小关闭窗"同形态自愈——窗口内关闭时新名条目由下次同名
        // Register 的 isClosed 接管回收，有界。
        var prev = (String) sender.getUserState();
        if (null != prev && !prev.equals(zokerName))
            zokers.remove(prev, sender);
        sender.setUserState(zokerName);
        r.SendResult();
        return 0;
    }

    private @NotNull AsyncSocket getZoker(String zokerName) {
        var zoker = zokers.get(zokerName);
        if (null == zoker)
            throw new RuntimeException("zoker not exist. " + zokerName);
        return zoker;
    }

    public long openFile(String zokerName, String fileName) {
        var zoker = getZoker(zokerName);
        var r = new OpenFile();
        r.Argument.setFileName(fileName);
        r.SendForWait(zoker).await();
        if (r.getResultCode() != 0)
            throw new RuntimeException("open file error. " + IModule.getErrorCode(r.getResultCode()));

        // 更多返回结果，修改 BOpenFileResult 并修改返回值。
        return r.Result.getOffset();
    }

    public void appendFile(String zokerName, String fileName, long offset, byte[] data) {
        appendFile(zokerName, fileName, offset, data, 0, data.length);
    }

    public void appendFile(String zokerName, String fileName, long offset, byte[] data, int dataOffset, int dataLength) {
        var zoker = getZoker(zokerName);
        var r = new AppendFile();
        r.Argument.setFileName(fileName);
        r.Argument.setOffset(offset);
        r.Argument.setChunk(new Binary(data, dataOffset, dataLength));
        r.SendForWait(zoker).await();
        if (r.getResultCode() != 0)
            throw new RuntimeException("append file error. " + IModule.getErrorCode(r.getResultCode()));
        // 需要返回结果，修改 BAppendFileResult
    }

    public void closeFile(String zokerName, String fileName, Binary md5) {
        var zoker = getZoker(zokerName);
        var r = new CloseFile();
        r.Argument.setFileName(fileName);
        r.Argument.setMd5(md5);
        r.SendForWait(zoker).await();
        if (r.getResultCode() != 0)
            throw new RuntimeException("close file error. " + IModule.getErrorCode(r.getResultCode()));
    }

    /**
     * 发布服务到zoker上。
     * @param zokerName zoker name
     * @param localServiceHome local service home 服务文件目录所在的文件夹
     * @param serviceName service name
     * @param versionNo version no 将被当作文件路径名，不能包含文件路径不支持的字符。
     */
    public void distributeService(String zokerName, File localServiceHome, String serviceName, String versionNo) throws Exception {
        var serviceDir = new File(localServiceHome, serviceName);
        if (!serviceDir.isDirectory() || !serviceDir.exists())
            throw new RuntimeException("service dir error.");
        distributeService(zokerName, localServiceHome.toPath(), serviceDir);
        var r = new CommitService();
        r.Argument.setServiceName(serviceName);
        r.Argument.setVersionNo(versionNo);
        // 应答路径含 pruneVersions 整树删除（服务包体量+磁盘速度无界）——默认 5s 会把成功部署
        // 当失败（stopService 同族，27e4dfcd3 先例），60s=部署级操作裕量。
        r.SendForWait(getZoker(zokerName), 60_000).await();
        if (r.getResultCode() != 0)
            throw new RuntimeException("commit service error=" + IModule.getErrorCode(r.getResultCode()));
    }

    private static void md5To(MessageDigest md5, BufferedInputStream bis, long toOffset) throws IOException {
        var buffer = new byte[16 * 1024];
        while (toOffset > 0) {
            var tryReadLen = (int)(toOffset > buffer.length ? buffer.length : toOffset);
            var rc = bis.read(buffer, 0, tryReadLen);
            if (rc >= 0) {
                toOffset -= rc;
                md5.update(buffer, 0, rc);
                continue;
            }
            throw new RuntimeException("md5To not enough data");
        }
    }

    private void distributeService(String zokerName, Path localServiceHome, File serviceDir) throws Exception {
        var files = serviceDir.listFiles();
        if (null == files)
            return;

        for (var file : files) {
            if (file.isDirectory()) {
                distributeService(zokerName, localServiceHome, file);
                continue;
            }
            var fileRelativeName = localServiceHome.relativize(file.toPath()).toString().replace("\\", "/");
            var md5 = MessageDigest.getInstance("MD5");
            var fileOffset = openFile(zokerName, fileRelativeName);
            try (var bis = new BufferedInputStream(new FileInputStream(file))) {
                md5To(md5, bis, fileOffset);
                var buffer = new byte[16 * 1024];
                var rc = 0;
                while ((rc = bis.read(buffer)) >= 0) {
                    md5.update(buffer, 0, rc);
                    appendFile(zokerName, fileRelativeName, fileOffset, buffer, 0, rc);
                    fileOffset += rc;
                }
            } catch (Exception primary) {
                // 传输已失败：closeFile的失败（部分digest的eMd5Mismatch/断链回收后的eNotOpened）
                // 是预期伴生，压制为suppressed，不得顶替原始异常（GE-D04）。
                try {
                    closeFile(zokerName, fileRelativeName, new Binary(md5.digest()));
                } catch (Exception suppressed) {
                    primary.addSuppressed(suppressed);
                }
                throw primary;
            }
            closeFile(zokerName, fileRelativeName, new Binary(md5.digest())); // 正常路径直线收尾，失败照抛
        }
    }

    public BListServiceResult.Data listService(String zokerName) {
        var zoker = getZoker(zokerName);
        var r = new ListService();
        r.SendForWait(zoker).await();
        if (r.getResultCode() != 0)
            throw new RuntimeException("list service error. " + IModule.getErrorCode(r.getResultCode()));
        return r.Result;
    }

    public BService.Data startService(String zokerName, String serviceName) {
        var zoker = getZoker(zokerName);
        var r = new StartService();
        r.Argument.setServiceName(serviceName);
        r.SendForWait(zoker).await();
        if (r.getResultCode() != 0)
            throw new RuntimeException("start service error. " + IModule.getErrorCode(r.getResultCode()));
        return r.Result;
    }

    public BService.Data stopService(String zokerName, String serviceName, boolean force) {
        var zoker = getZoker(zokerName);
        var r = new StopService();
        r.Argument.setServiceName(serviceName);
        r.Argument.setForce(force);
        // 服务端 stop 三态最坏路径 10s 优雅+10s 强杀（ServiceManager），默认 5s 超时会让
        // Force-Killed/Alive-After-Force 两态不可达（迟到结果包被丢上下文）。60s=最坏路径3倍裕量，
        // 对齐 00fd190c0 的 60s 族先例。
        r.SendForWait(zoker, 60_000).await();
        if (r.getResultCode() != 0)
            throw new RuntimeException("stop service error. " + IModule.getErrorCode(r.getResultCode()));
        return r.Result;
    }
}
