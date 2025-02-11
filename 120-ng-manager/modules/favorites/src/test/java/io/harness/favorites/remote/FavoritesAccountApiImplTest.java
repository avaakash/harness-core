/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.favorites.remote;

import static io.harness.rule.OwnerRule.BOOPESH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.eraro.ResponseMessage;
import io.harness.exception.InvalidRequestException;
import io.harness.favorites.ResourceType;
import io.harness.favorites.entities.Favorite;
import io.harness.favorites.services.FavoritesService;
import io.harness.favorites.utils.FavoritesResourceUtils;
import io.harness.rule.Owner;
import io.harness.spec.server.ng.v1.model.FavoriteDTO;
import io.harness.spec.server.ng.v1.model.FavoriteResponse;
import io.harness.spec.server.ng.v1.model.FavoritesResourceType;

import java.util.Collections;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.PL)
public class FavoritesAccountApiImplTest extends CategoryTest {
  private AccountFavoritesApiImpl accountFavoriteApi;
  private FavoritesResourceUtils favoritesResourceUtils;
  @Mock private FavoritesService favoriteService;
  private final String userId = "userId";
  private final String accountId = "accountId";
  private final String resourceType_connector = "CONNECTOR";
  private final String resourceId = "resourceUUID";

  private FavoriteDTO favoriteDTO = new FavoriteDTO();

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    favoritesResourceUtils = new FavoritesResourceUtils();
    accountFavoriteApi = new AccountFavoritesApiImpl(favoriteService, favoritesResourceUtils);
    favoriteDTO = favoriteDTO.module(io.harness.spec.server.ng.v1.model.ModuleType.CD)
                      .userId(userId)
                      .resourceType(FavoritesResourceType.fromValue(resourceType_connector))
                      .resourceId(resourceId);
  }

  @Test
  @Owner(developers = BOOPESH)
  @Category(UnitTests.class)
  public void testCreateAccountScopedFavorite() {
    Favorite favoriteEntity = getFavoriteEntity();
    when(favoriteService.createFavorite(any(), anyString())).thenReturn(favoriteEntity);
    Response accountFavoriteResponse = accountFavoriteApi.createAccountFavorite(favoriteDTO, accountId);
    assertThat(accountFavoriteResponse).isNotNull();
    assertThat(accountFavoriteResponse.getStatus()).isEqualTo(201);
    assertThat(accountFavoriteResponse.getEntity()).isEqualTo(getFavoriteResponse(favoriteEntity));
  }

  @Test
  @Owner(developers = BOOPESH)
  @Category(UnitTests.class)
  public void testGetAccountScopedFavoriteWithResourceType() {
    Favorite favoriteEntity = getFavoriteEntity();
    when(favoriteService.getFavorites(anyString(), any(), any(), anyString(), any()))
        .thenReturn(Collections.singletonList(favoriteEntity));
    Response accountFavoriteResponse =
        accountFavoriteApi.getAccountFavorites(userId, accountId, resourceType_connector);
    assertThat(accountFavoriteResponse).isNotNull();
    assertThat(accountFavoriteResponse.getStatus()).isEqualTo(200);
    assertThat(accountFavoriteResponse.getEntity())
        .isEqualTo(Collections.singletonList(getFavoriteResponse(favoriteEntity)));
  }

  @Test
  @Owner(developers = BOOPESH)
  @Category(UnitTests.class)
  public void testGetAllAccountScopedFavoriteOfUser() {
    Favorite favoriteEntity = getFavoriteEntity();
    when(favoriteService.getFavorites(anyString(), any(), any(), anyString()))
        .thenReturn(Collections.singletonList(favoriteEntity));
    Response accountFavoriteResponse = accountFavoriteApi.getAccountFavorites(userId, accountId, null);
    assertThat(accountFavoriteResponse).isNotNull();
    assertThat(accountFavoriteResponse.getStatus()).isEqualTo(200);
    assertThat(accountFavoriteResponse.getEntity())
        .isEqualTo(Collections.singletonList(getFavoriteResponse(favoriteEntity)));
  }

  @Test
  @Owner(developers = BOOPESH)
  @Category(UnitTests.class)
  public void testDeleteAccountScopedFavorite() {
    Response deleteAccountFavorite =
        accountFavoriteApi.deleteAccountFavorite(userId, accountId, resourceType_connector, resourceId);
    assertThat(deleteAccountFavorite).isNotNull();
    assertThat(deleteAccountFavorite.getStatus()).isEqualTo(204);
    assertThat(deleteAccountFavorite.getEntity()).isNull();
  }

  @Test
  @Owner(developers = BOOPESH)
  @Category(UnitTests.class)
  public void testDeleteAccountScopedFavoriteInvalidResourceTypeThrowException() {
    doThrow(new InvalidRequestException("Please provide a valid resource Type"))
        .when(favoriteService)
        .deleteFavorite(accountId, null, null, userId, resourceType_connector, resourceId);
    Response response = accountFavoriteApi.deleteAccountFavorite(userId, accountId, resourceType_connector, resourceId);
    ResponseMessage errorResponse = ResponseMessage.builder()
                                        .code(ErrorCode.INVALID_REQUEST)
                                        .level(Level.ERROR)
                                        .message("Please provide a valid resource Type")
                                        .build();
    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(response.getEntity()).isEqualTo(errorResponse);
  }

  @Test
  @Owner(developers = BOOPESH)
  @Category(UnitTests.class)
  public void testDeleteAccountScopedFavoriteNullResourceTypeThrowException() {
    doThrow(new InvalidRequestException("Please provide a valid resource Type"))
        .when(favoriteService)
        .deleteFavorite(accountId, null, null, userId, "Random", resourceId);
    Response response = accountFavoriteApi.deleteAccountFavorite(userId, accountId, "Random", resourceId);
    ResponseMessage errorResponse = ResponseMessage.builder()
                                        .code(ErrorCode.INVALID_REQUEST)
                                        .level(Level.ERROR)
                                        .message("Please provide a valid resource Type")
                                        .build();
    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(response.getEntity()).isEqualTo(errorResponse);
  }

  private Favorite getFavoriteEntity() {
    return Favorite.builder()
        .accountIdentifier(accountId)
        .resourceIdentifier(resourceId)
        .resourceType(ResourceType.CONNECTOR)
        .module(ModuleType.CD)
        .userIdentifier(userId)
        .build();
  }

  private FavoriteResponse getFavoriteResponse(Favorite favoriteEntity) {
    return favoritesResourceUtils.toFavoriteResponse(favoriteEntity);
  }
}
