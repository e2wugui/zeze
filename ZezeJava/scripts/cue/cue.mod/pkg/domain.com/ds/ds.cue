package ds

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
