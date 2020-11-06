# Hetzner

- https://community.hetzner.com/tutorials/create-microk8s-cluster
- https://community.hetzner.com/tutorials/create-microk8s-cluster

```bash
brew install hcloud

hcloud network create --name net-one --ip-range 10.44.0.0/16
hcloud network add-subnet net-one --network-zone eu-central --type server --ip-range 10.44.0.0/24

ubuntu-20.04

hcloud server-type list |grep -i ccx
hcloud server create --type ccx21 --name master-1 --image ubuntu-20.04 --ssh-key pinkstack --network net-one

hcloud server create --type ccx21 --name node-1 --image ubuntu-20.04 --ssh-key pinkstack --network net-one
hcloud server create --type ccx21 --name node-2 --image ubuntu-20.04 --ssh-key pinkstack --network net-one

```


If RBAC is not enabled access the dashboard using the default token retrieved with:

token=$(microk8s kubectl -n kube-system get secret | grep default-token | cut -d " " -f1)
microk8s kubectl -n kube-system describe secret $token

In an RBAC enabled setup (microk8s enable RBAC) you need to create a user with restricted
permissions as shown in:
https://github.com/kubernetes/dashboard/blob/master/docs/user/access-control/creating-sample-user.md


# Dashboard

```bash
kubectl -n kube-system describe secret $(kubectl -n kube-system get secret | grep default-token | cut -d " " -f1)
```

The interface for the first attached network will be named ens10 (for CX, CCX) or enp7s0 (for CPX). 

Additional interfaces will be name ens11 (CX, CCX) or enp8s0 (CPX) for the second 
and ens12 (CX, CCX) or enp9s0 (CPX) for the third.


# Storage

- https://stackoverflow.com/questions/58615019/how-to-change-a-kubernetes-hostpath-provisioner-mount-path
- https://www.server-world.info/en/note?os=Ubuntu_20.04&p=microk8s&f=5
```bash
k get StorageClass
```

- https://github.com/yesteph/training-aws-k8s/blob/e62f7efbac02f652f5b30b00149eaa5106818449/kubernetes-2/exercice-monitoring/grafana.yml


helm install ro https://github.com/neo4j-contrib/neo4j-helm/releases/download/4.1.3-1/neo4j-4.1.3-1.tgz \
    --set core.standalone=true --set acceptLicenseAgreement=yes --set neo4jPassword=dodotest --values 
    
$ helm install mygraph RELEASE_URL --set core.standalone=true --set acceptLicenseAgreement=yes --set neo4jPassword=mySecretPassword

# neo4j

NAME: ro
LAST DEPLOYED: Thu Nov  5 14:08:06 2020
NAMESPACE: oracle-peak
STATUS: deployed
REVISION: 1
NOTES:
Your cluster is now being deployed, and may take up to 5 minutes to become available.
If you'd like to track status and wait on your rollout to complete, run:

$ kubectl rollout status \
    --namespace oracle-peak \
    StatefulSet/ro-neo4j-core \
    --watch

You can inspect your logs containers like so:

We can see the content of the logs by running the following command:

$ kubectl logs --namespace oracle-peak -l \
    "app.kubernetes.io/instance=ro,app.kubernetes.io/name=neo4j,app.kubernetes.io/component=core"

We can now run a query to find the topology of the cluster.

export NEO4J_PASSWORD=$(kubectl get secrets ro-neo4j-secrets --namespace oracle-peak -o yaml | grep password | sed 's/.*: //' | base64 -d)
kubectl run -it --rm cypher-shell \
    --image=neo4j:4.1.3-enterprise \
    --restart=Never \
    --namespace oracle-peak \
    --command -- ./bin/cypher-shell -u neo4j -p "$NEO4J_PASSWORD" -a neo4j://ro-neo4j.oracle-peak.svc.cluster.local "call dbms.routing.getRoutingTable({}, 'system');"

This will print out the addresses of the members of the cluster.

Note:
You'll need to substitute <password> with the password you set when installing the Helm package.
If you didn't set a password, one will be auto generated.
You can find the base64 encoded version of the password by running the following command:

kubectl get secrets ro-neo4j-secrets -o yaml --namespace oracle-peak

```bash
kubectl run -it --rm cypher-shell \
    --image=neo4j:4.1.3-enterprise \
    --restart=Never \
    --namespace oracle-peak \
    --command -- ./bin/cypher-shell -u neo4j -p $(kubectl get secrets ro-neo4j-secrets --namespace oracle-peak -o yaml | grep password | sed 's/.*: //' | base64 -d) -a neo4j://ro-neo4j.oracle-peak.svc.cluster.local "call dbms.routing.getRoutingTable({}, 'system');"
```