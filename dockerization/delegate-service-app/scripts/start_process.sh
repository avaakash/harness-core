#!/usr/bin/env bash
# Copyright 2021 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

mkdir -p /opt/harness/logs
touch /opt/harness/logs/delegate-service.log

if [[ -v "{hostname}" ]]; then
   export HOSTNAME=$(hostname)
fi

if [[ -z "$MEMORY" ]]; then
   export MEMORY=4096
fi

if [[ -z "$COMMAND" ]]; then
   export COMMAND=server
fi

echo "Using memory " $MEMORY

if [[ -z "$CAPSULE_JAR" ]]; then
   export CAPSULE_JAR=/opt/harness/delegate-service-capsule.jar
fi


if [[ "${ENABLE_G1GC}" == "true" ]]; then
    export GC_PARAMS=" -XX:+UseG1GC -XX:InitiatingHeapOccupancyPercent=40 -XX:MaxGCPauseMillis=1000 -Dfile.encoding=UTF-8"
else
    export GC_PARAMS=" -XX:+UseParallelGC -XX:MaxGCPauseMillis=500 -Dfile.encoding=UTF-8"
fi

export JAVA_OPTS="-Xms${MEMORY}m -Xmx${MEMORY}m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/heapdump -Xloggc:mygclogfilename.gc $GC_PARAMS $JAVA_ADVANCED_FLAGS $JAVA_17_FLAGS"


if [[ "${ENABLE_APPDYNAMICS}" == "true" ]]; then
    mkdir /opt/harness/AppServerAgent && unzip AppServerAgent.zip -d /opt/harness/AppServerAgent
    node_name="-Dappdynamics.agent.nodeName=$(hostname)"
    JAVA_OPTS=$JAVA_OPTS" -javaagent:/opt/harness/AppServerAgent/javaagent.jar -Dappdynamics.jvm.shutdown.mark.node.as.historical=true"
    JAVA_OPTS="$JAVA_OPTS $node_name"
    echo "Using Appdynamics java agent"
fi

if [[ "${ENABLE_MONITORING}" == "true" ]] ; then
    echo "Monitoring  is enabled"
    JAVA_OPTS="$JAVA_OPTS ${MONITORING_FLAGS}"
    echo "Using inspectIT Java Agent"
fi

if [[ "${ENABLE_OPENTELEMETRY}" == "true" ]] ; then
    echo "OpenTelemetry is enabled"
    JAVA_OPTS=$JAVA_OPTS" -javaagent:/opt/harness/opentelemetry-javaagent.jar -Dotel.service.name=${OTEL_SERVICE_NAME:-delegate-service-app}"

    if [ -n "$OTEL_EXPORTER_OTLP_ENDPOINT" ]; then
        JAVA_OPTS=$JAVA_OPTS" -Dotel.exporter.otlp.endpoint=$OTEL_EXPORTER_OTLP_ENDPOINT "
    else
        echo "OpenTelemetry export is disabled"
        JAVA_OPTS=$JAVA_OPTS" -Dotel.traces.exporter=none -Dotel.metrics.exporter=none "
    fi
    echo "Using OpenTelemetry Java Agent"
fi

if [[ "${DEPLOY_MODE}" == "KUBERNETES" ]] || [[ "${DEPLOY_MODE}" == "KUBERNETES_ONPREM" ]]; then
    java $JAVA_OPTS -jar $CAPSULE_JAR $COMMAND /opt/harness/delegate-service-config.yml
else
    java $JAVA_OPTS -jar $CAPSULE_JAR $COMMAND /opt/harness/delegate-service-config.yml > /opt/harness/logs/delegate-service.log 2>&1
fi
