/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.metrics.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.DelegateRing;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.metrics.AutoMetricContext;
import io.harness.metrics.beans.AccountRingInfoMetricContext;
import io.harness.metrics.beans.DelegateAccountMetricContext;
import io.harness.metrics.beans.DelegateTaskTypeMetricContext;
import io.harness.metrics.beans.HeartbeatMetricContext;
import io.harness.metrics.beans.PerpetualTaskMetricContext;
import io.harness.metrics.intfc.DelegateMetricsService;
import io.harness.metrics.service.api.MetricService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
@Slf4j
@OwnedBy(HarnessTeam.DEL)
public class DelegateMetricsServiceImpl implements DelegateMetricsService {
  public static final String DELEGATE_TASK_CREATION = "delegate_task_creation";
  public static final String DELEGATE_TASK_NO_ELIGIBLE_DELEGATES = "delegate_task_no_eligible_delegates";
  public static final String DELEGATE_TASK_RESPONSE = "delegate_response";
  public static final String DELEGATE_TASK_ACQUIRE = "delegate_task_acquire";
  public static final String DELEGATE_TASK_ACQUIRE_FAILED = "delegate_task_acquire_failed";
  public static final String DELEGATE_TASK_EXPIRED = "delegate_task_expired";
  public static final String DELEGATE_TASK_ACQUIRE_LIMIT_EXCEEDED = "delegate_task_acquire_limit_exceeded";
  public static final String DELEGATE_TASK_REBROADCAST = "delegate_task_rebroadcast";
  public static final String DELEGATE_TASK_VALIDATION = "delegate_task_validation";
  public static final String DELEGATE_TASK_NO_FIRST_WHITELISTED = "delegate_task_no_first_whitelisted";
  public static final String DELEGATE_REGISTRATION_FAILED = "delegate_registration_failed";
  public static final String DELEGATE_REGISTRATION = "delegate_registration";
  public static final String DELEGATE_RESTARTED = "delegate_restarted";
  public static final String DELEGATE_DISCONNECTED = "delegate_disconnected";
  public static final String DELEGATE_DESTROYED = "destroy_delegate";
  public static final String DELEGATE_UPGRADE = "delegate_upgrade";
  public static final String UPGRADER_UPGRADE = "upgrader_upgrade";
  public static final String DELEGATE_LEGACY_UPGRADE = "delegate_legacy_upgrade";

  public static final String PERPETUAL_TASK_CREATE = "perpetual_task_create";
  public static final String PERPETUAL_TASK_RESET = "perpetual_task_reset";
  public static final String PERPETUAL_TASK_DELETE = "perpetual_task_delete";
  public static final String PERPETUAL_TASK_PAUSE = "perpetual_task_pause";
  public static final String PERPETUAL_TASK_ASSIGNED = "perpetual_task_assigned";
  public static final String PERPETUAL_TASK_UNASSIGNED = "perpetual_task_unassigned";
  public static final String PERPETUAL_TASK_NONASSIGNABLE = "perpetual_task_nonassignable";
  public static final String TASK_TYPE_SUFFIX = "_by_type";

  public static final String HEARTBEAT_RECEIVED = "heartbeat_received";
  public static final String ACCOUNT_RING_INFO = "account_ring_info";
  public static final String HEARTBEAT_CONNECTED = "CONNECTED";
  public static final String HEARTBEAT_RECONNECTED = "RE_CONNECTED";
  public static final String HEARTBEAT_DISCONNECTED = "DISCONNECTED";
  public static final String HEARTBEAT_EVENT = "heartBeat";
  public static final String HEARTBEAT_REGISTER_EVENT = "register";
  public static final String HEARTBEAT_UNREGISTER_EVENT = "unregister";
  public static final String HEARTBEAT_DELETE_EVENT = "delete";
  public static final String HEARTBEAT_RESTART_EVENT = "restart";

  public static final String DELEGATE_JWT_CACHE_HIT = "delegate_auth_cache_hit";
  public static final String DELEGATE_JWT_CACHE_MISS = "delegate_auth_cache_miss";

  public static final String DELEGATE_JWT_DECRYPTION_USING_ACCOUNT_KEY = "delegate_jwt_decryption_using_account_key";
  public static final String SECRETS_CACHE_HITS = "delegate_secret_cache_hit";
  public static final String SECRETS_CACHE_LOOKUPS = "delegate_secret_cache_lookups";
  public static final String SECRETS_CACHE_INSERTS = "delegate_secret_cache_inserts";

