/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.health;
import static io.harness.exception.WingsException.USER;
import static io.harness.maintenance.MaintenanceController.getMaintenanceFlag;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.eraro.ErrorCode;
import io.harness.exception.NoResultFoundException;
import io.harness.health.HealthException;
import io.harness.health.HealthService;
import io.harness.rest.RestResponse;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.jvm.ThreadDeadlockHealthCheck;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true,
    components = {HarnessModuleComponent.CDS_PIPELINE, HarnessModuleComponent.CDS_TRIGGERS})
@Slf4j
public class HealthResourceImpl implements HealthResource {
  private final HealthService healthService;

  private final ThreadDeadlockHealthCheck threadDeadlockHealthCheck;

  @Inject
  public HealthResourceImpl(HealthService healthService) {
    this.healthService = healthService;
    this.threadDeadlockHealthCheck = new ThreadDeadlockHealthCheck();
  }

  public RestResponse<String> get() throws Exception {
    if (getMaintenanceFlag()) {
      log.info("In maintenance mode. Throwing exception to prevent traffic.");
      throw NoResultFoundException.newBuilder()
          .code(ErrorCode.RESOURCE_NOT_FOUND)
          .message("in maintenance mode")
          .reportTargets(USER)
          .build();
    }

    final HealthCheck.Result check = healthService.check();
    if (check.isHealthy()) {
      return new RestResponse<>("healthy");
    }

    throw new HealthException(check.getMessage(), check.getError());
  }

  @Override
  public RestResponse<String> doLivenessCheck() {
    HealthCheck.Result check = threadDeadlockHealthCheck.execute();
    if (check.isHealthy()) {
      return new RestResponse<>("live");
    }
    log.info(check.getMessage());
    throw new HealthException(check.getMessage(), check.getError());
  }
}
