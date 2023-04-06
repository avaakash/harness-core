/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.onboarding.service;

import static io.harness.rule.OwnerRule.SATHISH;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.idp.gitintegration.processor.factory.ConnectorProcessorFactory;
import io.harness.idp.gitintegration.processor.impl.GithubConnectorProcessor;
import io.harness.idp.gitintegration.repositories.CatalogConnectorRepository;
import io.harness.idp.onboarding.client.FakeOrganizationClient;
import io.harness.idp.onboarding.client.FakeProjectClient;
import io.harness.idp.onboarding.client.FakeServiceResourceClient;
import io.harness.idp.onboarding.config.OnboardingModuleConfig;
import io.harness.idp.onboarding.mappers.HarnessOrgToBackstageDomain;
import io.harness.idp.onboarding.mappers.HarnessProjectToBackstageSystem;
import io.harness.idp.onboarding.mappers.HarnessServiceToBackstageComponent;
import io.harness.idp.onboarding.service.impl.OnboardingServiceImpl;
import io.harness.idp.status.service.StatusInfoService;
import io.harness.ng.beans.PageResponse;
import io.harness.rule.Owner;
import io.harness.spec.server.idp.v1.model.CatalogConnectorInfo;
import io.harness.spec.server.idp.v1.model.ConnectorDetails;
import io.harness.spec.server.idp.v1.model.HarnessBackstageEntities;
import io.harness.spec.server.idp.v1.model.HarnessEntitiesCountResponse;
import io.harness.spec.server.idp.v1.model.ImportEntitiesResponse;
import io.harness.spec.server.idp.v1.model.ImportHarnessEntitiesRequest;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(HarnessTeam.IDP)
public class OnboardingServiceImplTest extends CategoryTest {
  static final String ACCOUNT_IDENTIFIER = "123";
  static final String ACCOUNT_IDENTIFIER_DUMMY = "dummy_account_identifier";
  static final String TEST_SERVICE_IDENTIFIER = "serviceId";
  AutoCloseable openMocks;
  @InjectMocks private OnboardingServiceImpl onboardingServiceImpl;
  @InjectMocks HarnessOrgToBackstageDomain harnessOrgToBackstageDomain;
  @InjectMocks HarnessProjectToBackstageSystem harnessProjectToBackstageSystem;
  @Mock ConnectorProcessorFactory connectorProcessorFactory;
  @Mock GithubConnectorProcessor githubConnectorProcessor;
  @Mock CatalogConnectorRepository catalogConnectorRepository;
  @Mock StatusInfoService statusInfoService;
  final OnboardingModuleConfig onboardingModuleConfig =
      OnboardingModuleConfig.builder()
          .tmpPathForCatalogInfoYamlStore("/tmp")
          .harnessCiCdAnnotations(Map.of("projectUrl",
              "https://localhost:8181/ng/account/accountIdentifier/home/orgs/orgIdentifier/projects/projectIdentifier/details"))
          .build();

  @Before
  public void setUp() throws IllegalAccessException, IOException {
    openMocks = MockitoAnnotations.openMocks(this);

    FakeOrganizationClient organizationClient = new FakeOrganizationClient();
    FakeProjectClient projectClient = new FakeProjectClient();
    FakeServiceResourceClient serviceResourceClient = new FakeServiceResourceClient();

    FieldUtils.writeField(onboardingServiceImpl, "organizationClient", organizationClient, true);
    FieldUtils.writeField(onboardingServiceImpl, "projectClient", projectClient, true);
    FieldUtils.writeField(onboardingServiceImpl, "serviceResourceClient", serviceResourceClient, true);

    FieldUtils.writeField(onboardingServiceImpl, "harnessOrgToBackstageDomain", harnessOrgToBackstageDomain, true);
    FieldUtils.writeField(
        onboardingServiceImpl, "harnessProjectToBackstageSystem", harnessProjectToBackstageSystem, true);

    HarnessServiceToBackstageComponent harnessServiceToBackstageComponent =
        new HarnessServiceToBackstageComponent(onboardingModuleConfig);
    FieldUtils.writeField(
        onboardingServiceImpl, "harnessServiceToBackstageComponent", harnessServiceToBackstageComponent, true);

    FieldUtils.writeField(onboardingServiceImpl, "onboardingModuleConfig", onboardingModuleConfig, true);
  }

