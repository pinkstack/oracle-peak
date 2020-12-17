#!/usr/bin/env bash
set -ex

envsubst < helm/neo4j-values.yaml |
  helm upgrade ro "${NEO4J_HELM_URL}" -n neo --values -
