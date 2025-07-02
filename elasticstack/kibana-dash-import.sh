#!/bin/sh

#curl -X POST "http://localhost:5601/api/data_views/data_view" \
#-H "kbn-xsrf: string" \
#-H "Content-Type: application/json" \
#-d '{
#    "data_view": {
#        "name": "Morality Data View",
#        "title": "morality_*",
#        "timeFieldName": "timestamp"
#    }
#}'

curl -X POST "http://localhost:5601/api/saved_objects/_import?overwrite=true" \
-H "kbn-xsrf: true" \
--form file=@./kibana-dashboard.ndjson

