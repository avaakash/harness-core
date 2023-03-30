/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.steps.common.steps.stepgroup;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonTypeName;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@JsonTypeName("Delegate")
@OwnedBy(HarnessTeam.PIPELINE)
public class DelegateInfra implements StepGroupInfra {
  @Builder.Default @NotNull @Getter private StepGroupInfra.Type type = Type.DELEGATE;
}