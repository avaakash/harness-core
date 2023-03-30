/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.settings.repositories;

import static io.harness.rule.OwnerRule.SARTHAK_KASAT;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.idp.settings.beans.entity.BackstagePermissionsEntity;
import io.harness.rule.Owner;

import java.util.List;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(HarnessTeam.IDP)
public class BackstagePermissionsRepositoryCustomImplTest extends CategoryTest {
  AutoCloseable openMocks;
  @Mock MongoTemplate mongoTemplate;
  @InjectMocks BackstagePermissionsRepositoryCustomImpl backstagePermissionsRepositoryCustomImpl;
  static final String TEST_ACCOUNT_IDENTIFIER = "accountId";
  static final String TEST_USERGROUP = "IDP-ADMIN";
  static final List<String> TEST_PERMISSIONS =
      List.of("user_read", "user_update", "user_delete", "owner_read", "owner_update", "owner_delete", "all_create");
  @Before
  public void setUp() {
    openMocks = MockitoAnnotations.openMocks(this);
  }

  @Test
  @Owner(developers = SARTHAK_KASAT)
  @Category(UnitTests.class)
  public void testUpdate() {
    BackstagePermissionsEntity backstagePermissionsEntity = BackstagePermissionsEntity.builder()
                                                                .accountIdentifier(TEST_ACCOUNT_IDENTIFIER)
                                                                .permissions(TEST_PERMISSIONS)
                                                                .userGroup(TEST_USERGROUP)
                                                                .build();
    Criteria criteria = Criteria.where(BackstagePermissionsEntity.BackstagePermissionsEntityKeys.accountIdentifier)
                            .is(backstagePermissionsEntity.getAccountIdentifier());
    Query query = new Query(criteria);
    Update update = new Update();
    update.set(BackstagePermissionsEntity.BackstagePermissionsEntityKeys.permissions,
        backstagePermissionsEntity.getPermissions());
    update.set(
        BackstagePermissionsEntity.BackstagePermissionsEntityKeys.userGroup, backstagePermissionsEntity.getUserGroup());
    when(mongoTemplate.findAndModify(
             eq(query), eq(update), any(FindAndModifyOptions.class), eq(BackstagePermissionsEntity.class)))
        .thenReturn(backstagePermissionsEntity);
    assertEquals(
        backstagePermissionsEntity, backstagePermissionsRepositoryCustomImpl.update(backstagePermissionsEntity));
  }

  @After
  public void tearDown() throws Exception {
    openMocks.close();
  }
}