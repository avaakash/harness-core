/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.creator.plan.stage.v1;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.pipeline.beans.CustomStageSpecParams;
import io.harness.cdng.pipeline.steps.v1.CustomStageStep;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.InvalidYamlException;
import io.harness.plancreator.PlanCreatorUtilsV1;
import io.harness.plancreator.steps.common.v1.StageElementParametersV1.StageElementParametersV1Builder;
import io.harness.plancreator.strategy.StrategyUtilsV1;
import io.harness.pms.contracts.advisers.AdviserObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.contracts.plan.Dependency;
import io.harness.pms.contracts.plan.GraphLayoutNode;
import io.harness.pms.contracts.plan.HarnessStruct;
import io.harness.pms.contracts.plan.HarnessValue;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.plan.creation.PlanCreatorConstants;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.PlanNode.PlanNodeBuilder;
import io.harness.pms.sdk.core.plan.creation.beans.GraphLayoutResponse;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.creators.ChildrenPlanCreator;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.yaml.DependenciesUtils;
import io.harness.pms.yaml.HarnessYamlVersion;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlUtils;
import io.harness.serializer.KryoSerializer;
import io.harness.when.utils.v1.RunInfoUtilsV1;
import io.harness.yaml.core.failurestrategy.v1.FailureConfigV1;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@OwnedBy(HarnessTeam.PIPELINE)
public class CustomStagePlanCreator extends ChildrenPlanCreator<YamlField> {
  @Inject private KryoSerializer kryoSerializer;

  @Override
  public Class<YamlField> getFieldClass() {
    return YamlField.class;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap(
        YAMLFieldNameConstants.STAGE, Collections.singleton(YAMLFieldNameConstants.CUSTOM_V1));
  }

  @Override
  public LinkedHashMap<String, PlanCreationResponse> createPlanForChildrenNodes(
      PlanCreationContext ctx, YamlField field) {
    LinkedHashMap<String, PlanCreationResponse> planCreationResponseMap = new LinkedHashMap<>();
    Map<String, YamlField> dependenciesNodeMap = new HashMap<>();

    YamlField specField =
        Preconditions.checkNotNull(ctx.getCurrentField().getNode().getField(YAMLFieldNameConstants.SPEC));

    // Add dependency for execution
    YamlField stepsField = specField.getNode().getField(YAMLFieldNameConstants.STEPS);
    if (stepsField == null) {
      throw new InvalidRequestException("Steps section is required in Custom stage");
    }
    dependenciesNodeMap.put(specField.getNode().getUuid(), specField);

    // adding support for strategy
    Dependency strategyDependency = getDependencyForStrategy(dependenciesNodeMap, field, ctx);

    planCreationResponseMap.put(specField.getNode().getUuid(),
        PlanCreationResponse.builder()
            .dependencies(DependenciesUtils.toDependenciesProto(dependenciesNodeMap)
                              .toBuilder()
                              .putDependencyMetadata(field.getUuid(), strategyDependency)
                              .putDependencyMetadata(specField.getNode().getUuid(), getDependencyForSteps(field))
                              .build())
            .build());

    return planCreationResponseMap;
  }

  Dependency getDependencyForStrategy(
      Map<String, YamlField> dependenciesNodeMap, YamlField field, PlanCreationContext ctx) {
    Map<String, HarnessValue> dependencyMetadata = StrategyUtilsV1.getStrategyFieldDependencyMetadataIfPresent(
        kryoSerializer, ctx, field.getUuid(), dependenciesNodeMap, getBuild(ctx.getDependency()));
    return Dependency.newBuilder()
        .setNodeMetadata(HarnessStruct.newBuilder().putAllData(dependencyMetadata).build())
        .build();
  }

  Dependency getDependencyForSteps(YamlField field) {
    List<FailureConfigV1> stageFailureStrategies = PlanCreatorUtilsV1.getFailureStrategies(field.getNode());
    if (stageFailureStrategies != null) {
      return Dependency.newBuilder()
          .setParentInfo(
              HarnessStruct.newBuilder()
                  .putData(PlanCreatorConstants.STAGE_FAILURE_STRATEGIES,
                      HarnessValue.newBuilder()
                          .setBytesValue(ByteString.copyFrom(kryoSerializer.asDeflatedBytes(stageFailureStrategies)))
                          .build())
                  .build())
          .build();
    }
    return Dependency.newBuilder().setNodeMetadata(HarnessStruct.newBuilder().build()).build();
  }

  @Override
  public GraphLayoutResponse getLayoutNodeInfo(PlanCreationContext context, YamlField config) {
    Map<String, GraphLayoutNode> stageYamlFieldMap = new LinkedHashMap<>();
    YamlField stageYamlField = context.getCurrentField();
    String nextNodeUuid = PlanCreatorUtilsV1.getNextNodeUuid(kryoSerializer, context.getDependency());
    if (StrategyUtilsV1.isWrappedUnderStrategy(context.getCurrentField())) {
      stageYamlFieldMap = StrategyUtilsV1.modifyStageLayoutNodeGraph(stageYamlField, nextNodeUuid);
    }
    return GraphLayoutResponse.builder().layoutNodes(stageYamlFieldMap).build();
  }

  @Override
  public PlanNode createPlanForParentNode(PlanCreationContext ctx, YamlField config, List<String> childrenNodeIds) {
    CustomStageNodeV1 customStageNode;
    try {
      customStageNode = YamlUtils.read(config.getNode().toString(), CustomStageNodeV1.class);
    } catch (Exception e) {
      throw new InvalidYamlException(
          "Unable to parse custom stage yaml. Please ensure that it is in correct format", e);
    }
    StageElementParametersV1Builder stageParameters = StepParametersUtils.getStageParameters(customStageNode);
    stageParameters.type(YAMLFieldNameConstants.CUSTOM_V1);
    stageParameters.spec(CustomStageSpecParams.builder().childNodeID(childrenNodeIds.get(0)).build());
    String name = config.getNodeName();
    PlanNodeBuilder builder =
        PlanNode.builder()
            .uuid(StrategyUtilsV1.getSwappedPlanNodeId(ctx, config.getUuid()))
            .identifier(StrategyUtilsV1.getIdentifierWithExpression(ctx, config.getId()))
            .stepType(CustomStageStep.STEP_TYPE)
            .group(StepOutcomeGroup.STAGE.name())
            .name(StrategyUtilsV1.getIdentifierWithExpression(ctx, name))
            .skipUnresolvedExpressionsCheck(true)
            .whenCondition(RunInfoUtilsV1.getStageWhenCondition(config))
            .stepParameters(stageParameters.build())
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD).build())
                    .build())
            .skipExpressionChain(false);

    // If strategy present then don't add advisers. Strategy node will take care of running the stage nodes.
    if (config.getNode().getField(YAMLFieldNameConstants.SPEC).getNode().getField("strategy") == null) {
      builder.adviserObtainments(getBuild(ctx.getDependency()));
    }
    return builder.build();
  }

  private List<AdviserObtainment> getBuild(Dependency dependency) {
    return PlanCreatorUtilsV1.getAdviserObtainmentsForStage(kryoSerializer, dependency);
  }

  @Override
  public Set<String> getSupportedYamlVersions() {
    return Set.of(HarnessYamlVersion.V1);
  }
}
