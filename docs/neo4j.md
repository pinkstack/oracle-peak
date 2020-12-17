# Neo4j

```bash
helm install ro https://github.com/neo4j-contrib/neo4j-helm/releases/download/4.1.3-1/neo4j-4.1.3-1.tgz \
    --set core.standalone=true --set acceptLicenseAgreement=yes --set neo4jPassword=dodotest --values kubernetes/neo4j-values.yaml
```


```bash
NEO4J_HELM_URL=https://github.com/neo4j-contrib/neo4j-helm/releases/download/4.2.0-1/neo4j-4.2.0-1.tgz
helm template --name-template tester --set acceptLicenseAgreement=yes --set neo4jPassword=mySecretPassword . > expanded.yaml
```

---

Your cluster is now being deployed, and may take up to 5 minutes to become available.
If you'd like to track status and wait on your rollout to complete, run:

```bash
$ kubectl rollout status \
  --namespace neo \
  StatefulSet/ro-neo4j-core \
  --watch
```

You can inspect your logs containers like so:

We can see the content of the logs by running the following command:

```bash
$ kubectl logs --namespace neo -l \
"app.kubernetes.io/instance=ro,app.kubernetes.io/name=neo4j,app.kubernetes.io/component=core"
```

We can now run a query to find the topology of the cluster.

```bash
export NEO4J_PASSWORD=$(kubectl get secrets ro-neo4j-secrets --namespace neo -o jsonpath='{.data.neo4j-password}' | base64 -d)
kubectl run -it --rm cypher-shell \
  --image=neo4j:4.2.0-enterprise \
  --restart=Never \
  --namespace neo \
  --command -- ./bin/cypher-shell -u neo4j -p "$NEO4J_PASSWORD" -a neo4j://ro-neo4j.neo.svc.cluster.local "call dbms.routing.getRoutingTable({}, 'system');"
```

This will print out the addresses of the members of the cluster.

Note:
You'll need to substitute <password> with the password you set when installing the Helm package.
If you didn't set a password, one will be auto generated.
You can find the base64 encoded version of the password by running the following command:

```bash
kubectl get secrets ro-neo4j-secrets -o yaml --namespace neo
```

---

bolt://ro-neo4j-core-0.ro-neo4j.neo.svc.cluster.local:7687
bolt://127.0.0.1:7687

```cypher
MATCH (n) DETACH DELETE n;
```