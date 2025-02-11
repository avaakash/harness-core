/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.views.dao;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.persistence.HQuery.excludeValidate;

import io.harness.ccm.views.entities.CEView;
import io.harness.ccm.views.entities.CEView.CEViewKeys;
import io.harness.ccm.views.entities.CloudFilter;
import io.harness.ccm.views.entities.ViewPreferences;
import io.harness.ccm.views.entities.ViewState;
import io.harness.ccm.views.entities.ViewType;
import io.harness.ccm.views.graphql.QLCESortOrder;
import io.harness.ccm.views.graphql.QLCEViewSortCriteria;
import io.harness.exception.InvalidRequestException;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.BasicDBObject;
import dev.morphia.query.Query;
import dev.morphia.query.Sort;
import dev.morphia.query.UpdateOperations;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class CEViewDao {
  private static final String VIEW_VISUALIZATION_GROUP_BY_FIELD_ID = "viewVisualization.groupBy.fieldId";
  private static final String VIEW_VISUALIZATION_GROUP_BY_FIELD_NAME = "viewVisualization.groupBy.fieldName";
  private static final String VIEW_VISUALIZATION_GROUP_BY_IDENTIFIER = "viewVisualization.groupBy.identifier";
  private static final String VIEW_RULES_VIEW_CONDITIONS_VIEW_FIELD_FIELD_ID =
      "viewRules.viewConditions.viewField.fieldId";
  private static final String VIEW_RULES_VIEW_CONDITIONS_VIEW_FIELD_FIELD_NAME =
      "viewRules.$[].viewConditions.$[].viewField.fieldName";
  private static final String VIEW_RULES_VIEW_CONDITIONS_VIEW_FIELD_IDENTIFIER =
      "viewRules.viewConditions.viewField.identifier";
  private static final String BUSINESS_MAPPING = "BUSINESS_MAPPING";
  private static final String INSENSITIVE_SEARCH = "i";
  private static final String OPTIONS = "$options";
  private static final String REGEX = "$regex";

  @Inject private HPersistence hPersistence;

  public boolean save(CEView ceView) {
    return hPersistence.save(ceView) != null;
  }

  public void updateBusinessMappingName(String accountId, String buinessMappingUuid, String newBusinessMappingName) {
    // Checking & update on the groupBy
    Query queryGroupBy = hPersistence.createQuery(CEView.class)
                             .disableValidation()
                             .field(CEViewKeys.accountId)
                             .equal(accountId)
                             .field(VIEW_VISUALIZATION_GROUP_BY_IDENTIFIER)
                             .equal(BUSINESS_MAPPING)
                             .field(VIEW_VISUALIZATION_GROUP_BY_FIELD_ID)
                             .equal(buinessMappingUuid);

    UpdateOperations<CEView> updateOperations =
        hPersistence.createUpdateOperations(CEView.class)
            .disableValidation()
            .set(VIEW_VISUALIZATION_GROUP_BY_FIELD_NAME, newBusinessMappingName);
    hPersistence.update(queryGroupBy, updateOperations);

    // Checking & update on the viewRules
    Query queryViewRules = hPersistence.createQuery(CEView.class)
                               .disableValidation()
                               .field(CEViewKeys.accountId)
                               .equal(accountId)
                               .field(VIEW_RULES_VIEW_CONDITIONS_VIEW_FIELD_IDENTIFIER)
                               .equal(BUSINESS_MAPPING)
                               .field(VIEW_RULES_VIEW_CONDITIONS_VIEW_FIELD_FIELD_ID)
                               .equal(buinessMappingUuid);

    updateOperations = hPersistence.createUpdateOperations(CEView.class)
                           .disableValidation()
                           .set(VIEW_RULES_VIEW_CONDITIONS_VIEW_FIELD_FIELD_NAME, newBusinessMappingName);
    hPersistence.update(queryViewRules, updateOperations);
  }

  public CEView update(CEView ceView) {
    Query query = hPersistence.createQuery(CEView.class)
                      .field(CEViewKeys.accountId)
                      .equal(ceView.getAccountId())
                      .field(CEViewKeys.uuid)
                      .equal(ceView.getUuid());
    UpdateOperations<CEView> updateOperations = hPersistence.createUpdateOperations(CEView.class)
                                                    .set(CEViewKeys.viewVersion, ceView.getViewVersion())
                                                    .set(CEViewKeys.name, ceView.getName())
                                                    .set(CEViewKeys.viewTimeRange, ceView.getViewTimeRange())
                                                    .set(CEViewKeys.viewRules, ceView.getViewRules())
                                                    .set(CEViewKeys.viewVisualization, ceView.getViewVisualization())
                                                    .set(CEViewKeys.viewPreferences, ceView.getViewPreferences())
                                                    .set(CEViewKeys.viewType, ceView.getViewType())
                                                    .set(CEViewKeys.viewState, ViewState.COMPLETED)
                                                    .set(CEViewKeys.dataSources, ceView.getDataSources());
    if (ceView.getFolderId() != null) {
      updateOperations = updateOperations.set(CEViewKeys.folderId, ceView.getFolderId());
    }
    hPersistence.update(query, updateOperations);
    return (CEView) query.asList().get(0);
  }

  public CEView updateTotalCost(String viewId, String accountId, double totalCost) {
    Query query = hPersistence.createQuery(CEView.class)
                      .field(CEViewKeys.accountId)
                      .equal(accountId)
                      .field(CEViewKeys.uuid)
                      .equal(viewId);

    UpdateOperations<CEView> updateOperations =
        hPersistence.createUpdateOperations(CEView.class).set(CEViewKeys.totalCost, totalCost);
    hPersistence.update(query, updateOperations);
    return (CEView) query.asList().get(0);
  }

  public void updateViewPreferences(String viewId, String accountId, ViewPreferences viewPreferences) {
    Query<CEView> query = hPersistence.createQuery(CEView.class)
                              .field(CEViewKeys.accountId)
                              .equal(accountId)
                              .field(CEViewKeys.uuid)
                              .equal(viewId);
    UpdateOperations<CEView> updateOperations =
        hPersistence.createUpdateOperations(CEView.class).set(CEViewKeys.viewPreferences, viewPreferences);
    hPersistence.update(query, updateOperations);
  }

  public boolean delete(String uuid, String accountId) {
    Query query = hPersistence.createQuery(CEView.class)
                      .field(CEViewKeys.accountId)
                      .equal(accountId)
                      .field(CEViewKeys.uuid)
                      .equal(uuid);
    return hPersistence.delete(query);
  }

  public CEView get(String uuid) {
    return hPersistence.createQuery(CEView.class, excludeValidate).filter(CEViewKeys.uuid, uuid).get();
  }

  public List<CEView> list(String accountId, List<String> perspectiveIds) {
    return hPersistence.createQuery(CEView.class)
        .field(CEViewKeys.accountId)
        .equal(accountId)
        .field(CEViewKeys.uuid)
        .in(perspectiveIds)
        .asList();
  }

  public List<CEView> list(String accountId) {
    return hPersistence.createQuery(CEView.class).field(CEViewKeys.accountId).equal(accountId).asList();
  }

  public CEView findByName(String accountId, String name) {
    return hPersistence.createQuery(CEView.class)
        .filter(CEViewKeys.accountId, accountId)
        .filter(CEViewKeys.name, name)
        .get();
  }

  public List<CEView> findByAccountId(String accountId, QLCEViewSortCriteria sortCriteria) {
    Query<CEView> query = hPersistence.createQuery(CEView.class).filter(CEViewKeys.accountId, accountId);
    query = decorateQueryWithSortCriteria(query, sortCriteria);
    return query.asList();
  }

  public List<CEView> findByAccountIdAndState(String accountId, ViewState viewState) {
    return hPersistence.createQuery(CEView.class)
        .filter(CEViewKeys.accountId, accountId)
        .filter(CEViewKeys.viewState, viewState)
        .asList();
  }

  public List<CEView> findByAccountIdAndBusinessMapping(String accountId, String businessMappingUuid) {
    Query<CEView> query =
        hPersistence.createQuery(CEView.class).disableValidation().field(CEViewKeys.accountId).equal(accountId);
    query.or(query.and(query.criteria(VIEW_VISUALIZATION_GROUP_BY_IDENTIFIER).equal(BUSINESS_MAPPING),
                 query.criteria(VIEW_VISUALIZATION_GROUP_BY_FIELD_ID).equal(businessMappingUuid)),
        query.and(query.criteria(VIEW_RULES_VIEW_CONDITIONS_VIEW_FIELD_IDENTIFIER).equal(BUSINESS_MAPPING),
            query.criteria(VIEW_RULES_VIEW_CONDITIONS_VIEW_FIELD_FIELD_ID).equal(businessMappingUuid)));
    return query.asList();
  }

  public List<CEView> findByAccountIdAndType(String accountId, ViewType viewType) {
    return hPersistence.createQuery(CEView.class)
        .filter(CEViewKeys.accountId, accountId)
        .filter(CEViewKeys.viewType, viewType)
        .asList();
  }

  public Long findCountByAccountIdAndType(String accountId, ViewType viewType) {
    return hPersistence.createQuery(CEView.class)
        .filter(CEViewKeys.accountId, accountId)
        .filter(CEViewKeys.viewType, viewType)
        .count();
  }

  public List<CEView> findByAccountIdAndFolderId(String accountId, String folderId, QLCEViewSortCriteria sortCriteria) {
    Query<CEView> query = hPersistence.createQuery(CEView.class)
                              .filter(CEViewKeys.accountId, accountId)
                              .filter(CEViewKeys.folderId, folderId);
    query = decorateQueryWithSortCriteria(query, sortCriteria);
    return query.asList();
  }

  public List<CEView> moveMultiplePerspectiveFolder(String accountId, List<String> uuids, String toFolderId) {
    Query<CEView> query = hPersistence.createQuery(CEView.class)
                              .field(CEViewKeys.accountId)
                              .equal(accountId)
                              .field(CEViewKeys.uuid)
                              .in(uuids);
    UpdateOperations<CEView> updateOperations =
        hPersistence.createUpdateOperations(CEView.class).set(CEViewKeys.folderId, toFolderId);
    hPersistence.update(query, updateOperations);
    return query.asList();
  }

  private Query<CEView> decorateQueryWithSortCriteria(Query<CEView> query, QLCEViewSortCriteria sortCriteria) {
    if (sortCriteria == null) {
      return query;
    }
    String sortField;
    switch (sortCriteria.getSortType()) {
      case COST:
        sortField = CEViewKeys.totalCost;
        break;
      case TIME:
        sortField = CEViewKeys.createdAt;
        break;
      case NAME:
        sortField = CEViewKeys.name;
        break;
      default:
        throw new InvalidRequestException("Sort type not supported");
    }
    if (sortCriteria.getSortOrder().equals(QLCESortOrder.DESCENDING)) {
      return query.order(Sort.descending(sortField));
    }
    return query.order(Sort.ascending(sortField));
  }

  public List<CEView> getPerspectivesByIds(String accountId, List<String> uuids) {
    return hPersistence.createQuery(CEView.class)
        .field(CEViewKeys.accountId)
        .equal(accountId)
        .field(CEViewKeys.uuid)
        .in(uuids)
        .asList();
  }

  public long count(String accountId) {
    return hPersistence.createQuery(CEView.class).field(CEViewKeys.accountId).equal(accountId).count();
  }

  public Long countByAccountIdAndFolderIds(
      String accountId, Set<String> folderIds, String searchKey, List<CloudFilter> cloudFilters) {
    return getFiltersQuery(accountId, folderIds, searchKey, cloudFilters).count();
  }

  private void decorateQueryWithViewTypeFilter(Query<CEView> query, boolean excludeDefault) {
    if (!excludeDefault) {
      query.filter(CEViewKeys.viewType, ViewType.DEFAULT);
    } else {
      query.field(CEViewKeys.viewType).notEqual(ViewType.DEFAULT);
    }
  }

  private Query<CEView> getFiltersQuery(
      String accountId, Set<String> folderIds, String searchKey, List<CloudFilter> cloudFilters) {
    Query<CEView> query = hPersistence.createQuery(CEView.class)
                              .field(CEViewKeys.accountId)
                              .equal(accountId)
                              .field(CEViewKeys.folderId)
                              .in(Objects.isNull(folderIds) ? new HashSet<String>() : folderIds);

    if (!isEmpty(searchKey)) {
      BasicDBObject basicDBObject = new BasicDBObject(REGEX, searchKey);
      basicDBObject.put(OPTIONS, INSENSITIVE_SEARCH);
      query.filter(CEViewKeys.name, basicDBObject);
    }

    if (!isEmpty(cloudFilters)) {
      if (cloudFilters.contains(CloudFilter.DEFAULT) && cloudFilters.size() == 1) {
        query.filter(CEViewKeys.viewType, ViewType.DEFAULT);
      } else if (cloudFilters.contains(CloudFilter.DEFAULT)) {
        List<CloudFilter> tempCloudFilters = new ArrayList<>(cloudFilters);
        tempCloudFilters.remove(CloudFilter.DEFAULT);
        query.or(query.criteria(CEViewKeys.viewType).equal(ViewType.DEFAULT),
            query.criteria(CEViewKeys.dataSources).in(tempCloudFilters));
      } else {
        query.field(CEViewKeys.dataSources).in(cloudFilters);
      }
    }
    return query;
  }

  public List<CEView> findByAccountIdAndFolderIds(String accountId, Set<String> folderIds,
      QLCEViewSortCriteria sortCriteria, int limit, int offset, String searchKey, List<CloudFilter> filters,
      boolean excludeDefault) {
    Query<CEView> query = getFiltersQuery(accountId, folderIds, searchKey, filters);
    decorateQueryWithViewTypeFilter(query, excludeDefault);
    query = decorateQueryWithSortCriteria(query, sortCriteria);
    query.limit(limit);
    query.offset(offset);
    return query.asList();
  }

  public List<CEView> findByAccountIdAndFolderIds(String accountId, Set<String> folderIds,
      QLCEViewSortCriteria sortCriteria, Integer limit, Integer offset, String searchKey, List<CloudFilter> filters) {
    Query<CEView> query = getFiltersQuery(accountId, folderIds, searchKey, filters);
    query = decorateQueryWithSortCriteria(query, sortCriteria);
    query.limit(limit);
    query.offset(offset);
    return query.asList();
  }

  public boolean deleteAllForAccount(String accountId) {
    Query<CEView> query = hPersistence.createQuery(CEView.class).field(CEViewKeys.accountId).equal(accountId);
    return hPersistence.delete(query);
  }
}
