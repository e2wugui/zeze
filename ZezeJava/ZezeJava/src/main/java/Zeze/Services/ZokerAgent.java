package Zeze.Services;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
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

/**
 * Zoker 的本地代理：维持与各 Zoker 的注册连接，代理文件分发与服务生命周期 RPC。
 */
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
     * 注册表存活校验+死条目接管+自归属重放幂等。注册条目的唯一常规出口是旧连接
     * OnSocketClose 的 remove；从连接死亡（{@code isClosed} 已置位）到该回调被执行存在窗口
     * （半开连接可达 keepalive 检查周期，KeepCheckPeriod/KeepRecvTimeout 未配置时更长），
     * 期间 daemon 重连的 Register 被 putIfAbsent 恒拒 eDuplicateZoker——zokerName 被死条目
     * 锁死，manager 侧 getZoker 恒抛且无任何重注册/接管路径。putIfAbsent 冲突时先判自归属
     * （old==sender：同连接对已注册名的重放，客户端 RPC 超时重试的常规形态——幂等成功，
     * 不误报名字冲突），再检查现存 socket——已死则 CAS 接管（replace 失败=并发注册已改写
     * 条目，重读重试：新主人已死可再接管、活着则是真重复，循环必收敛）；仍活着才是真
     * 重复。接管成功后旧连接迟到的 close 回调由
     * {@link ZokerAgentService#OnSocketClose} 的条件移除兜底，不会误摘继承者条目。
     */
    @Override
    protected long ProcessRegisterRequest(Zeze.Builtin.Zoker.Register r) {
        var sender = r.getSender();
        var zokerName = r.Argument.getZokerName();
        var registered = registeredNames(sender);
        // 摘旧面快照（装账时刻之前本连接已注册的旧名，不含本次名）：Register为Normal派发
        //（线程池并发），同连接两帧可在不同池线程并发执行——摘旧若遍历活集合，A的摘旧会看见
        // B在A快照之后装账的新名并互摘（值恰为本socket，remove(name,sender)命中），已应答
        // 成功的注册被静默丢弃（getZoker恒抛，长连接下无自愈）。快照取在装账循环之前：
        // 并发双方各自的摘旧面都不含对方新名，互不摘除、两名并存（断链时OnSocketClose按
        // 集合全量条件摘除，零滞留）；顺序到达的换名注册，后到者的快照必含旧名，
        // 换名摘除语义不变。RegisteredNames仍记全部历史名（不退回单值记忆：并发交错/换名-
        // 断链交错下漏摘的条目值指向已死socket永久滞留，无认证acceptor上Register洪泛=无界增长）。
        var staleNames = new ArrayList<String>();
        registered.forEach(name -> {
            if (!name.equals(zokerName))
                staleNames.add(name);
        });
        while (true) {
            var old = zokers.putIfAbsent(zokerName, sender);
            if (null == old)
                break; // 空位直接注册
            if (old == sender)
                // 自归属重放：同连接对自己已注册名的重发（客户端 RPC 超时重试的常规形态），
                // 幂等成功应答——误回 eDuplicateZoker 会让重试型客户端在同连接上永不收敛。
                // （摘旧面快照不含本次名、registered.add 幂等、SendResult 照发。）
                break;
            if (!old.isClosed())
                return eDuplicateZoker; // 他方活连接占用：真重复
            if (zokers.replace(zokerName, old, sender))
                break; // 现存 socket 已死：接管
            // CAS 失败：并发注册已改写条目——重读评估
        }
        // 条件移除（remove(name, sender)）防误摘：旧名若已被他方接管（本 socket 曾死过、
        // 条目被 CAS 接管），值不是本 socket，不摘继承者。摘旧在装新成功之后：
        // 装新后、入集合前的极小关闭窗内关闭时，新名条目由下次同名
        // Register 的 isClosed 接管回收，有界。摘除面=上方快照（装账前旧名）。
        for (var name : staleNames)
            zokers.remove(name, sender);
        registered.add(zokerName);
        r.SendResult();
        return 0;
    }

    /**
     * 取本连接的名字集合（userState 载荷）。网络路径由 {@link ZokerAgentService#OnSocketAccept}
     * 预装——accept 先于本连接任何协议派发，Register 到达时集合必已就位，无并发补装；
     * 未走 accept 的桩形态（null-service 直构）惰性补装，仅存在于单线程直调场景。
     */
    private static RegisteredNames registeredNames(AsyncSocket sender) {
        if (sender.getUserState() instanceof RegisteredNames names)
            return names;
        var created = new RegisteredNames();
        sender.setUserState(created);
        return created;
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
        // 断点续传时服务端对已存在文件全量读盘算md5（FileBin构造），应答随文件体积线性增长
        // ——默认5s会把大文件续传当失败（commit/stopService 同族），60s=部署级操作裕量。
        // append/close 已同放宽至60s（FND26 zoker-04，见各自调用点：truncate路径并非毫秒级）。
        r.SendForWait(zoker, 60_000).await();
        if (r.getResultCode() != 0)
            throw new RuntimeException("open file error. " + IModule.getErrorCode(r.getResultCode()));

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
        // offset回退（服务端残留长于本地续传点）或并发同文件分发时，服务端append走truncate路径：
        // FileBin监视器内truncate+全量md5重算（GB级残留、慢盘可达数十秒），并发append还要排队等
        // 监视器——默认5s会把照常完成的append当失败中断整个distribute（openFile/commit同族），
        // 60s=部署级操作裕量。
        r.SendForWait(zoker, 60_000).await();
        if (r.getResultCode() != 0)
            throw new RuntimeException("append file error. " + IModule.getErrorCode(r.getResultCode()));
    }

    public void closeFile(String zokerName, String fileName, Binary md5) {
        var zoker = getZoker(zokerName);
        var r = new CloseFile();
        r.Argument.setFileName(fileName);
        r.Argument.setMd5(md5);
        // close要拿FileBin监视器（close内flush落盘）：可能排在并发append的truncate全量md5重算
        // （GB级、慢盘数十秒）之后——默认5s会把照常完成的收尾当失败（appendFile/openFile同族），
        // 对齐部署级60s。
        r.SendForWait(zoker, 60_000).await();
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
        var uploaded = new ArrayList<String>();
        distributeService(zokerName, localServiceHome.toPath(), serviceDir, uploaded);
        // zoker-05（FND26）：集合完整性清单——全部文件CloseFile收口后作为最后一个文件补传
        //（清单存在=声明本次发布集合已完整），服务端commit据此校验齐全并清退残留
        //（DistributeManager.verifyDistributeManifest）。零文件不上传清单：空集合由服务端
        // 空目录/空清单拒绝。
        if (!uploaded.isEmpty())
            uploadDistributeManifest(zokerName, serviceName, uploaded, versionNo);
        var r = new CommitService();
        r.Argument.setServiceName(serviceName);
        r.Argument.setVersionNo(versionNo);
        // 应答路径含 pruneVersions 整树删除（服务包体量+磁盘速度无界）——默认 5s 会把成功部署
        // 当失败（stopService 同族），60s=部署级操作裕量。
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

    private void distributeService(String zokerName, Path localServiceHome, File serviceDir,
                                   List<String> uploaded) throws Exception {
        var files = serviceDir.listFiles();
        // listFiles()==null=目录列举失败（IO错误/权限拒绝/目录被并发删除），非空目录语义
        //（zoker-02）：静默 return 会使该子树整棵缺失于 uploaded 与集合清单，服务端清单校验
        // 只验"清单内齐全"，被裁剪的集合照常成版切 current——回执成功而现役版本缺文件。
        // 对齐下方读失败路径（FileInputStream 抛错→closeFile 压制+重抛）：列失败同样上抛，
        // 使整个 distributeService 失败、清单不提交。空数组仍走正常空目录语义。
        if (null == files)
            throw new IOException("listFiles fail: " + serviceDir);

        for (var file : files) {
            if (file.isDirectory()) {
                distributeService(zokerName, localServiceHome, file, uploaded);
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
                // 是预期伴生，压制为suppressed，不得顶替原始异常。
                try {
                    closeFile(zokerName, fileRelativeName, new Binary(md5.digest()));
                } catch (Exception suppressed) {
                    primary.addSuppressed(suppressed);
                }
                throw primary;
            }
            closeFile(zokerName, fileRelativeName, new Binary(md5.digest())); // 正常路径直线收尾，失败照抛
            uploaded.add(fileRelativeName);
        }
    }

    // 集合完整性清单上传（zoker-05，FND26）：内容=各文件相对distributeDir根的路径（与
    // OpenFile寻址同根），UTF-8每行一条，md5收口与普通文件同协议。断点续传对齐普通文件：
    // openFile返回的offset为断点、md5按（已存在前缀+新追加）计算；残留长于本次清单（集合
    // 缩小的重发布中断残留）时追加凑长必失配——直接以空摘要收口触发服务端清场
    //（eMd5Mismatch为预期应答），下一轮从0重传。两轮仍失败上抛（distribute整体失败）。
    // 清单双名上传：主形态=版本限定名（.zoker-manifest.<versionNo>，commit 按本次版本号
    // 对应消费，同名服务并发分发互不覆盖）；兼容副本=裸名（旧服务端只认裸名，缺失即走
    // legacy 无屏障——补传保持混合版本窗口内屏障不丢；新服务端优先版本限定名）。
    private void uploadDistributeManifest(String zokerName, String serviceName,
                                          List<String> fileRelativeNames, String versionNo) throws Exception {
        var content = (String.join("\n", fileRelativeNames) + "\n").getBytes(StandardCharsets.UTF_8);
        uploadFileWithResume(zokerName, serviceName + "/"
                + Zeze.Services.ZokerImpl.DistributeManager.distributeManifestName(versionNo), content);
        uploadFileWithResume(zokerName, serviceName + "/"
                + Zeze.Services.ZokerImpl.DistributeManager.DISTRIBUTE_MANIFEST_NAME, content);
    }

    /** 带断点续传/清场阶梯的整文件上传（清单两个名字共用同一阶梯语义）。 */
    private void uploadFileWithResume(String zokerName, String relativeName, byte[] content) throws Exception {
        for (var attempt = 0; ; ++attempt) {
            var md5 = MessageDigest.getInstance("MD5");
            var offset = openFile(zokerName, relativeName);
            if (offset > content.length) {
                try {
                    closeFile(zokerName, relativeName, new Binary(md5.digest()));
                } catch (Exception expected) {
                    // md5必然失配：服务端已清场（预期路径）
                }
                if (attempt > 0)
                    throw new RuntimeException("upload distribute manifest failed to clear overlong residue: " + relativeName);
                continue;
            }
            if (offset > 0)
                md5.update(content, 0, (int)offset);
            if (offset < content.length)
                appendFile(zokerName, relativeName, offset, content, (int)offset, content.length - (int)offset);
            try {
                closeFile(zokerName, relativeName, new Binary(md5.digest()));
                return;
            } catch (Exception e) {
                if (attempt > 0)
                    throw e; // 两轮仍失败：上抛，整个distribute失败由调用方重试
                // eMd5Mismatch等：服务端已清场或残留不匹配，下一轮从0重传
            }
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
        // 服务端 start 与 stop 共用 opsLocks 互斥：并发 stop 同服务时先排队停机窗口（最长
        // 10s 优雅+10s 强杀，ServiceManager），加之 launch 与 run.pid 落盘（fsync）——默认 5s
        // 会把排队后照常完成的启动当失败（stopService/openFile/commit 同族），60s=部署级操作裕量。
        r.SendForWait(zoker, 60_000).await();
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
        // Force-Killed/Alive-After-Force 两态不可达（迟到结果包被丢上下文）。60s=最坏路径3倍裕量。
        r.SendForWait(zoker, 60_000).await();
        if (r.getResultCode() != 0)
            throw new RuntimeException("stop service error. " + IModule.getErrorCode(r.getResultCode()));
        return r.Result;
    }

    /**
     * 本连接注册过的全部 zokerName（含换名淘汰的历史名）：Register 维护，断链时
     * {@link ZokerAgentService#OnSocketClose} 按集合逐一条件摘除 zokers 条目。集合为并发容器：
     * Register（派发线程）与 OnSocketClose（selector/tick/stop 线程）可能交错，弱一致迭代
     * 与并发 add 安全共存（交错漏摘的极小窗口由下次同名 Register 的 isClosed 接管兜底，有界）。
     */
    public static final class RegisteredNames {
        private final Set<String> names = ConcurrentHashMap.newKeySet();

        /** Register 装账成功后记账（重复名幂等）。 */
        public void add(String name) {
            names.add(name);
        }

        /** 遍历本连接注册过的全部名字（Register 摘旧与 OnSocketClose 收殓共用）。 */
        public void forEach(Consumer<String> action) {
            names.forEach(action);
        }

        /** OnSocketClose 收殓后清空（socket 即将不可达，名字串提前释放）。 */
        public void clear() {
            names.clear();
        }
    }
}
