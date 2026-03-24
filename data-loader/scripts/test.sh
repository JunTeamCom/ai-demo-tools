curl http://localhost:6333/collections
# {"result":{"collections":[{"name":"ai_demo"},{"name":"star_charts"}]},"status":"ok","time":5.5e-6}
curl -X POST http://localhost:6333/collections/ai_demo/points/count \
  -H "Content-Type: application/json" \
  -d '{"exact": true}'
# {"result":{"count":2},"status":"ok","time":0.0004876}
