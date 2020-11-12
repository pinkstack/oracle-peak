#!/usr/bin/env bash
set -e

if [[ -z "${ORACLE_PEAK_HOME}" ]]; then
  echo "ORACLE_PEAK_HOME environment variable is not set!"
  exit 255
fi

cd "${ORACLE_PEAK_HOME}"

docker-compose \
  -f .docker/docker-compose.confluent.yml \
  -f .docker/docker-compose.confluent.overrides.yml \
  --project-name oracle-peak \
  --project-directory . $@
