/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.aws.lambda;

import static software.wings.beans.LogColor.Green;
import static software.wings.beans.LogHelper.color;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.aws.v2.lambda.AwsLambdaCommandUnitConstants;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.exception.AwsLambdaException;
import io.harness.delegate.task.aws.lambda.AwsLambda;
import io.harness.delegate.task.aws.lambda.AwsLambdaTaskHelper;
import io.harness.delegate.task.aws.lambda.request.AwsLambdaCommandRequest;
import io.harness.delegate.task.aws.lambda.request.AwsLambdaRollbackRequest;
import io.harness.delegate.task.aws.lambda.response.AwsLambdaRollbackResponse;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.sanitizer.ExceptionMessageSanitizer;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;

import software.wings.beans.LogColor;
import software.wings.beans.LogWeight;

import com.google.inject.Inject;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import software.amazon.awssdk.services.lambda.model.CreateFunctionResponse;

@OwnedBy(HarnessTeam.CDP)
@NoArgsConstructor
@Slf4j
public class AwsLambdaRollbackTaskCommandHandler {
  @Inject private AwsLambdaTaskHelper awsLambdaTaskHelper;

  public AwsLambdaRollbackResponse executeTaskInternal(AwsLambdaCommandRequest awsLambdaCommandRequest,
      ILogStreamingTaskClient iLogStreamingTaskClient, CommandUnitsProgress commandUnitsProgress) throws Exception {
    if (!(awsLambdaCommandRequest instanceof AwsLambdaRollbackRequest)) {
      throw new InvalidArgumentsException(Pair.of("awsLambdaCommandRequest",
          "Must be instance of "
              + "AwsLambdaRollbackRequest"));
    }

    AwsLambdaRollbackRequest awsLambdaRollbackRequest = (AwsLambdaRollbackRequest) awsLambdaCommandRequest;

    LogCallback executionLogCallback = awsLambdaTaskHelper.getLogCallback(
        iLogStreamingTaskClient, AwsLambdaCommandUnitConstants.rollback.toString(), true, commandUnitsProgress);

    try {
      executionLogCallback.saveExecutionLog(format("Starting Rollback..%n%n"), LogLevel.INFO);

      if (awsLambdaRollbackRequest.isFirstDeployment()) {
        awsLambdaTaskHelper.deleteFunction(awsLambdaRollbackRequest.getAwsLambdaInfraConfig(),
            awsLambdaRollbackRequest.getFunctionName(), executionLogCallback);
        executionLogCallback.saveExecutionLog(color("Done", Green), LogLevel.INFO, CommandExecutionStatus.SUCCESS);

        return AwsLambdaRollbackResponse.builder().commandExecutionStatus(CommandExecutionStatus.SUCCESS).build();

      } else {
        CreateFunctionResponse createFunctionResponse =
            awsLambdaTaskHelper.rollbackFunction(awsLambdaRollbackRequest.getFunctionName(),
                awsLambdaRollbackRequest.getAwsLambdaInfraConfig(), awsLambdaRollbackRequest.getFunctionCode(),
                awsLambdaRollbackRequest.getFunctionConfiguration(), awsLambdaRollbackRequest.getQualifier(),
                executionLogCallback, awsLambdaRollbackRequest.getAwsLambdaFunctionAliasDefinitionContents());
        executionLogCallback.saveExecutionLog(color("Done", Green), LogLevel.INFO, CommandExecutionStatus.SUCCESS);

        return AwsLambdaRollbackResponse.builder()
            .awsLambda(AwsLambda.builder()
                           .functionName(createFunctionResponse.functionName())
                           .runtime(createFunctionResponse.runtimeAsString())
                           .version(createFunctionResponse.version())
                           .functionArn(createFunctionResponse.functionArn())
                           .build())
            .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
            .build();
      }
    } catch (AwsLambdaException exception) {
      Exception sanitizedException = ExceptionMessageSanitizer.sanitizeException(exception);
      executionLogCallback.saveExecutionLog(
          sanitizedException.getMessage(), LogLevel.INFO, CommandExecutionStatus.SUCCESS);
      return AwsLambdaRollbackResponse.builder().commandExecutionStatus(CommandExecutionStatus.SUCCESS).build();
    } catch (Exception exception) {
      Exception sanitizedException = ExceptionMessageSanitizer.sanitizeException(exception);
      executionLogCallback.saveExecutionLog(color(format("%n Rollback Failed."), LogColor.Red, LogWeight.Bold),
          LogLevel.ERROR, CommandExecutionStatus.FAILURE);
      return AwsLambdaRollbackResponse.builder()
          .errorMessage(ExceptionUtils.getMessage(sanitizedException))
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .build();
    }
  }
}
