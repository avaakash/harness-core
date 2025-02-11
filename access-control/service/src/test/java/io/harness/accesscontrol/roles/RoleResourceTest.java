/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.roles;

import static io.harness.accesscontrol.AccessControlPermissions.EDIT_ROLE_PERMISSION;
import static io.harness.accesscontrol.AccessControlPermissions.VIEW_ROLE_PERMISSION;
import static io.harness.accesscontrol.AccessControlResourceTypes.ROLE;
import static io.harness.accesscontrol.common.filter.ManagedFilter.NO_FILTER;
import static io.harness.accesscontrol.common.filter.ManagedFilter.ONLY_CUSTOM;
import static io.harness.accesscontrol.scopes.harness.ScopeMapper.fromParams;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.rule.OwnerRule.ADITYA;
import static io.harness.rule.OwnerRule.ASHISHSANODIA;
import static io.harness.rule.OwnerRule.JIMIT_GANDHI;
import static io.harness.rule.OwnerRule.KARAN;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.accesscontrol.AccessControlTestBase;
import io.harness.accesscontrol.acl.api.Resource;
import io.harness.accesscontrol.acl.api.ResourceScope;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.common.filter.ManagedFilter;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.accesscontrol.roleassignments.RoleAssignment;
import io.harness.accesscontrol.roleassignments.RoleAssignmentFilter;
import io.harness.accesscontrol.roleassignments.RoleAssignmentService;
import io.harness.accesscontrol.roles.api.RoleDTO;
import io.harness.accesscontrol.roles.api.RoleDTOMapper;
import io.harness.accesscontrol.roles.api.RoleResource;
import io.harness.accesscontrol.roles.api.RoleResourceImpl;
import io.harness.accesscontrol.roles.api.RoleResponseDTO;
import io.harness.accesscontrol.roles.api.RoleWithPrincipalCountDTOMapper;
import io.harness.accesscontrol.roles.api.RoleWithPrincipalCountResponseDTO;
import io.harness.accesscontrol.roles.filter.RoleFilter;
import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.accesscontrol.scopes.harness.HarnessScopeParams;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.ff.FeatureFlagService;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.rule.Owner;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import javax.ws.rs.NotFoundException;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;

@OwnedBy(PL)
public class RoleResourceTest extends AccessControlTestBase {
  private RoleService roleService;
  private ScopeService scopeService;
  private RoleDTOMapper roleDTOMapper;
  private AccessControlClient accessControlClient;
  private RoleResource roleResource;
  private PageRequest pageRequest;
  private String accountIdentifier;
  private String orgIdentifier;
  private String projectIdentifier;
  private HarnessScopeParams harnessScopeParams;
  private ResourceScope resourceScope;
  private FeatureFlagService featureFlagService;
  private RoleWithPrincipalCountDTOMapper roleWithPrincipalCountDTOMapper;
  private RoleAssignmentService roleAssignmentService;

  @Before
  public void setup() {
    roleService = mock(RoleService.class);
    scopeService = mock(ScopeService.class);
    roleDTOMapper = mock(RoleDTOMapper.class);
    accessControlClient = mock(AccessControlClient.class);
    featureFlagService = mock(FeatureFlagService.class);
    roleWithPrincipalCountDTOMapper = mock(RoleWithPrincipalCountDTOMapper.class);
    roleAssignmentService = mock(RoleAssignmentService.class);
    roleResource = new RoleResourceImpl(roleService, scopeService, roleDTOMapper, accessControlClient,
        featureFlagService, roleWithPrincipalCountDTOMapper);
    pageRequest = PageRequest.builder().pageIndex(0).pageSize(50).build();
    accountIdentifier = randomAlphabetic(10);
    orgIdentifier = randomAlphabetic(10);
    projectIdentifier = randomAlphabetic(10);
    harnessScopeParams =
        HarnessScopeParams.builder().accountIdentifier(accountIdentifier).orgIdentifier(orgIdentifier).build();
    resourceScope = ResourceScope.builder()
                        .projectIdentifier(harnessScopeParams.getProjectIdentifier())
                        .orgIdentifier(harnessScopeParams.getOrgIdentifier())
                        .accountIdentifier(harnessScopeParams.getAccountIdentifier())
                        .build();
  }

  @Test
  @Owner(developers = {KARAN, ADITYA})
  @Category(UnitTests.class)
  public void testList() {
    String searchTerm = randomAlphabetic(10);
    HarnessScopeParams scopeParams = HarnessScopeParams.builder().accountIdentifier(accountIdentifier).build();
    doNothing()
        .when(accessControlClient)
        .checkForAccessOrThrow(resourceScope, Resource.of(ROLE, null), VIEW_ROLE_PERMISSION);
    when(roleService.listWithPrincipalCount(eq(pageRequest), any(), eq(true)))
        .thenReturn(PageResponse.getEmptyPageResponse(pageRequest));
    ResponseDTO<PageResponse<RoleWithPrincipalCountResponseDTO>> response =
        roleResource.get(pageRequest, scopeParams, searchTerm);
    assertTrue(response.getData().isEmpty());
    verify(accessControlClient, times(1)).checkForAccessOrThrow(any(), any(), any());

    ArgumentCaptor<RoleFilter> captor = ArgumentCaptor.forClass(RoleFilter.class);
    verify(roleService, times(1)).listWithPrincipalCount(any(), captor.capture(), eq(true));
    Assertions.assertThat(captor.getValue().getManagedFilter()).isEqualTo(NO_FILTER);
  }

