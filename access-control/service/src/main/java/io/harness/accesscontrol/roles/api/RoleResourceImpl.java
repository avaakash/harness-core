/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.roles.api;

import static io.harness.accesscontrol.AccessControlPermissions.DELETE_ROLE_PERMISSION;
import static io.harness.accesscontrol.AccessControlPermissions.EDIT_ROLE_PERMISSION;
import static io.harness.accesscontrol.AccessControlPermissions.VIEW_ROLE_PERMISSION;
import static io.harness.accesscontrol.AccessControlResourceTypes.ROLE;
import static io.harness.accesscontrol.common.filter.ManagedFilter.NO_FILTER;
import static io.harness.accesscontrol.common.filter.ManagedFilter.ONLY_CUSTOM;
import static io.harness.accesscontrol.roles.api.RoleDTOMapper.fromDTO;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.springdata.PersistenceUtils.DEFAULT_RETRY_POLICY;

import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.acl.api.Resource;
import io.harness.accesscontrol.acl.api.ResourceScope;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.common.filter.ManagedFilter;
import io.harness.accesscontrol.roles.Role;
import io.harness.accesscontrol.roles.RoleService;
import io.harness.accesscontrol.roles.RoleWithPrincipalCount;
import io.harness.accesscontrol.roles.api.RoleDTO.ScopeLevel;
import io.harness.accesscontrol.roles.filter.RoleFilter;
import io.harness.accesscontrol.roles.filter.RoleFilter.RoleFilterBuilder;
import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.accesscontrol.scopes.harness.HarnessScopeParams;
import io.harness.accesscontrol.scopes.harness.ScopeMapper;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.enforcement.client.annotation.FeatureRestrictionCheck;
import io.harness.enforcement.constants.FeatureRestrictionName;
import io.harness.exception.InvalidRequestException;
import io.harness.ff.FeatureFlagService;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ResponseDTO;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import javax.validation.executable.ValidateOnExecution;
import javax.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.RetryPolicy;
import retrofit2.http.Body;

@ValidateOnExecution
@Singleton
@Slf4j
@OwnedBy(HarnessTeam.PL)
public class RoleResourceImpl implements RoleResource {
  private final RoleService roleService;
  private final ScopeService scopeService;
  private final RoleDTOMapper roleDTOMapper;
  private RoleWithPrincipalCountDTOMapper roleWithPrincipalCountDTOMapper;
  private final AccessControlClient accessControlClient;
  private final FeatureFlagService featureFlagService;

  private final RetryPolicy<Object> transactionRetryPolicy = DEFAULT_RETRY_POLICY;

  @Inject
  public RoleResourceImpl(RoleService roleService, ScopeService scopeService, RoleDTOMapper roleDTOMapper,
      AccessControlClient accessControlClient, FeatureFlagService featureFlagService,
      RoleWithPrincipalCountDTOMapper roleWithPrincipalCountDTOMapper) {
    this.roleService = roleService;
    this.scopeService = scopeService;
    this.roleDTOMapper = roleDTOMapper;
    this.accessControlClient = accessControlClient;
    this.featureFlagService = featureFlagService;
    this.roleWithPrincipalCountDTOMapper = roleWithPrincipalCountDTOMapper;
  }

  @Override
  public ResponseDTO<PageResponse<RoleWithPrincipalCountResponseDTO>> get(
      PageRequest pageRequest, HarnessScopeParams harnessScopeParams, String searchTerm) {
    accessControlClient.checkForAccessOrThrow(
        ResourceScope.of(harnessScopeParams.getAccountIdentifier(), harnessScopeParams.getOrgIdentifier(),
            harnessScopeParams.getProjectIdentifier()),
        Resource.of(ROLE, null), VIEW_ROLE_PERMISSION);
    String scopeIdentifier = ScopeMapper.fromParams(harnessScopeParams).toString();

    RoleFilterBuilder roleFilterBuilder = RoleFilter.builder().searchTerm(searchTerm).scopeIdentifier(scopeIdentifier);

    if (isNotEmpty(harnessScopeParams.getProjectIdentifier())
        && featureFlagService.isEnabled(
            FeatureName.PL_HIDE_PROJECT_LEVEL_MANAGED_ROLE, harnessScopeParams.getAccountIdentifier())) {
      roleFilterBuilder.managedFilter(ONLY_CUSTOM);
    } else if (isNotEmpty(harnessScopeParams.getOrgIdentifier())
        && featureFlagService.isEnabled(
            FeatureName.PL_HIDE_ORGANIZATION_LEVEL_MANAGED_ROLE, harnessScopeParams.getAccountIdentifier())) {
      roleFilterBuilder.managedFilter(ONLY_CUSTOM);
    } else {
      roleFilterBuilder.managedFilter(NO_FILTER);
    }

    PageResponse<RoleWithPrincipalCount> pageResponse =
        roleService.listWithPrincipalCount(pageRequest, roleFilterBuilder.build(), true);
    return ResponseDTO.newResponse(pageResponse.map(roleWithPrincipalCountDTOMapper::toResponseDTO));
  }

