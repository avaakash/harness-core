/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.service.step.asg;

import static io.harness.ngmigration.utils.NGMigrationConstants.RUNTIME_FIELD;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.cdng.aws.asg.AsgBlueGreenDeployStepInfo;
import io.harness.cdng.aws.asg.AsgBlueGreenDeployStepNode;
import io.harness.cdng.aws.asg.AsgRollingDeployStepInfo;
import io.harness.cdng.aws.asg.AsgRollingDeployStepNode;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.ngmigration.beans.MigrationContext;
import io.harness.ngmigration.beans.SupportStatus;
import io.harness.ngmigration.beans.WorkflowMigrationContext;
import io.harness.ngmigration.utils.CaseFormat;
import io.harness.ngmigration.utils.MigratorUtility;
import io.harness.plancreator.steps.AbstractStepNode;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.core.timeout.Timeout;

import software.wings.beans.GraphNode;
import software.wings.sm.State;
import software.wings.sm.states.AwsAmiServiceSetup;

import java.util.Collections;
import java.util.Map;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_MIGRATOR})
public class AsgRollingDeployStepMapperImpl extends AsgBaseStepMapper {
  @Override
  public ParameterField<Timeout> getTimeout(State state) {
    if (state instanceof AwsAmiServiceSetup) {
      AwsAmiServiceSetup asgState = (AwsAmiServiceSetup) state;
      if (asgState.getAutoScalingSteadyStateTimeout() > 0) {
        return MigratorUtility.getTimeout((long) asgState.getAutoScalingSteadyStateTimeout() * 1000 * 60);
      }
    }
    return MigratorUtility.getTimeout(null);
  }
  @Override
  public String getStepType(GraphNode stepYaml) {
    AwsAmiServiceSetup state = (AwsAmiServiceSetup) getState(stepYaml);
    return state.isBlueGreen() ? StepSpecTypeConstants.ASG_BLUE_GREEN_DEPLOY : StepSpecTypeConstants.ASG_ROLLING_DEPLOY;
  }

  @Override
  public State getState(GraphNode stepYaml) {
    Map<String, Object> properties = getProperties(stepYaml);
    AwsAmiServiceSetup state = new AwsAmiServiceSetup(stepYaml.getName());
    state.parseProperties(properties);
    return state;
  }

  @Override
  public AbstractStepNode getSpec(
      MigrationContext migrationContext, WorkflowMigrationContext context, GraphNode graphNode) {
    AwsAmiServiceSetup state = (AwsAmiServiceSetup) getState(graphNode);
    if (state.isBlueGreen()) {
      return getBGRollingStepNode(state, context.getIdentifierCaseFormat());
    } else {
      return getRollingStepNode(state, context.getIdentifierCaseFormat());
    }
  }

  private AbstractStepNode getBGRollingStepNode(AwsAmiServiceSetup state, CaseFormat identifierCaseFormat) {
    AsgBlueGreenDeployStepNode node = new AsgBlueGreenDeployStepNode();
    baseSetup(state, node, identifierCaseFormat);
    node.setAsgBlueGreenDeployStepInfo(AsgBlueGreenDeployStepInfo.infoBuilder()
                                           .instances(getAsgInstancesNode(state))
                                           .loadBalancers(ParameterField.createValueField(Collections.emptyList()))
                                           .loadBalancer(RUNTIME_FIELD)
                                           .prodListener(RUNTIME_FIELD)
                                           .prodListenerRuleArn(RUNTIME_FIELD)
                                           .stageListener(RUNTIME_FIELD)
                                           .stageListenerRuleArn(RUNTIME_FIELD)
                                           .useAlreadyRunningInstances(ParameterField.createValueField(false))
                                           .build());
    return node;
  }

  private AbstractStepNode getRollingStepNode(AwsAmiServiceSetup state, CaseFormat identifierCaseFormat) {
    AsgRollingDeployStepNode node = new AsgRollingDeployStepNode();
    baseSetup(state, node, identifierCaseFormat);
    node.setAsgRollingDeployStepInfo(
        AsgRollingDeployStepInfo.infoBuilder()
            .useAlreadyRunningInstances(ParameterField.createValueField(state.isUseCurrentRunningCount()))
            .instances(getAsgInstancesNode(state))
            .skipMatching(ParameterField.createValueField(true))
            .minimumHealthyPercentage(ParameterField.createValueField(100))
            .build());
    return node;
  }

  @Override
  public boolean areSimilar(GraphNode stepYaml1, GraphNode stepYaml2) {
    AwsAmiServiceSetup state1 = (AwsAmiServiceSetup) getState(stepYaml1);
    AwsAmiServiceSetup state2 = (AwsAmiServiceSetup) getState(stepYaml2);
    return state1.isUseCurrentRunningCount() == state2.isUseCurrentRunningCount();
  }

  @Override
  public SupportStatus stepSupportStatus(GraphNode graphNode) {
    return SupportStatus.MANUAL_EFFORT;
  }
}