  @Test
  @Owner(developers = {ASHISHSANODIA, ADITYA})
  @Category(UnitTests.class)
  public void testListWithHideOrgLevelManagedRolesIfFFIsEnabled() {
    String searchTerm = randomAlphabetic(10);
    HarnessScopeParams scopeParams =
        HarnessScopeParams.builder().accountIdentifier(accountIdentifier).orgIdentifier(orgIdentifier).build();
    when(featureFlagService.isEnabled(FeatureName.PL_HIDE_ORGANIZATION_LEVEL_MANAGED_ROLE, accountIdentifier))
        .thenReturn(true);
    doNothing()
        .when(accessControlClient)
        .checkForAccessOrThrow(resourceScope, Resource.of(ROLE, null), VIEW_ROLE_PERMISSION);
    when(roleService.listWithPrincipalCount(eq(pageRequest), any(), eq(true)))
        .thenReturn(PageResponse.getEmptyPageResponse(pageRequest));
    ResponseDTO<PageResponse<RoleWithPrincipalCountResponseDTO>> response =
        roleResource.get(pageRequest, scopeParams, searchTerm);
    assertTrue(response.getData().isEmpty());
    verify(accessControlClient, times(1)).checkForAccessOrThrow(any(), any(), any());
    ArgumentCaptor<RoleFilter> captor = ArgumentCaptor.forClass(RoleFilter.class);
    verify(roleService, times(1)).listWithPrincipalCount(any(), captor.capture(), eq(true));
    Assertions.assertThat(captor.getValue().getManagedFilter()).isEqualTo(ONLY_CUSTOM);
  }

  @Test
  @Owner(developers = {ASHISHSANODIA, ADITYA})
  @Category(UnitTests.class)
  public void testListToShowOrgLevelManagedRolesIfFFIsDisabled() {
    String searchTerm = randomAlphabetic(10);
    HarnessScopeParams scopeParams =
        HarnessScopeParams.builder().accountIdentifier(accountIdentifier).orgIdentifier(orgIdentifier).build();
    when(featureFlagService.isEnabled(FeatureName.PL_HIDE_ORGANIZATION_LEVEL_MANAGED_ROLE, accountIdentifier))
        .thenReturn(false);
    doNothing()
        .when(accessControlClient)
        .checkForAccessOrThrow(resourceScope, Resource.of(ROLE, null), VIEW_ROLE_PERMISSION);
    when(roleService.listWithPrincipalCount(eq(pageRequest), any(), eq(true)))
        .thenReturn(PageResponse.getEmptyPageResponse(pageRequest));
    ResponseDTO<PageResponse<RoleWithPrincipalCountResponseDTO>> response =
        roleResource.get(pageRequest, scopeParams, searchTerm);
    assertTrue(response.getData().isEmpty());
    verify(accessControlClient, times(1)).checkForAccessOrThrow(any(), any(), any());
    ArgumentCaptor<RoleFilter> captor = ArgumentCaptor.forClass(RoleFilter.class);
    verify(roleService, times(1)).listWithPrincipalCount(any(), captor.capture(), eq(true));
    Assertions.assertThat(captor.getValue().getManagedFilter()).isEqualTo(NO_FILTER);
  }

  @Test
  @Owner(developers = {ASHISHSANODIA, ADITYA})
  @Category(UnitTests.class)
  public void testListToShowOrgLevelManagedRolesIfFFIsEnabledForProject() {
    String searchTerm = randomAlphabetic(10);
    HarnessScopeParams scopeParams =
        HarnessScopeParams.builder().accountIdentifier(accountIdentifier).orgIdentifier(orgIdentifier).build();
    when(featureFlagService.isEnabled(FeatureName.PL_HIDE_PROJECT_LEVEL_MANAGED_ROLE, accountIdentifier))
        .thenReturn(true);
    doNothing()
        .when(accessControlClient)
        .checkForAccessOrThrow(resourceScope, Resource.of(ROLE, null), VIEW_ROLE_PERMISSION);
    when(roleService.listWithPrincipalCount(eq(pageRequest), any(), eq(true)))
        .thenReturn(PageResponse.getEmptyPageResponse(pageRequest));
    ResponseDTO<PageResponse<RoleWithPrincipalCountResponseDTO>> response =
        roleResource.get(pageRequest, scopeParams, searchTerm);
    assertTrue(response.getData().isEmpty());
    verify(accessControlClient, times(1)).checkForAccessOrThrow(any(), any(), any());
    ArgumentCaptor<RoleFilter> captor = ArgumentCaptor.forClass(RoleFilter.class);
    verify(roleService, times(1)).listWithPrincipalCount(any(), captor.capture(), eq(true));
    Assertions.assertThat(captor.getValue().getManagedFilter()).isEqualTo(NO_FILTER);
  }

