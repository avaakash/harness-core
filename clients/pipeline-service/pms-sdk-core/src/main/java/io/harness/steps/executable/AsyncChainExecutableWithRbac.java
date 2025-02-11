/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.steps.executable;
import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.AsyncChainExecutableResponse;
import io.harness.pms.sdk.core.steps.executables.AsyncChainExecutable;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.security.PmsSecurityContextEventGuard;
import io.harness.supplier.ThrowingSupplier;
import io.harness.tasks.ResponseData;

import lombok.SneakyThrows;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_PIPELINE})
@OwnedBy(PIPELINE)
public interface AsyncChainExecutableWithRbac<T extends StepParameters> extends AsyncChainExecutable<T> {
  void validateResources(Ambiance ambiance, T stepParameters);

  @SneakyThrows
  @Override
  default AsyncChainExecutableResponse startChainLink(
      Ambiance ambiance, T stepParameters, StepInputPackage inputPackage) {
    try (PmsSecurityContextEventGuard securityContextEventGuard = new PmsSecurityContextEventGuard(ambiance)) {
      validateResources(ambiance, stepParameters);
      return this.startChainLinkAfterRbac(ambiance, stepParameters, inputPackage);
    }
  }

  default AsyncChainExecutableResponse executeNextLink(Ambiance ambiance, T stepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData, ThrowingSupplier<ResponseData> responseSupplier)
      throws Exception {
    try (PmsSecurityContextEventGuard securityContextEventGuard = new PmsSecurityContextEventGuard(ambiance)) {
      return this.executeNextLinkWithSecurityContext(
          ambiance, stepParameters, inputPackage, passThroughData, responseSupplier);
    }
  }

  AsyncChainExecutableResponse executeNextLinkWithSecurityContext(Ambiance ambiance, T stepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData, ThrowingSupplier<ResponseData> responseSupplier)
      throws Exception;

  default StepResponse finalizeExecution(Ambiance ambiance, T stepParameters, PassThroughData passThroughData,
      ThrowingSupplier<ResponseData> responseDataSupplier) throws Exception {
    try (PmsSecurityContextEventGuard securityContextEventGuard = new PmsSecurityContextEventGuard(ambiance)) {
      return this.finalizeExecutionWithSecurityContext(ambiance, stepParameters, passThroughData, responseDataSupplier);
    }
  }

  StepResponse finalizeExecutionWithSecurityContext(Ambiance ambiance, T stepParameters,
      PassThroughData passThroughData, ThrowingSupplier<ResponseData> responseDataSupplier) throws Exception;

  AsyncChainExecutableResponse startChainLinkAfterRbac(
      Ambiance ambiance, T stepParameters, StepInputPackage inputPackage);
}
