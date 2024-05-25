#!/bin/sh
curl -m 5 --silent --fail --request GET http://localhost:42069/actuator/health | \
  jq --exit-status -n 'inputs | if has("status") then .status=="UP" else false end' > /dev/null && exit 0
curl -m 5 --silent --fail --request GET https://localhost:42069/actuator/health | \
  jq --exit-status -n 'inputs | if has("status") then .status=="UP" else false end' > /dev/null && exit 0
exit 1