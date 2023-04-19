/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.proxy.delegate;

import static io.harness.annotations.dev.HarnessTeam.CI;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTaskRequest;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.task.http.HttpStepResponse;
import io.harness.delegate.task.http.HttpTaskParametersNg;
import io.harness.http.HttpHeaderConfig;
import io.harness.service.DelegateGrpcClientWrapper;
import io.harness.waiter.WaitNotifyEngine;

import software.wings.beans.TaskType;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(CI)
public class DelegateProxyRequestForwarder {
  private static final long EXECUTION_TIMEOUT_IN_SECONDS = 60;
  private static final int SOCKET_TIMEOUT_IN_MILLISECONDS = 20000;
  DelegateGrpcClientWrapper delegateGrpcClientWrapper;
  WaitNotifyEngine waitNotifyEngine;

  public UriBuilder createUrlWithQueryParameters(String url, MultivaluedMap<String, String> queryParam) {
    UriBuilder uriBuilder = UriBuilder.fromUri(url);

    for (Map.Entry<String, List<String>> entry : queryParam.entrySet()) {
      uriBuilder.queryParam(entry.getKey(), entry.getValue());
    }
    return uriBuilder;
  }

  public List<HttpHeaderConfig> createHeaderConfig(Map<String, Object> headers) {
    List<HttpHeaderConfig> headerList = new ArrayList<>();
    try {
      for (Map.Entry<String, Object> entry : headers.entrySet()) {
        if (entry.getKey().equalsIgnoreCase("Content-Length") || entry.getKey().equalsIgnoreCase("host")) {
          continue;
        }
        String value = entry.getValue().toString();
        headerList.add(HttpHeaderConfig.builder().key(entry.getKey()).value(value).build());
        log.debug("header {} : {}", entry.getKey(), value);
      }
    } catch (Exception ex) {
      log.error("Error while mapping the headers", ex);
      throw ex;
    }

    return headerList;
  }

  public HttpStepResponse forwardRequestToDelegate(
      String accountId, String url, List<HttpHeaderConfig> headerList, String body, String methodType) {
    DelegateTaskRequest delegateTaskRequest =
        DelegateTaskRequest.builder()
            .accountId(accountId)
            .executionTimeout(java.time.Duration.ofSeconds(EXECUTION_TIMEOUT_IN_SECONDS))
            .taskType(TaskType.HTTP_TASK_NG.name())
            .taskParameters(getTaskParams(url, methodType, headerList, body))
            .taskDescription("IDP Proxy Http Task")
            .taskSetupAbstraction("ng", "true")
            .build();
    HttpStepResponse httpResponse = null;
    try {
      DelegateResponseData responseData = delegateGrpcClientWrapper.executeSyncTaskV2(delegateTaskRequest);
      if (responseData instanceof ErrorNotifyResponseData) {
        ErrorNotifyResponseData errorNotifyResponseData = (ErrorNotifyResponseData) responseData;
        log.error("errorMessage: {}", errorNotifyResponseData.getErrorMessage());
      }
      if (responseData instanceof HttpStepResponse) {
        httpResponse = (HttpStepResponse) responseData;

        log.debug("responseData header: {}", httpResponse.getHeader());
        log.debug("responseData body: {}", httpResponse.getHttpResponseBody());
      }
    } catch (Exception ex) {
      log.error("Delegate error: ", ex);
      throw ex;
    }

    return httpResponse;
  }

  private HttpTaskParametersNg getTaskParams(
      String url, String methodType, List<HttpHeaderConfig> headers, String body) {
    return HttpTaskParametersNg.builder()
        .url(url)
        .method(methodType)
        .requestHeader(headers)
        .body(body)
        .socketTimeoutMillis(SOCKET_TIMEOUT_IN_MILLISECONDS)
        .build();
  }
}