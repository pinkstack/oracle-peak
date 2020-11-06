# Hetzner

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