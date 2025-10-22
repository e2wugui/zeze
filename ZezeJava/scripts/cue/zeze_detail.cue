package zeze

import (
	"domain.com/ds"
	dashboardBuilder "github.com/perses/perses/cue/dac-utils/dashboard"
	panelGroupsBuilder "github.com/perses/perses/cue/dac-utils/panelgroups"
	panelBuilder "github.com/perses/plugins/prometheus/sdk/cue/panel"
	varGroupBuilder "github.com/perses/perses/cue/dac-utils/variable/group"
	// promQLVarBuilder "github.com/perses/plugins/prometheus/sdk/cue/variable/promql"
	labelValuesVarBuilder "github.com/perses/plugins/prometheus/sdk/cue/variable/labelvalues"
	promFilterBuilder "github.com/perses/plugins/prometheus/sdk/cue/filter"
	// commonProxy "github.com/perses/perses/cue/common/proxy"
	// promDs "github.com/perses/plugins/prometheus/schemas/datasource:model"
	promQuery "github.com/perses/plugins/prometheus/schemas/prometheus-time-series-query:model"
)


// ===== variables =====
#myVarsBuilder: varGroupBuilder & {
	#input: [
		labelValuesVarBuilder & {
			#name:           "job"
			#metric:         "up"
			#datasourceName: "local"
		},
		labelValuesVarBuilder & {
			#name:           "app"
			#metric:         "up"
			#datasourceName: "local"
		},
	]
}

#filter: {promFilterBuilder & #myVarsBuilder}.filter

// ===== time series =====
#timeSeriesPanel: {
	#name:             string
	#query:            string
	#seriesNameFormat: string

	panel: panelBuilder & {
		spec: {
			display: name: #name
			plugin: {
				kind: "TimeSeriesChart"
				spec: {}
			}
			queries: [
				{
					kind: "TimeSeriesQuery"
					spec: plugin: promQuery & {
						spec: query:            #query
						spec: seriesNameFormat: #seriesNameFormat
					}
				},
			]
		}
	}
}

// ===== 详细协议监控 =====
#protocolSendRateDetailPanel: #timeSeriesPanel & {
	#name:             "每秒发协议数量 [5m] $job-$app"
	#query:            "topk(10, rate(protocol_send_total{ \(#filter) }[5m]))"
	#seriesNameFormat: "{{protocol}}"
}

#protocolRecvRateDetailPanel: #timeSeriesPanel & {
	#name:             "每秒收协议数量 [5m] $job-$app"
	#query:            "topk(10, rate(protocol_duration_seconds_count{ \(#filter) }[5m]))"
	#seriesNameFormat: "{{protocol}}"
}

// ===== 详细数据库监控 =====
#dbWriteLockRateDetailPanel: #timeSeriesPanel & {
	#name:             "每秒写锁数 [5m] $job-$app"
	#query:            "topk(10, rate(database_table_operation_total{operation=\"writeLock\", \(#filter) }[5m]))"
	#seriesNameFormat: "{{table}}"
}

// ===== 详细事务监控 =====
#procedureRateDetailPanel: #timeSeriesPanel & {
	#name:             "每秒事务数 [5m] $job-$app"
	#query:            "rate(procedure_completed_total{ \(#filter) }[5m])"
	#seriesNameFormat: "{{procedure}}"
}

#procedureDurationP95DetailPanel: #timeSeriesPanel & {
	#name:             "事务完成时间p95 [5m] $job-$app"
	#query:            "histogram_quantile(0.95, rate(procedure_duration_seconds_bucket{ \(#filter) }[5m]))"
	#seriesNameFormat: "{{procedure}}"
}

#procedureDurationP99DetailPanel: #timeSeriesPanel & {
	#name:             "事务完成时间p99 [5m] $job-$app"
	#query:            "histogram_quantile(0.99, rate(procedure_duration_seconds_bucket{ \(#filter) }[5m]))"
	#seriesNameFormat: "{{procedure}}"
}

#procedureErrorRateDetailPanel: #timeSeriesPanel & {
	#name:             "每5m事务出错数 $job-$app"
	#query:            "sum by (procedure, result_code) (increase(procedure_completed_total{result_code!=\"0\", \(#filter) }[5m]))"
	#seriesNameFormat: "{{procedure}} - {{result_code}}"
}

#procedureRedoRateDetailPanel: #timeSeriesPanel & {
	#name:             "每5m事务redo数 $job-$app"
	#query:            "increase(procedure_redo_total{ \(#filter) }[5m])"
	#seriesNameFormat: "{{procedure}}"
}

#procedureRedoAndReleaseLockRateDetailPanel: #timeSeriesPanel & {
	#name:             "每5m事务redoAndReleaseLock数 $job-$app"
	#query:            "increase(procedure_redo_and_release_lock_total{ \(#filter) }[5m])"
	#seriesNameFormat: "{{procedure}}"
}

#procedureManyLocksRateDetailPanel: #timeSeriesPanel & {
	#name:             "每5m事务(>50lock)数 $job-$app"
	#query:            "increase(procedure_many_locks_count{ \(#filter) }[5m])"
	#seriesNameFormat: "{{procedure}}"
}

// ===== 详细任务监控 =====
#taskRateDetailPanel: #timeSeriesPanel & {
	#name:             "每秒task数 [5m] $job-$app"
	#query:            "rate(task_duration_seconds_count{ \(#filter) }[5m])"
	#seriesNameFormat: "{{task}}"
}

