package zeze

import (
	dashboardBuilder "github.com/perses/perses/cue/dac-utils/dashboard"
	panelGroupsBuilder "github.com/perses/perses/cue/dac-utils/panelgroups"
	panelBuilder "github.com/perses/plugins/prometheus/sdk/cue/panel"
	// commonProxy "github.com/perses/perses/cue/common/proxy"
	// promDs "github.com/perses/plugins/prometheus/schemas/datasource:model"
	promQuery "github.com/perses/plugins/prometheus/schemas/prometheus-time-series-query:model"
)

#localDatasources: {
	local: {
		default: true
		plugin: {
			kind: "PrometheusDatasource"
			spec: {
				proxy: {
					kind: "HTTPProxy"
					spec: {
						url: "http://localhost:9090"
						allowedEndpoints: [
							{
								endpointPattern: "/api/v1/labels"
								method:          "POST"
							},
							{
								endpointPattern: "/api/v1/series"
								method:          "POST"
							},
							{
								endpointPattern: "/api/v1/metadata"
								method:          "GET"
							},
							{
								endpointPattern: "/api/v1/query"
								method:          "POST"
							},
							{
								endpointPattern: "/api/v1/query_range"
								method:          "POST"
							},
							{
								endpointPattern: "/api/v1/label/([a-zA-Z0-9_-]+)/values"
								method:          "GET"
							},
						]
					}
				}
				scrapeInterval: "15s"
			}
		}
	}
}

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

// ===== jvm相关 =====
#cpuPanel: #briefPanel & {
	#name:   "cpu时间/s [5m]"
	#plugin: #decimalStatChart
	#query:  "rate(process_cpu_seconds_total[5m])"
}

#gcPanel: #briefPanel & {
	#name:   "gc时间/s [5m]"
	#plugin: #decimalStatChart
	#query:  "sum by(job, app) (rate(jvm_gc_collection_seconds_count[5m]))"
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
#protoSendCountPanel: #briefPanel & {
	#name:   "发协议数/s [5m]"
	#plugin: #decimalStatChart
	#query:  "sum by(job, app)(rate(protocol_send_total[5m]))"
}

#protoRecvCountPanel: #briefPanel & {
	#name:   "收协议数/s [5m]"
	#plugin: #decimalStatChart
	#query:  "sum by(job, app)(rate(protocol_duration_seconds_count[5m]))"
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
#tpsPanel: #briefPanel & {
	#name:   "事务/s [5m]"
	#plugin: #decimalStatChart
	#query:  "sum by(job, app)(rate(procedurecompletedtotal[5m]))"
}

#transactionErrorPanel: #briefPanel & {
	#name:   "1h事务出错数"
	#plugin: #decimalStatChart
	#query:  "sum by(job, app)(increase(procedurecompletedtotal{result_code!=\"0\"}[1h]))"
}

#taskRatePanel: #briefPanel & {
	#name:   "task/s [5m]"
	#plugin: #decimalStatChart
	#query:  "sum by(job, app)(rate(taskdurationseconds_count[5m]))"
}

// ===== dashboard =====
dashboardBuilder & {
	#name:    "zeze"
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
					#tpsPanel.panel,
					#transactionErrorPanel.panel,
					#taskRatePanel.panel,
				]
			},

		]
	}

	#datasources:     #localDatasources
	#duration:        "3h"
	#refreshInterval: "30s"
}
