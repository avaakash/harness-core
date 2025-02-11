/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ci.execution.states.ssca;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ci.execution.states.AbstractStepExecutable;
import io.harness.pms.contracts.steps.StepType;
import io.harness.ssca.beans.SscaConstants;

@OwnedBy(HarnessTeam.SSCA)
public class ProvenanceStep extends AbstractStepExecutable {
  public static final StepType STEP_TYPE = SscaConstants.PROVENANCE_STEP_TYPE;
}
