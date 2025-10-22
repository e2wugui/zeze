# 监控

## 启动监控

使用-DZezeCounter=Zeze.Util.PrometheusCounter 参数启动gs。http://localhost:8080/  下点击metrics可以看到所有的指标

下载 https://prometheus.io/ ，然后在prometheus.yml 的scrape_configs:下加入  

```
- job_name: "taiwan"
   static_configs:
    - targets: ["localhost:8080"]
      labels:
        app: "server"
```

启动prometheus.exe，然后浏览 http://localhost:9090/

- job_name 代表服务器组名称
- app 代表再次服务器组下具体的进程 （link，server, servicemgr,  globalserver）



下载https://perses.dev/

启动perses.exe，然后浏览 http://localhost:8080/


- prometheus用于采集metrics数据，存储
- perses用于dashboard展示



### 部署

```
percli 登录，方便之后percli apply
percli login http://localhost:8080 

如果prometheus服务器不是 http://localhost:9090 或有多个prometheus服务器，
更改cue\cue.mod\pkg\domain.com\ds\ds.cue

---
之后修改具体的dashboard，只要做以下操作就好：

更改zeze.cue
percli dac build -f zeze.cue -ojson
percli apply --force -f built/zeze_output.json

更改zeze_detail.cue
percli dac build -f zeze_detail.cue -ojson
percli apply --force -f built/zeze_detail_output.json
```

以下监控说明是规划，具体的配置已经写在了zeze.cue，zeze_detail.cue里。
percli apply 就是based on cue，将cue生成的dashboard配置应用到perses中。

## 总体监控

使用 stat chart来展示

"kind": "StatChart",

"seriesNameFormat": "{{job}}-{{app}}"


### jvm

- cpu时间（1秒内用多少）[5m]

    rate(process_cpu_seconds_total[5m])

- gc时间（1秒内用多少）[5m]

    sum by(job, app) (rate(jvm_gc_collection_seconds_count[5m]))

- jvm内存（heap+noheap）

    sum by(job, app) (jvm_memory_used_bytes)

    "format": { "unit": "bytes" },

- jvm内存使用率（heap）

    jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} 

    "format": { "unit": "percent-decimal" }



### 协议

- 每秒发协议数 [5m]

  sum by(job, app)(rate(protocol_send_total[5m]))


- 每秒收协议数 [5m]

  sum by(job, app)(rate(protocol_duration_seconds_count[5m]))


- 每秒发协议bytes [5m]

  sum by(job, app)(rate(protocol_send_bytes_total[5m]))

  "format": { "unit": "bytes" }

- 每秒收协议bytes [5m]

  sum by(job, app)(rate(protocol_recv_bytes_total[5m]))

  "format": { "unit": "bytes" }


### 事务

- 每秒处理事务 [5m]

  sum by(job, app)(rate(procedure_completed_total[5m]))

- 1h事务出错数

  sum by(job, app)(increase(procedure_completed_total{result_code!="0"}[1h]))

- 每秒task数 [5m]

  sum by(job, app)(rate(task_duration_seconds_count[5m]))
  

## 详细监控

variables: job,app

filter 也以这两个为准

- "kind": "TimeSeriesChart"

### 协议

"seriesNameFormat": "{{protocol}}"

- 每秒发协议数量 [5m]：

  topk(10, rate(protocol_send_total[5m]))

- 每秒收协议数量 [5m]：

  topk(10, rate(protocol_duration_seconds_count[5m]))


- 协议完成时间p95 [5m]

  ```
  histogram_quantile(0.95, rate(protocol_duration_seconds_bucket[5m]))
  ```

- 事务完成时间p99 [5m]

  ```
  histogram_quantile(0.99, rate(protocol_duration_seconds_bucket[5m]))
  ```


### db

"seriesNameFormat": "{{table}}"

- 每秒写锁数 db.writeLock [5m]

  ```
  topk(10, rate(database_table_operation_total{operation="writeLock"}[5m]))
  ```

- mysql的db相关没有监控，todo


### 事务

"seriesNameFormat": "{{procedure}}"

- 每秒事务数 [5m]

  ```
  rate(procedure_completed_total[5m])
  ```


- 事务完成时间p95 [5m]

  ```
  histogram_quantile(0.95, rate(procedure_duration_seconds_bucket[5m]))
  ```

- 事务完成时间p99 [5m]

  ```
  histogram_quantile(0.99, rate(procedure_duration_seconds_bucket[5m]))
  ```


- 每5m事务出错数

  ```
  sum by (procedure, result_code) (
    increase(procedure_completed_total{result_code!="0"}[5m])
  )
  ```

  "seriesNameFormat": "{{procedure}} - {{result_code}}"


### task

"seriesNameFormat": "{{task}}"

- 每秒task数 [5m]

  ```
  rate(task_duration_seconds_count[5m])
  ```


- task完成时间p95 [5m]

  ```
  histogram_quantile(0.95, rate(task_duration_seconds_bucket[5m]))
  ```

- task完成时间p99 [5m]

  ```
  histogram_quantile(0.99, rate(task_duration_seconds_bucket[5m]))
  ```


### service

"seriesNameFormat": "{{service}}"


- 每秒send数 [5m]：

  topk(10, rate(service_send_total[5m]))

- 每秒send bytes [5m]：

  topk(10, rate(service_send_bytes_total[5m]))

- 每秒recv数 [5m]：

  topk(10, rate(service_recv_total[5m]))


- 每秒recv bytes [5m]：

  topk(10, rate(service_recv_bytes_total[5m]))


## scene （不是zeze自带的）

### 总体监控

使用 stat chart来展示

"kind": "StatChart"

"seriesNameFormat": "{{job}}-{{app}}"

- 副本数

  ```
  scene_started_total{scene_type="instance"} -scene_destroyed_total{scene_type="instance"}
  ```

- 在线人数

  ```
  fighter_started_total{fighter_type="role"} -fighter_offline_total{fighter_type="role"}
  ```

- npc数

  ```
  fighter_started_total{fighter_type="monster"} -fighter_offline_total{fighter_type="monster"}
  ```

- projectile数

  ```
  fighter_started_total{fighter_type="projectile"} -fighter_offline_total{fighter_type="projectile"}
  ```

- static场景数

  ```
  scene_started_total{scene_type="static"} -scene_destroyed_total{scene_type="static"}
  ```

### 详细监控

"kind": "TimeSeriesChart"

- 5m副本新开启

  ```
  increase(scene_started_total{scene_type="instance"}[5m])
  ```

- 5m角色新创建

  ```
  increase(fighter_started_total{fighter_type="role"}[5m])
  ```

- 5m npc新创建

  ```
  increase(fighter_started_total{fighter_type="monster"}[5m])
  ```


- 5m projectile新创建

  ```
  increase(fighter_started_total{fighter_type="projectile"}[5m])
  ```

- 5m 静态场景新创建

  ```
  increase(scene_started_total{scene_type="static"}[5m])
  ```