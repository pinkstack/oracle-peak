# Grafana

kubectl -n monitoring port-forward svc/cookie-grafana 8080:3000

6ivc94hkWG

<Service Aame>.<Namespace Name>


<Service Aame>.<Namespace Name>.svc.cluster.local


http://snow-influxdb.tick.svc.cluster.local:8086

kubectl get secret cookie-grafana-admin --namespace monitoring -o jsonpath="{.data.GF_SECURITY_ADMIN_PASSWORD}" | base64 --decode