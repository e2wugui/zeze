<!--
Module 包含 Table 默认私有，可以通过增加方法返回并暴露出去
Module 使用 partical 把生成部分放到gen下
Module 生成 IModule 和 Module，这样IModule接口方法发生增删，方法参数变更等，代码编译可以报错。

协议里面的数值默认大于等于0，支持负数需要明确声明。
Config 独立
ByteBuffer.SkipUnknownField 移到其他地方
XXX 统一 bean 和 xbean 定义的话，怎么区分生成：被 table 引用的 bean 自动生成一份到某个特别的目录
统一生成工具，应用框架和数据存储框架定义分开？两者的数据bean的定义应该是不一样的，重用的可能性很小。
	主要入口 application 数据库定义 database. database.module 必须在 application 中存在。

去掉 xbean.Const xbean.Data
去掉 select。拷贝数据直接由 bean duplicate 支持
managed
数据变更?
Net.Manager 怎么重新定义？现在这个不够灵活。

-->
ConcurrentDictionary
DataInDB DataInTrans                                                                 实际操作
   null           null                 事务中删除，数据库中没有，  cache.remove      delete
  !null           null                 事务中删除，数据库中有，    db.delete
   null           !null                事务中put,数据库中没有，    db.replace        replace
  !null           !null                事务中put,数据库中有，      db.replace

关于 cache，只要访问 table 的记录，在 cache 中不存在时总是新增一个 record。
包括删除不存在的记录（这点需要琢磨一下，也就是说删除也加入 lockandchecknoconflit）。

读写锁
Record.Commit
