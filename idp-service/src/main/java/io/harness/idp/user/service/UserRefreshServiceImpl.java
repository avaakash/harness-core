/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.user.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.entity_crud.EntityChangeDTO;
import io.harness.idp.namespace.service.NamespaceService;
import io.harness.idp.user.beans.entity.UserEventEntity;
import io.harness.idp.user.repositories.UserEventRepository;

import com.google.inject.Inject;
import lombok.AllArgsConstructor;
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(HarnessTeam.IDP)
public class UserRefreshServiceImpl implements UserRefreshService {
  UserEventRepository userEventRepository;
  NamespaceService namespaceService;

  @Override
  public void processEntityUpdate(Message message, EntityChangeDTO entityChangeDTO) {
    String accountIdentifier = entityChangeDTO.getAccountIdentifier().getValue();
    String userGroupIdentifier = entityChangeDTO.getIdentifier().getValue();
    if (namespaceService.getAccountIdpStatus(accountIdentifier)) {
      UserEventEntity userEventEntity = UserEventEntity.builder()
                                            .accountIdentifier(accountIdentifier)
                                            .userGroupIdentifier(userGroupIdentifier)
                                            .hasEvent(true)
                                            .build();
      userEventRepository.saveOrUpdate(userEventEntity);
    }
  }
}
