# Limit

## Database
* Zeze.Transaction.Database.eMaxKeyLength

Table.Key的最大长度。默认为3072，这个数字来源自Mysql 8的varbinary为
primary key时的最大长度。现在这个配置用于Sqlserver，Mysql以及兼容Mysql
的数据库。修改这个参数需要重新编译Zeze。已经创建的表的Key类型需要自己
手动修改(Alter)。
