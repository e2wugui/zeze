## What Is Zeze?

Zeze是一个基于一致性缓存的分布式事务应用框架，它具有很多独具的特性。
* 异常导致内存数据修改不一致的问题怎么解决？zeze使用事务解决了这个问题。
* 可以不懂多线程编写安全多线程程序。zeze是完全多线程的，线程之间使用锁访问共享的数据，
你的多线程程序访问通过zeze定义的数据自动就是安全的，完全不需要多线程知识就能写逻辑。
当然写某些需要自己管理数据的多线程高级功能还是需要一定的多线程知识。
* zeze使用乐观锁算法，不会死锁。
* 支持多种后端数据库映射，向应用提供KV形式的数据库接口。对于mysql也支持二维表映射。
内存数据自动和后端数据库同步，完全不需要用户关心。
* 除了定义数据，使用数据实现逻辑，用户几乎不需要考虑任何其他问题就能实现服务器功能。应用开发高效简单。

更多文档详见 https://gitee.com/e2wugui/zeze/blob/master/doc/index.md

## Maven

```xml
<!-- https://mvnrepository.com/artifact/com.zezeno/zeze-java -->
<dependency>
    <groupId>com.zezeno</groupId>
    <artifactId>zeze-java</artifactId>
    <version>1.5.10</version>
</dependency>
```
