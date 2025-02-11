/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.repositories.gitfilecache;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.caching.entity.GitFileCache;
import io.harness.gitsync.caching.entity.GitProvider;

import org.springframework.data.repository.CrudRepository;

@HarnessRepo
@OwnedBy(HarnessTeam.PIPELINE)
public interface GitFileCacheRepository extends CrudRepository<GitFileCache, String>, GitFileCacheRepositoryCustom {
  GitFileCache findByAccountIdentifierAndGitProviderAndRepoNameAndRefAndCompleteFilepath(
      String accountIdentifier, GitProvider gitProvider, String repoName, String ref, String completeFilepath);

  GitFileCache findByAccountIdentifierAndGitProviderAndRepoNameAndCompleteFilepathAndIsDefaultBranch(
      String accountIdentifier, GitProvider gitProvider, String repoName, String completeFilepath,
      boolean isDefaultBranch);
}