  @Override
  public ResponseDTO<RoleResponseDTO> get(String identifier, HarnessScopeParams harnessScopeParams) {
    accessControlClient.checkForAccessOrThrow(
        ResourceScope.of(harnessScopeParams.getAccountIdentifier(), harnessScopeParams.getOrgIdentifier(),
            harnessScopeParams.getProjectIdentifier()),
        Resource.of(ROLE, identifier), VIEW_ROLE_PERMISSION);
    String scopeIdentifier = ScopeMapper.fromParams(harnessScopeParams).toString();
    ManagedFilter managedFilter = NO_FILTER;
    if (isNotEmpty(harnessScopeParams.getProjectIdentifier())
        && featureFlagService.isEnabled(
            FeatureName.PL_HIDE_PROJECT_LEVEL_MANAGED_ROLE, harnessScopeParams.getAccountIdentifier())) {
      managedFilter = ONLY_CUSTOM;
    } else if (isNotEmpty(harnessScopeParams.getOrgIdentifier())
        && featureFlagService.isEnabled(
            FeatureName.PL_HIDE_ORGANIZATION_LEVEL_MANAGED_ROLE, harnessScopeParams.getAccountIdentifier())) {
      managedFilter = ONLY_CUSTOM;
    }

    return ResponseDTO.newResponse(roleDTOMapper.toResponseDTO(
        roleService.get(identifier, scopeIdentifier, managedFilter).<InvalidRequestException>orElseThrow(() -> {
          throw new NotFoundException(
              String.format("Role with identifier [%s] is not found in the given scope", identifier));
        })));
  }

  @Override
  public ResponseDTO<RoleResponseDTO> update(
      String identifier, HarnessScopeParams harnessScopeParams, RoleDTO roleDTO) {
    accessControlClient.checkForAccessOrThrow(
        ResourceScope.of(harnessScopeParams.getAccountIdentifier(), harnessScopeParams.getOrgIdentifier(),
            harnessScopeParams.getProjectIdentifier()),
        Resource.of(ROLE, identifier), EDIT_ROLE_PERMISSION);
    String scopeIdentifier = ScopeMapper.fromParams(harnessScopeParams).toString();
    if (!identifier.equals(roleDTO.getIdentifier())) {
      throw new InvalidRequestException("Role identifier in the request body and the url do not match");
    }
    Role updatedRole = roleService.update(fromDTO(scopeIdentifier, roleDTO));
    RoleResponseDTO response = roleDTOMapper.toResponseDTO(updatedRole);
    return ResponseDTO.newResponse(response);
  }

  @Override
  @FeatureRestrictionCheck(FeatureRestrictionName.CUSTOM_ROLES)
  public ResponseDTO<RoleResponseDTO> create(@AccountIdentifier String accountIdentifier, String orgIdentifier,
      String projectIdentifier, @RequestBody(description = "Role entity", required = true) @Body RoleDTO roleDTO) {
    HarnessScopeParams harnessScopeParams = HarnessScopeParams.builder()
                                                .accountIdentifier(accountIdentifier)
                                                .orgIdentifier(orgIdentifier)
                                                .projectIdentifier(projectIdentifier)
                                                .build();
    accessControlClient.checkForAccessOrThrow(
        ResourceScope.of(harnessScopeParams.getAccountIdentifier(), harnessScopeParams.getOrgIdentifier(),
            harnessScopeParams.getProjectIdentifier()),
        Resource.of(ROLE, null), EDIT_ROLE_PERMISSION);
    Scope scope = scopeService.getOrCreate(ScopeMapper.fromParams(harnessScopeParams));
    if (isEmpty(roleDTO.getAllowedScopeLevels())) {
      roleDTO.setAllowedScopeLevels(Sets.newHashSet(ScopeLevel.fromString(scope.getLevel().toString())));
    }
    RoleResponseDTO response = roleDTOMapper.toResponseDTO(roleService.create(fromDTO(scope.toString(), roleDTO)));
    return ResponseDTO.newResponse(response);
  }

  @Override
  public ResponseDTO<RoleResponseDTO> delete(String identifier, HarnessScopeParams harnessScopeParams) {
    accessControlClient.checkForAccessOrThrow(
        ResourceScope.of(harnessScopeParams.getAccountIdentifier(), harnessScopeParams.getOrgIdentifier(),
            harnessScopeParams.getProjectIdentifier()),
        Resource.of(ROLE, identifier), DELETE_ROLE_PERMISSION);
    String scopeIdentifier = ScopeMapper.fromParams(harnessScopeParams).toString();
    RoleResponseDTO response = roleDTOMapper.toResponseDTO(roleService.delete(identifier, scopeIdentifier));
    return ResponseDTO.newResponse(response);
  }
}
