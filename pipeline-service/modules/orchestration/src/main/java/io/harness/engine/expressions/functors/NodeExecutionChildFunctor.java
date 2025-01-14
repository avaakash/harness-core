/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.engine.expressions.functors;
import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.engine.expressions.NodeExecutionsCache;
import io.harness.engine.pms.data.PmsOutcomeService;
import io.harness.engine.pms.data.PmsSweepingOutputService;
import io.harness.execution.NodeExecution;
import io.harness.expression.LateBindingValue;
import io.harness.graph.stepDetail.service.NodeExecutionInfoService;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;

import java.util.Set;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.jexl3.JexlEngine;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_PIPELINE})
@OwnedBy(CDC)
@Value
@Builder
public class NodeExecutionChildFunctor implements LateBindingValue {
  NodeExecutionsCache nodeExecutionsCache;
  PmsOutcomeService pmsOutcomeService;
  PmsSweepingOutputService pmsSweepingOutputService;
  NodeExecutionInfoService nodeExecutionInfoService;
  Ambiance ambiance;
  Set<NodeExecutionEntityType> entityTypes;
  JexlEngine engine;

  @Override
  public Object bind() {
    String nodeExecutionId = AmbianceUtils.obtainCurrentRuntimeId(ambiance);
    if (nodeExecutionId == null) {
      return null;
    }

    NodeExecution nodeExecution = nodeExecutionsCache.fetch(nodeExecutionId);
    if (nodeExecution == null) {
      return null;
    }

    return NodeExecutionValue.builder()
        .nodeExecutionsCache(nodeExecutionsCache)
        .pmsOutcomeService(pmsOutcomeService)
        .pmsSweepingOutputService(pmsSweepingOutputService)
        .nodeExecutionInfoService(nodeExecutionInfoService)
        .ambiance(ambiance)
        .startNodeExecution(nodeExecution)
        .entityTypes(entityTypes)
        .engine(engine)
        .build()
        .bind();
  }
}
