# Google Cloud Platform Setup

### GKE


Create cluster on GKE

```bash
gcloud beta container --project "oracle-peak" clusters create "peak" --zone "europe-west3-a" --no-enable-basic-auth --cluster-version "1.17.13-gke.2001" --release-channel "regular" --machine-type "e2-medium" --image-type "COS" --disk-type "pd-standard" --disk-size "100" --metadata disable-legacy-endpoints=true --scopes "https://www.googleapis.com/auth/devstorage.read_only","https://www.googleapis.com/auth/logging.write","https://www.googleapis.com/auth/monitoring","https://www.googleapis.com/auth/servicecontrol","https://www.googleapis.com/auth/service.management.readonly","https://www.googleapis.com/auth/trace.append" --num-nodes "3" --enable-stackdriver-kubernetes --enable-ip-alias --network "projects/oracle-peak/global/networks/default" --subnetwork "projects/oracle-peak/regions/europe-west3/subnetworks/default" --default-max-pods-per-node "110" --no-enable-master-authorized-networks --addons HorizontalPodAutoscaling,HttpLoadBalancing --enable-autoupgrade --enable-autorepair --max-surge-upgrade 1 --max-unavailable-upgrade 0

```

Connect to cluster

```bash
gcloud container clusters get-credentials peak --zone europe-west3-a --project oracle-peak
```

#### Playing around


```bash
kubectl create namespace sandbox
kubens sandbox
```

- https://kubernetes.io/docs/reference/kubectl/cheatsheet/

```bash
helm repo add influxdata https://helm.influxdata.com/
helm search repo influxdata

```

```bash
kubectl port-forward --namespace tick svc/snow-influxdb 8086:8086
kubectl port-forward --namespace tick svc/rain-chronograf 8888:80
```

### influx

NOTES:
InfluxDB can be accessed via port 8086 on the following DNS name from within your cluster:

http://snow-influxdb.tick:8086

You can connect to the remote instance with the influx CLI. To forward the API port to localhost:8086, run the following:

kubectl port-forward --namespace tick $(kubectl get pods --namespace tick -l app=snow-influxdb -o jsonpath='{ .items[0].metadata.name }') 8086:8086

You can also connect to the influx CLI from inside the container. To open a shell session in the InfluxDB pod, run the following:

kubectl exec -i -t --namespace tick $(kubectl get pods --namespace tick -l app=snow-influxdb -o jsonpath='{.items[0].metadata.name}') /bin/sh

To view the logs for the InfluxDB pod, run the following:

kubectl logs -f --namespace tick $(kubectl get pods --namespace tick -l app=snow-influxdb -o jsonpath='{ .items[0].metadata.name }')
