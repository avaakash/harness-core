/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.repositories.userSourceCodeManager;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.beans.UserSourceCodeManager;

import java.util.List;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
@OwnedBy(HarnessTeam.PIPELINE)
public interface UserSourceCodeManagerRepository extends PagingAndSortingRepository<UserSourceCodeManager, String> {
  List<UserSourceCodeManager> findByAccountIdentifierAndUserIdentifier(String accountIdentifier, String userIdentifier);
  List<UserSourceCodeManager> findByAccountIdentifierAndUserIdentifierAndType(
      String accountIdentifier, String userIdentifier, String type);
  long deleteByAccountIdentifierAndUserIdentifier(String accountIdentifier, String userIdentifier);
  long deleteByAccountIdentifierAndUserIdentifierAndType(String accountIdentifier, String userIdentifier, String type);
}