  public static final String IMMUTABLE_DELEGATES = "immutable_delegate";
  public static final String MUTABLE_DELEGATES = "mutable_delegate";

  public static final String PERPETUAL_TASKS = "perpetual_tasks_num";
  public static final String PERPETUAL_TASKS_ASSIGNED = "perpetual_tasks_assigned_num";
  public static final String PERPETUAL_TASKS_UNASSIGNED = "perpetual_tasks_unassigned_num";
  public static final String PERPETUAL_TASKS_PAUSED = "perpetual_tasks_paused_num";
  public static final String PERPETUAL_TASKS_NON_ASSIGNABLE = "perpetual_tasks_non_assignable_num";
  public static final String PERPETUAL_TASKS_INVALID = "perpetual_invalid_tasks";
  public static final String PERPETUAL_ASSIGNMENT_DELAY = "perpetual_tasks_assignment_delay";

  private final MetricService metricService;
  private final DelegateTaskMetricContextBuilder metricContextBuilder;

  @Override
  public void recordDelegateTaskMetrics(DelegateTask task, String metricName) {
    try (DelegateAccountMetricContext ignore = new DelegateAccountMetricContext(task.getAccountId())) {
      metricService.incCounter(metricName);
    }
    String taskType = null;
    if (task.getTaskDataV2() != null) {
      taskType = task.getTaskDataV2().getTaskType();
    } else if (task.getData() != null) {
      taskType = task.getData().getTaskType();
    }
    if (Objects.isNull(taskType)) {
      return;
    }
    try (AutoMetricContext ignore = new DelegateTaskTypeMetricContext(taskType)) {
      metricService.incCounter(metricName + TASK_TYPE_SUFFIX);
    }
  }

  @Override
  public void recordDelegateTaskMetrics(String accountId, String metricName) {
    try (DelegateAccountMetricContext ignore = new DelegateAccountMetricContext(accountId)) {
      metricService.incCounter(metricName);
    }
  }

  @Override
  public void recordDelegateTaskResponseMetrics(
      DelegateTask delegateTask, DelegateTaskResponse response, String metricName) {
    try (DelegateAccountMetricContext ignore = new DelegateAccountMetricContext(delegateTask.getAccountId())) {
      metricService.incCounter(metricName);
    }
  }

  @Override
  public void recordDelegateMetrics(Delegate delegate, String metricName) {
    if (delegate == null) {
      return;
    }
    try (AutoMetricContext ignore = metricContextBuilder.getContext(delegate, Delegate.class)) {
      metricService.incCounter(metricName);
    }
  }

  @Override
  public void recordPerpetualTaskMetrics(String accountId, String perpetualTaskType, String metricName) {
    try (PerpetualTaskMetricContext ignore = new PerpetualTaskMetricContext(accountId, perpetualTaskType)) {
      metricService.incCounter(metricName);
    }
  }

  @Override
  public void recordDelegateMetricsPerAccount(String accountId, String metricName) {
    try (DelegateAccountMetricContext ignore = new DelegateAccountMetricContext(accountId)) {
      metricService.incCounter(metricName);
    }
  }

  @Override
  public void recordDelegateHeartBeatMetricsPerAccount(String accountId, String accountName, String companyName,
      String orgId, String projectId, String delegateName, String delegateId, String delegateVersion,
      String delegateConnectionStatus, String delegateEventType, boolean isNg, boolean isImmutable, long lastHB,
      String metricName) {
    try (HeartbeatMetricContext ignore =
             new HeartbeatMetricContext(accountId, accountName, companyName, orgId, projectId, delegateName, delegateId,
                 delegateVersion, delegateConnectionStatus, delegateEventType, isNg, isImmutable)) {
      metricService.recordMetric(metricName, lastHB);
    }
  }

  @Override
  public void recordAccountRingInfoMetric(
      String accountId, String accountName, DelegateRing delegateRing, long time, String metricName) {
    try (AccountRingInfoMetricContext ignore = new AccountRingInfoMetricContext(accountId, accountName,
             delegateRing.getRingName(), delegateRing.getDelegateImageTag(), delegateRing.getUpgraderImageTag(),
             delegateRing.getWatcherVersions(), delegateRing.getWatcherJREVersion(),
             delegateRing.getDelegateJREVersion(),
             isNotEmpty(delegateRing.getDelegateVersions()) ? delegateRing.getDelegateVersions().get(0) : null)) {
      metricService.recordMetric(metricName, time);
    }
  }
}
