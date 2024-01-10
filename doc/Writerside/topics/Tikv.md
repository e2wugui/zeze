# Tikv

## Windows安装虚拟机及CentOS系统

1. 下载 CentOS7 光盘镜像: https://mirrors.aliyun.com/centos/ 选择最新的7.x版本, 进入isos/x86_64子目录, 下载 CentOS-7-x86_64-Minimal- 开头的iso镜像文件
2. 下载安装 Virtual Box: https://www.virtualbox.org/wiki/Downloads
3. 启动 Virtual Box, 新建虚拟机, 内存至少4G, 网络配置成"网络地址转换(NAT)"和"仅主机(Host-Only)网络"双网卡, 前者用于访问互联网, 后者用于跟主机互连
4. 存储设置里, IDE控制器里增加 CentOS7 光盘镜像, SATA控制器里增加新硬盘, 至少8G, 推荐16G以上
5. 启动虚拟机, 安装 CentOS7, 需要注意的是6G以下内存需要创建swap分区作为内存的补充, 至少分256M的boot分区, 其它空间都分配给根分区, 别忘开启网络, 安装过程中设置root密码并增加一个普通权限的账号
6. 安装完成并重启后光盘会自动卸载, 进入系统后登录root账号, 查看虚拟机的IP地址(ip address), 其中"10.0."开头的用于访问互联网的NAT地址, "192.168."开头的用于跟主机连接的IP地址, 然后退出登录(exit)
7. 主机启动SSH客户端, 连接"192.168."开头的IP地址, 22端口, 使用普通权限的账号和密码登录

## Install TiKV

1. 下载 TiDB-community-server 的Linux端软件包: https://pingcap.com/zh/product-community
2. 以下均在SSH客户端已登录虚拟机的环境, 先进入root账号(su)
3. 可先更新所有已安装的软件(yum upgrade), 然后安装Zmodem工具(yum install lrzsz)和其它需要的工具(如net-tools等)
4. 用vim修改"/etc/security/limits.conf", 增加一行"* hard nofile 100000"
5. 关闭防火墙(systemctl disable firewalld.service)
6. 退出root账号, 用vim修改"~/.bash_profile", 增加一行"ulimit -n 100000"
7. 重启虚拟机后, SSH客户端重新登录, 在home目录中创建tikv目录, 进入tikv目录后用rz命令上传主机下载好的tikv压缩包中的"pd-server"和"tikv-server"两个可执行程序文件
8. 给上传的2个程序增加可执行权限(chmod +x *)

## Start TiKV
1. 进入tikv目录(含有pd-server和tikv-server的目录)
2. 执行如下命令启动TiKV(其中的"192.168.56.101"要换成自己虚拟机用于跟主机连接的IP地址, 注意首次执行会占用4G左右的硬盘空间):
```
nohup ./pd-server --name=pd1 --data-dir=pd1 --client-urls="http://192.168.56.101:2379" --peer-urls="http://192.168.56.101:2380" --initial-cluster="pd1=http://192.168.56.101:2380" --log-file=pd1.log 1> pd1_out.log 2> pd1_err.log &
nohup ./tikv-server --pd-endpoints="192.168.56.101:2379" --addr="192.168.56.101:20160" --data-dir=tikv1 --log-file=tikv1.log 1> tikv1_out.log 2> tikv1_err.log &
nohup ./tikv-server --pd-endpoints="192.168.56.101:2379" --addr="192.168.56.101:20161" --data-dir=tikv2 --log-file=tikv2.log 1> tikv2_out.log 2> tikv2_err.log &
nohup ./tikv-server --pd-endpoints="192.168.56.101:2379" --addr="192.168.56.101:20162" --data-dir=tikv3 --log-file=tikv3.log 1> tikv3_out.log 2> tikv3_err.log &
```
3. 停止TiKV时用kill命令停止以上4个进程即可
4. 以上的TiKV仅在单机启动服务用于测试, 如需分布式部署, 应参考官方文档(https://tikv.org/docs/6.1/deploy/install/production/)使用TiUP管理工具更加方便可靠

### Test TiKV
1. Java客户端的测试代码:
```
// 依赖 'org.tikv:tikv-client-java:3.3.0' 和 'org.slf4j:slf4j-api:1.7.36'
TiSession session = TiSession.create(TiConfiguration.createRawDefault("192.168.56.101:2379")); // 改成自己虚拟机用于跟主机连接的IP地址
RawKVClient client = session.createRawClient();
client.put(ByteString.copyFromUtf8("key"), ByteString.copyFromUtf8("Hello, World!"));
var v = client.get(ByteString.copyFromUtf8("key"));
System.out.println(v.isPresent() ? v.get().toStringUtf8() : null); // 输出"Hello, World!"即表示安装成功
```
2. 3个tikv-server组成了高可用性的一组raft节点, 可以尝试停止其中1个tikv-server, 测试依然成功
3. 如果停止2个或3个tikv-server, 则测试无法成功, 客户端从pd-server得到错误信息"peer is not leader for region 2, leader may None"
4. pd-server也可以分布式, 客户端连接某个pd-server时会得到其leader的地址端口, 如果不是当前连接的pd-server, 则再连接leader的pd-server才能继续访问服务
