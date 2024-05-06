# 自定义配置

配置由Zeze.Config类管理。

## 简单装载
使用下面两个静态方法装载配置
```
Zeze.Config.Load(String filename);
Zeze.Config.Load() 使用fileName=“zeze.xml”装载配置。
```

## 自定义配置
1.	定义自己的配置类
```
    public class MyConfig implements Zeze.Config.ICustomize
    实现方法 getName，Parse。
    其中getName的返回值就是以后配置文件(zeze.xml)中CustomizeConf的Name。如
    下：
    <CustomizeConf Name=”MyConfig”>
    …
    </ CustomizeConf>
```

2.	装载代码例子
```
Var config = Zeze.Config.load(myConfig);
Var myConfig = new MyConfig();
config.parseCustomize(myConfig);
```
