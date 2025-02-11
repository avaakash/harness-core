/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.plan.execution.handlers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.engine.observers.SecretObserverInfo;
import io.harness.engine.observers.SecretResolutionObserver;
import io.harness.eventsframework.protohelper.IdentifierRefProtoDTOHelper;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum;
import io.harness.eventsframework.schemas.entity.EntityUsageDetailProto;
import io.harness.eventsframework.schemas.entity.IdentifierRefProtoDTO;
import io.harness.eventsframework.schemas.entity.PipelineExecutionUsageDataProto;
import io.harness.ng.core.entityusageactivity.EntityUsageTypes;
import io.harness.observer.AsyncInformObserver;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.secretusage.SecretRuntimeUsageService;
import io.harness.utils.IdentifierRefHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PIPELINE)
@Slf4j
@Singleton
public class SecretResolutionEventHandler implements AsyncInformObserver, SecretResolutionObserver {
  @Inject @Named("PipelineExecutorService") ExecutorService executorService;
  @Inject SecretRuntimeUsageService secretRuntimeUsageService;

  @Override
  public void onSecretsRuntimeUsage(SecretObserverInfo secretObserverInfo) {
    Ambiance ambiance = secretObserverInfo.getAmbiance();

    IdentifierRefProtoDTO identifierRefProtoDTO = IdentifierRefProtoDTOHelper.createIdentifierRefProtoDTO(
        AmbianceUtils.getAccountId(ambiance), AmbianceUtils.getOrgIdentifier(ambiance),
        AmbianceUtils.getProjectIdentifier(ambiance), AmbianceUtils.getPipelineIdentifier(ambiance));

    IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(secretObserverInfo.getSecretIdentifier(),
        AmbianceUtils.getAccountId(ambiance), AmbianceUtils.getOrgIdentifier(ambiance),
        AmbianceUtils.getProjectIdentifier(ambiance));

    EntityDetailProtoDTO referredByEntity = EntityDetailProtoDTO.newBuilder()
                                                .setIdentifierRef(identifierRefProtoDTO)
                                                .setType(EntityTypeProtoEnum.PIPELINES)
                                                .build();

    EntityUsageDetailProto entityUsageDetail =
        EntityUsageDetailProto.newBuilder()
            .setUsageType(EntityUsageTypes.PIPELINE_EXECUTION)
            .setPipelineExecutionUsageData(
                PipelineExecutionUsageDataProto.newBuilder()
                    .setPlanExecutionId(ambiance.getPlanExecutionId())
                    .setStageExecutionId(AmbianceUtils.getStageExecutionIdForExecutionMode(ambiance))
                    .build())
            .setIdentifierRef(identifierRefProtoDTO)
            .setEntityType(EntityTypeProtoEnum.PIPELINES)
            .build();

    secretRuntimeUsageService.createSecretRuntimeUsage(identifierRef, referredByEntity, entityUsageDetail);
  }

  @Override
  public ExecutorService getInformExecutorService() {
    return executorService;
  }
}
