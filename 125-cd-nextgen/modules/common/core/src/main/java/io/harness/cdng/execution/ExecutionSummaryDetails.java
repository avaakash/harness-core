/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.execution;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.execution.failure.FailureInfo;

import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.CDP)
@Data
@Builder
@RecasterAlias("io.harness.cdng.execution.ExecutionSummaryDetails")
public class ExecutionSummaryDetails {
  ServiceExecutionSummaryDetails serviceInfo;
  InfraExecutionSummaryDetails infraExecutionSummary;
  GitOpsExecutionSummaryDetails gitOpsExecutionSummary;
  GitOpsAppSummaryDetails gitOpsAppSummary;
  FreezeExecutionSummaryDetails freezeExecutionSummary;
  FailureInfo failureInfo;
}
