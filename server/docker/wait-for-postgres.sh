#!/bin/sh
# wait-for-postgres.sh host cmd

set -e

host="$1"
shift
cmd="$@"

>&2 echo "waiting for postgres on $host"
until pg_isready -h "$host" -U "${POSTGRES_USER:-dbuser}" -d "${POSTGRES_DB:-db}"; do
  >&2 echo "Postgres is unavailable - sleeping"
  sleep 1
done

>&2 echo "Postgres is up - executing command '$cmd'"
exec $cmd