  @Test
  @Owner(developers = {ASHISHSANODIA, ADITYA})
  @Category(UnitTests.class)
  public void testListWithHideProjectLevelManagedRolesIfFFIsEnabled() {
    String searchTerm = randomAlphabetic(10);
    HarnessScopeParams scopeParams = HarnessScopeParams.builder()
                                         .accountIdentifier(accountIdentifier)
                                         .orgIdentifier(orgIdentifier)
                                         .projectIdentifier(projectIdentifier)
                                         .build();
    when(featureFlagService.isEnabled(FeatureName.PL_HIDE_PROJECT_LEVEL_MANAGED_ROLE, accountIdentifier))
        .thenReturn(true);
    doNothing()
        .when(accessControlClient)
        .checkForAccessOrThrow(resourceScope, Resource.of(ROLE, null), VIEW_ROLE_PERMISSION);
    when(roleService.listWithPrincipalCount(eq(pageRequest), any(), eq(true)))
        .thenReturn(PageResponse.getEmptyPageResponse(pageRequest));
    ResponseDTO<PageResponse<RoleWithPrincipalCountResponseDTO>> response =
        roleResource.get(pageRequest, scopeParams, searchTerm);
    assertTrue(response.getData().isEmpty());
    verify(accessControlClient, times(1)).checkForAccessOrThrow(any(), any(), any());
    ArgumentCaptor<RoleFilter> captor = ArgumentCaptor.forClass(RoleFilter.class);
    verify(roleService, times(1)).listWithPrincipalCount(any(), captor.capture(), eq(true));
    Assertions.assertThat(captor.getValue().getManagedFilter()).isEqualTo(ONLY_CUSTOM);
  }