#taskDurationP95DetailPanel: #timeSeriesPanel & {
	#name:             "task完成时间p95 [5m] $job-$app"
	#query:            "histogram_quantile(0.95, rate(task_duration_seconds_bucket{ \(#filter) }[5m]))"
	#seriesNameFormat: "{{task}}"
}

#taskDurationP99DetailPanel: #timeSeriesPanel & {
	#name:             "task完成时间p99 [5m] $job-$app"
	#query:            "histogram_quantile(0.99, rate(task_duration_seconds_bucket{ \(#filter) }[5m]))"
	#seriesNameFormat: "{{task}}"
}

// ===== 详细服务监控 =====
#serviceSendRateDetailPanel: #timeSeriesPanel & {
	#name:             "每秒send数 [5m] $job-$app"
	#query:            "topk(10, rate(service_send_total{ \(#filter) }[5m]))"
	#seriesNameFormat: "{{service}}"
}

#serviceSendBytesRateDetailPanel: #timeSeriesPanel & {
	#name:             "每秒send bytes [5m] $job-$app"
	#query:            "topk(10, rate(service_send_bytes_total{ \(#filter) }[5m]))"
	#seriesNameFormat: "{{service}}"
}

#serviceRecvRateDetailPanel: #timeSeriesPanel & {
	#name:             "每秒recv数 [5m] $job-$app"
	#query:            "topk(10, rate(service_recv_total{ \(#filter) }[5m]))"
	#seriesNameFormat: "{{service}}"
}

#serviceRecvBytesRateDetailPanel: #timeSeriesPanel & {
	#name:             "每秒recv bytes [5m] $job-$app"
	#query:            "topk(10, rate(service_recv_bytes_total{ \(#filter) }[5m]))"
	#seriesNameFormat: "{{service}}"
}

// ===== 详细场景监控 =====
#sceneInstanceCreationRateDetailPanel: #timeSeriesPanel & {
	#name:             "5m副本新开启 $job-$app"
	#query:            "increase(scene_started_total{scene_type=\"instance\", \(#filter) }[5m])"
	#seriesNameFormat: "{{scene}}"
}

#sceneRoleCreationRateDetailPanel: #timeSeriesPanel & {
	#name:             "5m角色新创建 $job-$app"
	#query:            "increase(fighter_started_total{fighter_type=\"role\", \(#filter) }[5m])"
	#seriesNameFormat: "{{fighter}}"
}

#sceneMonsterCreationRateDetailPanel: #timeSeriesPanel & {
	#name:             "5m npc新创建 $job-$app"
	#query:            "increase(fighter_started_total{fighter_type=\"monster\", \(#filter) }[5m])"
	#seriesNameFormat: "{{fighter}}"
}

#sceneProjectileCreationRateDetailPanel: #timeSeriesPanel & {
	#name:             "5m projectile新创建 $job-$app"
	#query:            "increase(fighter_started_total{fighter_type=\"projectile\", \(#filter) }[5m])"
	#seriesNameFormat: "{{fighter}}"
}

#sceneStaticCreationRateDetailPanel: #timeSeriesPanel & {
	#name:             "5m 静态场景新创建 $job-$app"
	#query:            "increase(scene_started_total{scene_type=\"static\", \(#filter) }[5m])"
	#seriesNameFormat: "{{scene}}"
}

// ===== dashboard =====
dashboardBuilder & {
	#name:      "detail"
	#project:   "zeze"
	#variables: #myVarsBuilder.variables
	#panelGroups: panelGroupsBuilder & {
		#input: [
			{
				#title: "详细协议监控"
				#cols:  2
				#panels: [
					#protocolSendRateDetailPanel.panel,
					#protocolRecvRateDetailPanel.panel,
				]
			},
			{
				#title: "详细数据库监控"
				#cols:  1
				#panels: [
					#dbWriteLockRateDetailPanel.panel,
				]
			},
			{
				#title: "详细事务监控"
				#cols:  2
				#panels: [
					#procedureRateDetailPanel.panel,
					#procedureRedoRateDetailPanel.panel,
					#procedureRedoAndReleaseLockRateDetailPanel.panel,
					#procedureManyLocksRateDetailPanel.panel,
					#procedureDurationP95DetailPanel.panel,
					#procedureDurationP99DetailPanel.panel,
					#procedureErrorRateDetailPanel.panel,
				]
			},
			{
				#title: "详细任务监控"
				#cols:  3
				#panels: [
					#taskRateDetailPanel.panel,
					#taskDurationP95DetailPanel.panel,
					#taskDurationP99DetailPanel.panel,
				]
			},
			{
				#title: "详细服务监控"
				#cols:  2
				#panels: [
					#serviceSendRateDetailPanel.panel,
					#serviceSendBytesRateDetailPanel.panel,
					#serviceRecvRateDetailPanel.panel,
					#serviceRecvBytesRateDetailPanel.panel,
				]
			},
			{
				#title: "详细场景监控"
				#cols:  3
				#panels: [
					#sceneInstanceCreationRateDetailPanel.panel,
					#sceneRoleCreationRateDetailPanel.panel,
					#sceneMonsterCreationRateDetailPanel.panel,
					#sceneProjectileCreationRateDetailPanel.panel,
					#sceneStaticCreationRateDetailPanel.panel,
				]
			},

		]
	}

	#datasources:     ds.#localDatasources
	#duration:        "3h"
	#refreshInterval: "30s"
}
