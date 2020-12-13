#!/usr/bin/env bash
set -ex

envsubst < helm/grafana-values.yaml | \
  helm upgrade cookie bitnami/grafana -n tick --values -