  @Test
  @Owner(developers = {ADITYA, ASHISHSANODIA})
  @Category(UnitTests.class)
  public void testListToShowProjectLevelManagedRolesIfFFIsDisabled() {
    String searchTerm = randomAlphabetic(10);
    HarnessScopeParams scopeParams = HarnessScopeParams.builder()
                                         .accountIdentifier(accountIdentifier)
                                         .orgIdentifier(orgIdentifier)
                                         .projectIdentifier(projectIdentifier)
                                         .build();
    when(featureFlagService.isEnabled(FeatureName.PL_HIDE_PROJECT_LEVEL_MANAGED_ROLE, accountIdentifier))
        .thenReturn(false);
    doNothing()
        .when(accessControlClient)
        .checkForAccessOrThrow(resourceScope, Resource.of(ROLE, null), VIEW_ROLE_PERMISSION);
    when(roleService.listWithPrincipalCount(eq(pageRequest), any(), eq(true)))
        .thenReturn(PageResponse.getEmptyPageResponse(pageRequest));
    ResponseDTO<PageResponse<RoleWithPrincipalCountResponseDTO>> response =
        roleResource.get(pageRequest, scopeParams, searchTerm);
    assertTrue(response.getData().isEmpty());
    verify(accessControlClient, times(1)).checkForAccessOrThrow(any(), any(), any());
    ArgumentCaptor<RoleFilter> captor = ArgumentCaptor.forClass(RoleFilter.class);
    verify(roleService, times(1)).listWithPrincipalCount(any(), captor.capture(), eq(true));
    Assertions.assertThat(captor.getValue().getManagedFilter()).isEqualTo(NO_FILTER);
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testGet() {
    String identifier = randomAlphabetic(10);
    doNothing()
        .when(accessControlClient)
        .checkForAccessOrThrow(resourceScope, Resource.of(ROLE, identifier), VIEW_ROLE_PERMISSION);
    String scopeIdentifier = fromParams(harnessScopeParams).toString();
    Role role = Role.builder().scopeIdentifier(scopeIdentifier).identifier(identifier).build();
    when(roleService.get(identifier, scopeIdentifier, NO_FILTER)).thenReturn(Optional.of(role));
    RoleResponseDTO roleResponseDTO =
        RoleResponseDTO.builder().role(RoleDTO.builder().identifier(identifier).build()).build();
    when(roleDTOMapper.toResponseDTO(role)).thenReturn(roleResponseDTO);
    ResponseDTO<RoleResponseDTO> response = roleResource.get(identifier, harnessScopeParams);
    assertEquals(roleResponseDTO, response.getData());
    verify(accessControlClient, times(1)).checkForAccessOrThrow(any(), any(), any());
    verify(roleService, times(1)).get(any(), any(), any());
    verify(roleDTOMapper, times(1)).toResponseDTO(any());
  }

  @Test
  @Owner(developers = ASHISHSANODIA)
  @Category(UnitTests.class)
  public void testGetToHideOrgLevelManagedRolesIfFFIsEnabled() {
    String identifier = randomAlphabetic(10);
    HarnessScopeParams scopeParams =
        HarnessScopeParams.builder().accountIdentifier(accountIdentifier).orgIdentifier(orgIdentifier).build();
    when(featureFlagService.isEnabled(FeatureName.PL_HIDE_ORGANIZATION_LEVEL_MANAGED_ROLE, accountIdentifier))
        .thenReturn(true);
    doNothing()
        .when(accessControlClient)
        .checkForAccessOrThrow(resourceScope, Resource.of(ROLE, null), VIEW_ROLE_PERMISSION);
    String scopeIdentifier = fromParams(scopeParams).toString();
    Role role = Role.builder().scopeIdentifier(scopeIdentifier).identifier(identifier).build();
    when(roleService.get(eq(identifier), eq(scopeIdentifier), any())).thenReturn(Optional.of(role));
    RoleResponseDTO roleResponseDTO =
        RoleResponseDTO.builder().role(RoleDTO.builder().identifier(identifier).build()).build();
    when(roleDTOMapper.toResponseDTO(role)).thenReturn(roleResponseDTO);

    roleResource.get(identifier, scopeParams);

    ArgumentCaptor<ManagedFilter> captor = ArgumentCaptor.forClass(ManagedFilter.class);
    verify(roleService, times(1)).get(any(), any(), captor.capture());
    Assertions.assertThat(captor.getValue()).isEqualTo(ONLY_CUSTOM);
  }

  @Test
  @Owner(developers = ASHISHSANODIA)
  @Category(UnitTests.class)
  public void testGetToShowOrgLevelManagedRolesIfFFIsDisabled() {
    String identifier = randomAlphabetic(10);
    HarnessScopeParams scopeParams =
        HarnessScopeParams.builder().accountIdentifier(accountIdentifier).orgIdentifier(orgIdentifier).build();
    when(featureFlagService.isEnabled(FeatureName.PL_HIDE_ORGANIZATION_LEVEL_MANAGED_ROLE, accountIdentifier))
        .thenReturn(false);
    doNothing()
        .when(accessControlClient)
        .checkForAccessOrThrow(resourceScope, Resource.of(ROLE, null), VIEW_ROLE_PERMISSION);
    String scopeIdentifier = fromParams(scopeParams).toString();
    Role role = Role.builder().scopeIdentifier(scopeIdentifier).identifier(identifier).build();
    when(roleService.get(eq(identifier), eq(scopeIdentifier), any())).thenReturn(Optional.of(role));
    RoleResponseDTO roleResponseDTO =
        RoleResponseDTO.builder().role(RoleDTO.builder().identifier(identifier).build()).build();
    when(roleDTOMapper.toResponseDTO(role)).thenReturn(roleResponseDTO);

    roleResource.get(identifier, scopeParams);

    ArgumentCaptor<ManagedFilter> captor = ArgumentCaptor.forClass(ManagedFilter.class);
    verify(roleService, times(1)).get(any(), any(), captor.capture());
    Assertions.assertThat(captor.getValue()).isEqualTo(NO_FILTER);
  }

  @Test
  @Owner(developers = ASHISHSANODIA)
  @Category(UnitTests.class)
  public void testGetToShowOrgLevelManagedRolesIfFFIsEnabledForProjectLevel() {
    String identifier = randomAlphabetic(10);
    HarnessScopeParams scopeParams =
        HarnessScopeParams.builder().accountIdentifier(accountIdentifier).orgIdentifier(orgIdentifier).build();
    when(featureFlagService.isEnabled(FeatureName.PL_HIDE_PROJECT_LEVEL_MANAGED_ROLE, accountIdentifier))
        .thenReturn(true);
    doNothing()
        .when(accessControlClient)
        .checkForAccessOrThrow(resourceScope, Resource.of(ROLE, null), VIEW_ROLE_PERMISSION);
    String scopeIdentifier = fromParams(scopeParams).toString();
    Role role = Role.builder().scopeIdentifier(scopeIdentifier).identifier(identifier).build();
    when(roleService.get(eq(identifier), eq(scopeIdentifier), any())).thenReturn(Optional.of(role));
    RoleResponseDTO roleResponseDTO =
        RoleResponseDTO.builder().role(RoleDTO.builder().identifier(identifier).build()).build();
    when(roleDTOMapper.toResponseDTO(role)).thenReturn(roleResponseDTO);

    roleResource.get(identifier, scopeParams);

    ArgumentCaptor<ManagedFilter> captor = ArgumentCaptor.forClass(ManagedFilter.class);
    verify(roleService, times(1)).get(any(), any(), captor.capture());
    Assertions.assertThat(captor.getValue()).isEqualTo(NO_FILTER);
  }

  @Test
  @Owner(developers = ASHISHSANODIA)
  @Category(UnitTests.class)
  public void testGetToHideProjectLevelManagedRolesIfFFIsEnabled() {
    String identifier = randomAlphabetic(10);
    HarnessScopeParams scopeParams = HarnessScopeParams.builder()
                                         .accountIdentifier(accountIdentifier)
                                         .orgIdentifier(orgIdentifier)
                                         .projectIdentifier(projectIdentifier)
                                         .build();
    when(featureFlagService.isEnabled(FeatureName.PL_HIDE_PROJECT_LEVEL_MANAGED_ROLE, accountIdentifier))
        .thenReturn(true);
    doNothing()
        .when(accessControlClient)
        .checkForAccessOrThrow(resourceScope, Resource.of(ROLE, null), VIEW_ROLE_PERMISSION);
    String scopeIdentifier = fromParams(scopeParams).toString();
    Role role = Role.builder().scopeIdentifier(scopeIdentifier).identifier(identifier).build();
    when(roleService.get(eq(identifier), eq(scopeIdentifier), any())).thenReturn(Optional.of(role));
    RoleResponseDTO roleResponseDTO =
        RoleResponseDTO.builder().role(RoleDTO.builder().identifier(identifier).build()).build();
    when(roleDTOMapper.toResponseDTO(role)).thenReturn(roleResponseDTO);

    roleResource.get(identifier, scopeParams);

    ArgumentCaptor<ManagedFilter> captor = ArgumentCaptor.forClass(ManagedFilter.class);
    verify(roleService, times(1)).get(any(), any(), captor.capture());
    Assertions.assertThat(captor.getValue()).isEqualTo(ONLY_CUSTOM);
  }

  @Test
  @Owner(developers = ASHISHSANODIA)
  @Category(UnitTests.class)
  public void testGetToShowProjectLevelManagedRolesIfFFIsDisabled() {
    String identifier = randomAlphabetic(10);
    HarnessScopeParams scopeParams = HarnessScopeParams.builder()
                                         .accountIdentifier(accountIdentifier)
                                         .orgIdentifier(orgIdentifier)
                                         .projectIdentifier(projectIdentifier)
                                         .build();
    when(featureFlagService.isEnabled(FeatureName.PL_HIDE_PROJECT_LEVEL_MANAGED_ROLE, accountIdentifier))
        .thenReturn(false);
    doNothing()
        .when(accessControlClient)
        .checkForAccessOrThrow(resourceScope, Resource.of(ROLE, null), VIEW_ROLE_PERMISSION);
    String scopeIdentifier = fromParams(scopeParams).toString();
    Role role = Role.builder().scopeIdentifier(scopeIdentifier).identifier(identifier).build();
    when(roleService.get(eq(identifier), eq(scopeIdentifier), any())).thenReturn(Optional.of(role));
    RoleResponseDTO roleResponseDTO =
        RoleResponseDTO.builder().role(RoleDTO.builder().identifier(identifier).build()).build();
    when(roleDTOMapper.toResponseDTO(role)).thenReturn(roleResponseDTO);

    roleResource.get(identifier, scopeParams);

    ArgumentCaptor<ManagedFilter> captor = ArgumentCaptor.forClass(ManagedFilter.class);
    verify(roleService, times(1)).get(any(), any(), captor.capture());
    Assertions.assertThat(captor.getValue()).isEqualTo(NO_FILTER);
  }

  @Test(expected = NotFoundException.class)
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testGetNotFound() {
    String identifier = randomAlphabetic(10);
    doNothing()
        .when(accessControlClient)
        .checkForAccessOrThrow(resourceScope, Resource.of(ROLE, identifier), VIEW_ROLE_PERMISSION);
    String scopeIdentifier = fromParams(harnessScopeParams).toString();
    when(roleService.get(identifier, scopeIdentifier, NO_FILTER)).thenReturn(Optional.empty());
    roleResource.get(identifier, harnessScopeParams);
  }

  @Test
  @Owner(developers = {KARAN, JIMIT_GANDHI})
  @Category(UnitTests.class)
  public void testUpdate() {
    String identifier = randomAlphabetic(10);
    RoleDTO roleDTO = RoleDTO.builder().identifier(identifier).build();
    Scope scope = fromParams(harnessScopeParams);
    Role role = RoleDTOMapper.fromDTO(scope.toString(), roleDTO);
    RoleResponseDTO roleResponseDTO = RoleResponseDTO.builder().role(roleDTO).build();
    when(roleService.update(role)).thenReturn(role);
    when(scopeService.buildScopeFromScopeIdentifier(any())).thenReturn(scope);
    when(roleDTOMapper.toResponseDTO(role)).thenReturn(roleResponseDTO);
    doNothing()
        .when(accessControlClient)
        .checkForAccessOrThrow(resourceScope, Resource.of(ROLE, identifier), EDIT_ROLE_PERMISSION);
    ResponseDTO<RoleResponseDTO> response = roleResource.update(identifier, harnessScopeParams, roleDTO);
    assertEquals(roleResponseDTO, response.getData());
    verify(accessControlClient, times(1)).checkForAccessOrThrow(any(), any(), any());
    verify(roleService, times(1)).update(role);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testUpdateInvalidIdentifier() {
    String identifier = randomAlphabetic(10);
    RoleDTO roleDTO = RoleDTO.builder().identifier(randomAlphabetic(11)).build();
    doNothing()
        .when(accessControlClient)
        .checkForAccessOrThrow(resourceScope, Resource.of(ROLE, identifier), EDIT_ROLE_PERMISSION);
    roleResource.update(identifier, harnessScopeParams, roleDTO);
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testCreate() {
    String identifier = randomAlphabetic(10);
    RoleDTO roleDTO = RoleDTO.builder().identifier(identifier).build();
    doNothing()
        .when(accessControlClient)
        .checkForAccessOrThrow(resourceScope, Resource.of(ROLE, null), EDIT_ROLE_PERMISSION);
    Scope scope = fromParams(harnessScopeParams);
    when(scopeService.getOrCreate(fromParams(harnessScopeParams))).thenReturn(scope);
    RoleResponseDTO roleResponseDTO = RoleResponseDTO.builder().role(roleDTO).build();
    Role role = RoleDTOMapper.fromDTO(scope.toString(), roleDTO);
    when(roleDTOMapper.toResponseDTO(role)).thenReturn(roleResponseDTO);
    when(roleService.create(any())).thenReturn(role);
    ResponseDTO<RoleResponseDTO> response = roleResource.create(accountIdentifier, orgIdentifier, null, roleDTO);
    assertEquals(roleResponseDTO, response.getData());
    verify(accessControlClient, times(1)).checkForAccessOrThrow(any(), any(), any());
    verify(scopeService, times(1)).getOrCreate(any());
    verify(roleService, times(1)).create(any());
  }

  @Test
  @Owner(developers = {KARAN, JIMIT_GANDHI})
  @Category(UnitTests.class)
  public void testDelete() {
    String identifier = randomAlphabetic(10);
    doNothing()
        .when(accessControlClient)
        .checkForAccessOrThrow(resourceScope, Resource.of(ROLE, identifier), EDIT_ROLE_PERMISSION);
    RoleDTO roleDTO = RoleDTO.builder().identifier(identifier).build();
    Scope scope = fromParams(harnessScopeParams);
    Role role = RoleDTOMapper.fromDTO(scope.toString(), roleDTO);
    RoleResponseDTO roleResponseDTO = RoleResponseDTO.builder().role(roleDTO).build();
    when(roleDTOMapper.toResponseDTO(role)).thenReturn(roleResponseDTO);
    when(roleService.delete(identifier, scope.toString())).thenReturn(role);
    ResponseDTO<RoleResponseDTO> response = roleResource.delete(identifier, harnessScopeParams);
    assertEquals(roleResponseDTO, response.getData());
    verify(accessControlClient, times(1)).checkForAccessOrThrow(any(), any(), any());
    verify(roleService, times(1)).delete(identifier, scope.toString());
  }

  @Test
  @Owner(developers = ADITYA)
  @Category(UnitTests.class)
  public void testUserAssignedToRoleCount() {
    PageRequest pageRequest = PageRequest.builder().pageIndex(0).pageSize(50).build();
    RoleFilter roleFilter = RoleFilter.builder().scopeIdentifier(randomAlphabetic(10)).build();
    RoleAssignment roleAssignmentToUser = RoleAssignment.builder()
                                              .identifier(randomAlphabetic(10))
                                              .scopeIdentifier(randomAlphabetic(10))
                                              .roleIdentifier(randomAlphabetic(10))
                                              .principalType(PrincipalType.USER)
                                              .build();
    PageResponse<RoleAssignment> roleAssignmentPageResponse =
        PageResponse.<RoleAssignment>builder()
            .content(Collections.singletonList(roleAssignmentToUser))
            .totalPages(1)
            .totalItems(1)
            .pageItemCount(1)
            .pageSize(50)
            .pageIndex(0)
            .empty(false)
            .build();
    when(roleAssignmentService.list(any(), any(RoleAssignmentFilter.class), eq(true)))
        .thenReturn(roleAssignmentPageResponse);

    Role role = Role.builder().identifier("userRole").name(randomAlphabetic(10)).build();
    RoleWithPrincipalCount roleWithPrincipalCount = RoleWithPrincipalCount.builder()
                                                        .role(role)
                                                        .roleAssignedToUserCount(1)
                                                        .roleAssignedToUserGroupCount(0)
                                                        .roleAssignedToServiceAccountCount(0)
                                                        .build();

    PageResponse<RoleWithPrincipalCount> rolePageResponse =
        PageResponse.<RoleWithPrincipalCount>builder()
            .content(Collections.singletonList(roleWithPrincipalCount))
            .totalPages(1)
            .totalItems(1)
            .pageItemCount(1)
            .pageSize(50)
            .pageIndex(0)
            .empty(false)
            .build();
    when(roleService.listWithPrincipalCount(eq(pageRequest), any(RoleFilter.class), eq(true)))
        .thenReturn(rolePageResponse);

    PageResponse<RoleWithPrincipalCount> result = roleService.listWithPrincipalCount(pageRequest, roleFilter, true);
    assertEquals(1, result.getContent().size());
    for (int i = 0; i < result.getContent().size(); i++) {
      assertEquals("userRole", result.getContent().get(i).getRole().getIdentifier());
      assertEquals(1, (int) result.getContent().get(i).getRoleAssignedToUserCount());
      assertEquals(0, (int) result.getContent().get(i).getRoleAssignedToUserGroupCount());
      assertEquals(0, (int) result.getContent().get(i).getRoleAssignedToServiceAccountCount());
    }
  }
  @Test
  @Owner(developers = ADITYA)
  @Category(UnitTests.class)
  public void testUserGroupAssignedToRoleCount() {
    PageRequest pageRequest = PageRequest.builder().pageIndex(0).pageSize(50).build();
    RoleFilter roleFilter = RoleFilter.builder().scopeIdentifier(randomAlphabetic(10)).build();
    RoleAssignment roleAssignmentToUserGroup = RoleAssignment.builder()
                                                   .identifier(randomAlphabetic(10))
                                                   .scopeIdentifier(randomAlphabetic(10))
                                                   .roleIdentifier(randomAlphabetic(10))
                                                   .principalType(PrincipalType.USER_GROUP)
                                                   .build();
    PageResponse<RoleAssignment> roleAssignmentPageResponse =
        PageResponse.<RoleAssignment>builder()
            .content(Collections.singletonList(roleAssignmentToUserGroup))
            .totalPages(1)
            .totalItems(1)
            .pageItemCount(1)
            .pageSize(50)
            .pageIndex(0)
            .empty(false)
            .build();
    when(roleAssignmentService.list(any(), any(RoleAssignmentFilter.class), eq(true)))
        .thenReturn(roleAssignmentPageResponse);

    Role role = Role.builder().identifier("userGroupRole").name(randomAlphabetic(10)).build();
    RoleWithPrincipalCount roleWithPrincipalCount = RoleWithPrincipalCount.builder()
                                                        .role(role)
                                                        .roleAssignedToUserCount(0)
                                                        .roleAssignedToUserGroupCount(1)
                                                        .roleAssignedToServiceAccountCount(0)
                                                        .build();

    PageResponse<RoleWithPrincipalCount> rolePageResponse =
        PageResponse.<RoleWithPrincipalCount>builder()
            .content(Collections.singletonList(roleWithPrincipalCount))
            .totalPages(1)
            .totalItems(1)
            .pageItemCount(1)
            .pageSize(50)
            .pageIndex(0)
            .empty(false)
            .build();
    when(roleService.listWithPrincipalCount(eq(pageRequest), any(RoleFilter.class), eq(true)))
        .thenReturn(rolePageResponse);

    PageResponse<RoleWithPrincipalCount> result = roleService.listWithPrincipalCount(pageRequest, roleFilter, true);
    assertEquals(1, result.getContent().size());
    for (int i = 0; i < result.getContent().size(); i++) {
      assertEquals("userGroupRole", result.getContent().get(i).getRole().getIdentifier());
      assertEquals(0, (int) result.getContent().get(i).getRoleAssignedToUserCount());
      assertEquals(1, (int) result.getContent().get(i).getRoleAssignedToUserGroupCount());
      assertEquals(0, (int) result.getContent().get(i).getRoleAssignedToServiceAccountCount());
    }
  }
  @Test
  @Owner(developers = ADITYA)
  @Category(UnitTests.class)
  public void testServiceAccountAssignedToRoleCount() {
    PageRequest pageRequest = PageRequest.builder().pageIndex(0).pageSize(50).build();
    RoleFilter roleFilter = RoleFilter.builder().scopeIdentifier(randomAlphabetic(10)).build();
    RoleAssignment roleAssignmentToServiceAccount = RoleAssignment.builder()
                                                        .identifier(randomAlphabetic(10))
                                                        .scopeIdentifier(randomAlphabetic(10))
                                                        .roleIdentifier(randomAlphabetic(10))
                                                        .principalType(PrincipalType.SERVICE_ACCOUNT)
                                                        .build();
    PageResponse<RoleAssignment> roleAssignmentPageResponse =
        PageResponse.<RoleAssignment>builder()
            .content(Collections.singletonList(roleAssignmentToServiceAccount))
            .totalPages(1)
            .totalItems(1)
            .pageItemCount(1)
            .pageSize(50)
            .pageIndex(0)
            .empty(false)
            .build();
    when(roleAssignmentService.list(any(), any(RoleAssignmentFilter.class), eq(true)))
        .thenReturn(roleAssignmentPageResponse);

    Role role = Role.builder().identifier("serviceAccountRole").name(randomAlphabetic(10)).build();
    RoleWithPrincipalCount roleWithPrincipalCount = RoleWithPrincipalCount.builder()
                                                        .role(role)
                                                        .roleAssignedToUserCount(0)
                                                        .roleAssignedToUserGroupCount(0)
                                                        .roleAssignedToServiceAccountCount(1)
                                                        .build();

    PageResponse<RoleWithPrincipalCount> rolePageResponse =
        PageResponse.<RoleWithPrincipalCount>builder()
            .content(Collections.singletonList(roleWithPrincipalCount))
            .totalPages(1)
            .totalItems(1)
            .pageItemCount(1)
            .pageSize(50)
            .pageIndex(0)
            .empty(false)
            .build();
    when(roleService.listWithPrincipalCount(eq(pageRequest), any(RoleFilter.class), eq(true)))
        .thenReturn(rolePageResponse);

    PageResponse<RoleWithPrincipalCount> result = roleService.listWithPrincipalCount(pageRequest, roleFilter, true);
    assertEquals(1, result.getContent().size());
    for (int i = 0; i < result.getContent().size(); i++) {
      assertEquals("serviceAccountRole", result.getContent().get(i).getRole().getIdentifier());
      assertEquals(0, (int) result.getContent().get(i).getRoleAssignedToUserCount());
      assertEquals(0, (int) result.getContent().get(i).getRoleAssignedToUserGroupCount());
      assertEquals(1, (int) result.getContent().get(i).getRoleAssignedToServiceAccountCount());
    }
  }
  @Test
  @Owner(developers = ADITYA)
  @Category(UnitTests.class)
  public void testNoRoleAssigned() {
    PageRequest pageRequest = PageRequest.builder().pageIndex(0).pageSize(50).build();
    RoleFilter roleFilter = RoleFilter.builder().scopeIdentifier(randomAlphabetic(10)).build();
    RoleAssignment roleAssignmentToUser = RoleAssignment.builder()
                                              .identifier(randomAlphabetic(10))
                                              .scopeIdentifier(randomAlphabetic(10))
                                              .roleIdentifier(randomAlphabetic(10))
                                              .build();
    PageResponse<RoleAssignment> roleAssignmentPageResponse =
        PageResponse.<RoleAssignment>builder()
            .content(Collections.singletonList(roleAssignmentToUser))
            .totalPages(1)
            .totalItems(1)
            .pageItemCount(1)
            .pageSize(50)
            .pageIndex(0)
            .empty(false)
            .pageToken(null)
            .build();
    when(roleAssignmentService.list(any(), any(RoleAssignmentFilter.class), eq(true)))
        .thenReturn(roleAssignmentPageResponse);

    Role role = Role.builder().identifier(randomAlphabetic(10)).name(randomAlphabetic(10)).build();
    RoleWithPrincipalCount roleWithPrincipalCount = RoleWithPrincipalCount.builder()
                                                        .role(role)
                                                        .roleAssignedToUserCount(0)
                                                        .roleAssignedToUserGroupCount(0)
                                                        .roleAssignedToServiceAccountCount(0)
                                                        .build();

    PageResponse<RoleWithPrincipalCount> rolePageResponse =
        PageResponse.<RoleWithPrincipalCount>builder()
            .content(Collections.singletonList(roleWithPrincipalCount))
            .totalPages(1)
            .totalItems(1)
            .pageItemCount(1)
            .pageSize(50)
            .pageIndex(0)
            .empty(false)
            .pageToken(null)
            .build();
    when(roleService.listWithPrincipalCount(eq(pageRequest), any(RoleFilter.class), eq(true)))
        .thenReturn(rolePageResponse);

    PageResponse<RoleWithPrincipalCount> result = roleService.listWithPrincipalCount(pageRequest, roleFilter, true);

    assertEquals(1, result.getContent().size());
    for (int i = 0; i < result.getContent().size(); i++) {
      assertEquals(0, (int) result.getContent().get(i).getRoleAssignedToUserCount());
      assertEquals(0, (int) result.getContent().get(i).getRoleAssignedToUserGroupCount());
      assertEquals(0, (int) result.getContent().get(i).getRoleAssignedToServiceAccountCount());
    }
  }
  @Test
  @Owner(developers = ADITYA)
  @Category(UnitTests.class)
  public void testAllTypesOfRoleAssigned() {
    PageRequest pageRequest = PageRequest.builder().pageIndex(0).pageSize(50).build();
    RoleFilter roleFilter = RoleFilter.builder().scopeIdentifier(randomAlphabetic(10)).build();
    RoleAssignment roleAssignmentToUser = RoleAssignment.builder()
                                              .identifier(randomAlphabetic(10))
                                              .scopeIdentifier(randomAlphabetic(10))
                                              .roleIdentifier(randomAlphabetic(10))
                                              .principalType(PrincipalType.USER)
                                              .build();
    RoleAssignment roleAssignmentToUserGroup = RoleAssignment.builder()
                                                   .identifier(randomAlphabetic(10))
                                                   .scopeIdentifier(randomAlphabetic(10))
                                                   .roleIdentifier(randomAlphabetic(10))
                                                   .principalType(PrincipalType.USER)
                                                   .build();
    RoleAssignment roleAssignmentToServiceAccount = RoleAssignment.builder()
                                                        .identifier(randomAlphabetic(10))
                                                        .scopeIdentifier(randomAlphabetic(10))
                                                        .roleIdentifier(randomAlphabetic(10))
                                                        .principalType(PrincipalType.USER)
                                                        .build();
    PageResponse<RoleAssignment> roleAssignmentPageResponse =
        PageResponse.<RoleAssignment>builder()
            .content(Arrays.asList(roleAssignmentToUser, roleAssignmentToUserGroup, roleAssignmentToServiceAccount))
            .totalPages(1)
            .totalItems(1)
            .pageItemCount(1)
            .pageSize(50)
            .pageIndex(0)
            .empty(false)
            .pageToken(null)
            .build();
    when(roleAssignmentService.list(any(), any(RoleAssignmentFilter.class), eq(true)))
        .thenReturn(roleAssignmentPageResponse);

    Role role = Role.builder().identifier("allThreeAssignment").name(randomAlphabetic(10)).build();
    RoleWithPrincipalCount roleWithPrincipalCount = RoleWithPrincipalCount.builder()
                                                        .role(role)
                                                        .roleAssignedToUserCount(1)
                                                        .roleAssignedToUserGroupCount(1)
                                                        .roleAssignedToServiceAccountCount(1)
                                                        .build();

    PageResponse<RoleWithPrincipalCount> rolePageResponse =
        PageResponse.<RoleWithPrincipalCount>builder()
            .content(Collections.singletonList(roleWithPrincipalCount))
            .totalPages(1)
            .totalItems(1)
            .pageItemCount(1)
            .pageSize(50)
            .pageIndex(0)
            .empty(false)
            .pageToken(null)
            .build();
    when(roleService.listWithPrincipalCount(eq(pageRequest), any(RoleFilter.class), eq(true)))
        .thenReturn(rolePageResponse);

    PageResponse<RoleWithPrincipalCount> result = roleService.listWithPrincipalCount(pageRequest, roleFilter, true);

    assertEquals(1, result.getContent().size());
    for (int i = 0; i < result.getContent().size(); i++) {
      assertEquals("allThreeAssignment", result.getContent().get(i).getRole().getIdentifier());
      assertEquals(1, (int) result.getContent().get(i).getRoleAssignedToUserCount());
      assertEquals(1, (int) result.getContent().get(i).getRoleAssignedToUserGroupCount());
      assertEquals(1, (int) result.getContent().get(i).getRoleAssignedToServiceAccountCount());
    }
  }
}