  @Test
  @Owner(developers = SATHISH)
  @Category(UnitTests.class)
  public void testGetHarnessEntitiesCount() {
    HarnessEntitiesCountResponse harnessEntitiesCountResponse =
        onboardingServiceImpl.getHarnessEntitiesCount(ACCOUNT_IDENTIFIER);
    assertNotNull(harnessEntitiesCountResponse);
    assertEquals(1, (int) harnessEntitiesCountResponse.getOrgCount());
    assertEquals(1, (int) harnessEntitiesCountResponse.getProjectCount());
    assertEquals(1, (int) harnessEntitiesCountResponse.getServiceCount());
  }

  @Test
  @Owner(developers = SATHISH)
  @Category(UnitTests.class)
  public void testGetHarnessEntitiesCountWithInvalidAccountIdentifier() {
    HarnessEntitiesCountResponse harnessEntitiesCountResponse =
        onboardingServiceImpl.getHarnessEntitiesCount(ACCOUNT_IDENTIFIER_DUMMY);
    assertNotNull(harnessEntitiesCountResponse);
    assertEquals(0, (int) harnessEntitiesCountResponse.getOrgCount());
    assertEquals(0, (int) harnessEntitiesCountResponse.getProjectCount());
    assertEquals(0, (int) harnessEntitiesCountResponse.getServiceCount());
  }

  @Test
  @Owner(developers = SATHISH)
  @Category(UnitTests.class)
  public void testGetHarnessEntities() {
    PageResponse<HarnessBackstageEntities> harnessBackstageEntitiesPageResponse =
        onboardingServiceImpl.getHarnessEntities(ACCOUNT_IDENTIFIER, 0, 10, null, null, null, null);
    assertNotNull(harnessBackstageEntitiesPageResponse);
    assertEquals(1, (int) harnessBackstageEntitiesPageResponse.getTotalItems());
    assertEquals(1, (int) harnessBackstageEntitiesPageResponse.getTotalPages());
    assertEquals(1, (int) harnessBackstageEntitiesPageResponse.getPageItemCount());
    assertEquals(10, (int) harnessBackstageEntitiesPageResponse.getPageSize());
    List<HarnessBackstageEntities> harnessBackstageEntities = harnessBackstageEntitiesPageResponse.getContent();
    assertNotNull(harnessBackstageEntities);
    assertEquals(1, harnessBackstageEntities.size());
    assertEquals(TEST_SERVICE_IDENTIFIER, harnessBackstageEntities.get(0).getName());
    assertEquals("projectId", harnessBackstageEntities.get(0).getSystem());
    assertEquals("Unknown", harnessBackstageEntities.get(0).getOwner());
    assertEquals("Service", harnessBackstageEntities.get(0).getType());
  }

  @Test
  @Owner(developers = SATHISH)
  @Category(UnitTests.class)
  public void testImportHarnessEntities() {
    when(connectorProcessorFactory.getConnectorProcessor(ConnectorType.fromString("Github")))
        .thenReturn(githubConnectorProcessor);
    when(githubConnectorProcessor.getInfraConnectorType(any(), any())).thenReturn("DIRECT");
    ImportEntitiesResponse importEntitiesResponse = onboardingServiceImpl.importHarnessEntities(ACCOUNT_IDENTIFIER,
        new ImportHarnessEntitiesRequest()
            .allImport(true)
            .entities(Collections.emptyList())
            .catalogConnectorInfo(
                new CatalogConnectorInfo()
                    .infraConnector(new ConnectorDetails().identifier("account.sathishgithub").type("Github"))
                    .sourceConnector(new ConnectorDetails().identifier("account.sathishgithub").type("Github"))
                    .repo("https://github.com/sathish-soundarapandian/onboarding-test")
                    .branch("main")
                    .path("idp")));
    assertNotNull(importEntitiesResponse);
    assertEquals("SUCCESS", importEntitiesResponse.getStatus());
  }

  @After
  public void tearDown() throws Exception {
    openMocks.close();
  }
}