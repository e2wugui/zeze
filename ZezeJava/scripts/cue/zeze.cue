package zeze

import (
	"domain.com/ds"
	dashboardBuilder "github.com/perses/perses/cue/dac-utils/dashboard"
	panelGroupsBuilder "github.com/perses/perses/cue/dac-utils/panelgroups"
	panelBuilder "github.com/perses/plugins/prometheus/sdk/cue/panel"
	promQuery "github.com/perses/plugins/prometheus/schemas/prometheus-time-series-query:model"
)

// 设置chart样式
#baseStatChart: {
	#format: _
	kind:    "StatChart"
	spec: {
		calculation: "last-number"
		format:      #format
		sparkline: {}
	}
}

#decimalStatChart: #baseStatChart & {
	#format: {
		unit:        "decimal"
		shortValues: true
	}
}

#bytesStatChart: #baseStatChart & {
	#format: {
		unit:        "bytes"
		shortValues: true
	}
}

#percentDecimalStatChart: #baseStatChart & {
	#format: {
		unit: "percent-decimal"
	}
}

#briefPanel: {
	#name:   string
	#plugin: _
	#query:  string

	panel: panelBuilder & {
		spec: {
			display: name: #name
			plugin: #plugin
			queries: [
				{
					kind: "TimeSeriesQuery"
					spec: plugin: promQuery & {
						spec: query:            #query
						spec: seriesNameFormat: "{{job}}-{{app}}"
					}
				},
			]
		}
	}
}

#decimalBriefPanel: #briefPanel & {
	#plugin: #decimalStatChart
}

// ===== jvm相关 =====
#cpuPanel: #decimalBriefPanel & {
	#name:  "cpu时间/s [5m]"
	#query: "rate(process_cpu_seconds_total[5m])"
}

#gcPanel: #decimalBriefPanel & {
	#name:  "gc时间/s [5m]"
	#query: "sum by(job, app) (rate(jvm_gc_collection_seconds_count[5m]))"
}

#memPanel: #briefPanel & {
	#name:   "jvm内存（heap+noheap）"
	#plugin: #bytesStatChart
	#query:  "sum by(job, app) (jvm_memory_used_bytes)"
}

#memPercentPanel: #briefPanel & {
	#name:   "jvm内存使用率（heap）"
	#plugin: #percentDecimalStatChart
	#query:  "jvm_memory_used_bytes{area=\"heap\"} / jvm_memory_max_bytes{area=\"heap\"}"
}

// ===== 协议 =====
#protoSendCountPanel: #decimalBriefPanel & {
	#name:  "发协议数/s [5m]"
	#query: "sum by(job, app)(rate(protocol_send_total[5m]))"
}

#protoRecvCountPanel: #decimalBriefPanel & {
	#name:  "收协议数/s [5m]"
	#query: "sum by(job, app)(rate(protocol_duration_seconds_count[5m]))"
}

#protoSendBytesPanel: #briefPanel & {
	#name:   "发协议bytes/s [5m]"
	#plugin: #bytesStatChart
	#query:  "sum by(job, app)(rate(protocol_send_bytes_total[5m]))"
}

#protoRecvBytesPanel: #briefPanel & {
	#name:   "收协议bytes/s [5m]"
	#plugin: #bytesStatChart
	#query:  "sum by(job, app)(rate(protocol_recv_bytes_total[5m]))"
}

// ===== 事务 =====
#tpsPanel: #decimalBriefPanel & {
	#name:  "事务/s [5m]"
	#query: "sum by(job, app)(rate(procedurecompletedtotal[5m]))"
}

#transactionErrorPanel: #decimalBriefPanel & {
	#name:  "1h事务出错数"
	#query: "sum by(job, app)(increase(procedurecompletedtotal{result_code!=\"0\"}[1h]))"
}

#taskRatePanel: #decimalBriefPanel & {
	#name: "task/s [5m]"

	#query: "sum by(job, app)(rate(taskdurationseconds_count[5m]))"
}

#currentTransactionCountPanel: #decimalBriefPanel & {
	#name:  "当前正在处理事务数"
	#query: "sum by(job, app)(procedure_started_total) - sum by(job, app)(procedure_completed_total)"
}

#transactionRedoCountPanel: #decimalBriefPanel & {
	#name:  "1h事务redo数"
	#query: "sum by(job, app)(increase(procedure_redo_total[1h]))"
}

#transactionRedoAndReleaseLockCountPanel: #decimalBriefPanel & {
	#name:  "1h事务redoAndReleaseLock数"
	#query: "sum by(job, app)(increase(procedure_redo_and_release_lock_total[1h]))"
}

#transactionManyLocksCountPanel: #decimalBriefPanel & {
	#name:  "1h事务(>50lock)数"
	#query: "sum by(job, app)(increase(procedure_many_locks_count[1h]))"
}

// ===== 场景 =====
#instanceCountPanel: #decimalBriefPanel & {
	#name:  "副本数"
	#query: "scene_started_total{scene_type=\"instance\"} - scene_destroyed_total{scene_type=\"instance\"}"
}

#roleCountPanel: #decimalBriefPanel & {
	#name:  "在线人数"
	#query: "fighter_started_total{fighter_type=\"role\"} - fighter_offline_total{fighter_type=\"role\"}"
}

#npcCountPanel: #decimalBriefPanel & {
	#name:  "npc数"
	#query: "fighter_started_total{fighter_type=\"monster\"} - fighter_offline_total{fighter_type=\"monster\"}"
}

#projectileCountPanel: #decimalBriefPanel & {
	#name:  "projectile数"
	#query: "fighter_started_total{fighter_type=\"projectile\"} - fighter_offline_total{fighter_type=\"projectile\"}"
}

#staticSceneCountPanel: #decimalBriefPanel & {
	#name:  "static场景数"
	#query: "scene_started_total{scene_type=\"static\"} - scene_destroyed_total{scene_type=\"static\"}"
}

// ===== dashboard =====
dashboardBuilder & {
	#name:    "brief"
	#project: "zeze"
	#panelGroups: panelGroupsBuilder & {
		#input: [
			{
				#title: "jvm"
				#cols:  2
				#panels: [
					#cpuPanel.panel,
					#gcPanel.panel,
					#memPanel.panel,
					#memPercentPanel.panel,
				]
			},
			{
				#title: "协议"
				#cols:  2
				#panels: [
					#protoSendCountPanel.panel,
					#protoRecvCountPanel.panel,
					#protoSendBytesPanel.panel,
					#protoRecvBytesPanel.panel,
				]
			},
			{
				#title: "事务"
				#cols:  3
				#panels: [
					#currentTransactionCountPanel.panel,
					#tpsPanel.panel,
					#transactionErrorPanel.panel,
					#transactionRedoCountPanel.panel,
					#transactionRedoAndReleaseLockCountPanel.panel,
					#transactionManyLocksCountPanel.panel,
					#taskRatePanel.panel,
				]
			},
			{
				#title: "场景"
				#cols:  2
				#panels: [
					#instanceCountPanel.panel,
					#roleCountPanel.panel,
					#npcCountPanel.panel,
					#projectileCountPanel.panel,
					#staticSceneCountPanel.panel,
				]
			},

		]
	}

	#datasources:     ds.#localDatasources
	#duration:        "3h"
	#refreshInterval: "30s"
}
