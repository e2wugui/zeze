### 准备
1. 到sonatype网站 https://central.sonatype.com/ 注册账号(推荐使用github账号认证), 为发布包的域名做认证, 获取上传token
   (域名认证是给域名的TXT属性设置为认证指定的一串随机码,设置后可用"nslookup -type=TXT 域名"查询)
2. 修改ZezeJava\ZezeJava\pom.xml里的域名: <groupId>域名</groupId>
3. %USERPROFILE%\.m2\settings.xml里配置上传token:
  <servers>
    <server>
      <id>central</id>
      <username>......</username>
      <password>......</password>
    </server>
  </servers>
4. 到 https://www.gpg4win.org/ 下载安装gpg, 生成并上传自己的证书

### 发布
1. 在ZezeJava\ZezeJava\里运行: mvn deploy
2. 到sonatype网站登录账号, 找到刚上传的deployment, 没问题的话点publish就公开发布了

### 参考
https://juejin.cn/post/7364764922339639330
