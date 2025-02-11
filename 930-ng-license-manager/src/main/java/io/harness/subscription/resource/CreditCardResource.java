/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.subscription.resource;

import static io.harness.NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE;
import static io.harness.licensing.accesscontrol.LicenseAccessControlPermissions.VIEW_LICENSE_PERMISSION;

import io.harness.NGCommonEntityConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.NGAccessControlCheck;
import io.harness.licensing.accesscontrol.ResourceTypes;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.NextGenManagerAuth;
import io.harness.subscription.dto.CardDTO;
import io.harness.subscription.dto.CreditCardDTO;
import io.harness.subscription.responses.AccountCreditCardValidationResponse;
import io.harness.subscription.responses.CreditCardResponse;
import io.harness.subscription.responses.CreditCardValidationResponse;
import io.harness.subscription.services.CreditCardService;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api(value = "credit-cards")
@Path("credit-cards")
@Produces({"application/json"})
@Consumes({"application/json"})
@Tag(name = "Credit Cards", description = "This contains APIs related to credit cards as defined in Harness")
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request",
    content =
    {
      @Content(mediaType = "application/json", schema = @Schema(implementation = FailureDTO.class))
      , @Content(mediaType = "application/yaml", schema = @Schema(implementation = FailureDTO.class))
    })
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
    content =
    {
      @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDTO.class))
      , @Content(mediaType = "application/yaml", schema = @Schema(implementation = ErrorDTO.class))
    })
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@Hidden
@NextGenManagerAuth
public class CreditCardResource {
  @Inject private CreditCardService creditCardService;

  @POST
  @ApiOperation(value = "Saves non-sensitive credit card information", nickname = "save card")
  @Operation(operationId = "saveCreditCard", summary = "Saves non-sensitive credit card information")
  public ResponseDTO<CreditCardResponse> saveCreditCard(
      @Parameter(required = true, description = ACCOUNT_PARAM_MESSAGE) @NotNull @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @Valid @RequestBody(description = "Non-sensitive credit card information") @NotNull CreditCardDTO creditCardDTO) {
    return ResponseDTO.newResponse(creditCardService.saveCreditCard(creditCardDTO));
  }

  @DELETE
  @Path("{identifier}")
  @ApiOperation(value = "Deletes credit card information", nickname = "delete card")
  @Operation(operationId = "deleteCreditCard", summary = "Deletes credit card information")
  public ResponseDTO<CreditCardResponse> deleteCreditCard(
      @Parameter(required = true, description = ACCOUNT_PARAM_MESSAGE) @NotNull @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @Parameter(required = true, description = "Credit Card Identifier") @NotNull @Valid @PathParam(
          "identifier") @NotNull String creditCardIdentifier) {
    return ResponseDTO.newResponse(creditCardService.deleteCreditCard(accountIdentifier, creditCardIdentifier));
  }

  @GET
  @Path("/default")
  @ApiOperation(value = "Gets the default card for the customer", nickname = "getDefaultCard")
  @Operation(operationId = "getDefaultCard", summary = "Gets the default card for the customer",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns payment details")
      })
  @NGAccessControlCheck(resourceType = ResourceTypes.LICENSE, permission = VIEW_LICENSE_PERMISSION)
  public ResponseDTO<CardDTO>
  getDefaultCreditCard(@Parameter(required = true, description = ACCOUNT_PARAM_MESSAGE) @NotNull @QueryParam(
      NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier) {
    return ResponseDTO.newResponse(creditCardService.getDefaultCreditCard(accountIdentifier));
  }

  @GET
  @Path("/has-valid-card")
  @ApiOperation(value = "Checks for a valid credit card", nickname = "has a valid card")
  @Operation(operationId = "hasValidCard", summary = "Checks if the account has a valid credit card")
  public ResponseDTO<AccountCreditCardValidationResponse> validateCreditCard(
      @Parameter(required = true, description = ACCOUNT_PARAM_MESSAGE) @NotNull @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier) {
    return ResponseDTO.newResponse(
        AccountCreditCardValidationResponse.builder()
            .hasAtleastOneValidCreditCard(creditCardService.hasAtleastOneValidCreditCard(accountIdentifier))
            .build());
  }

  @GET
  @Path("{identifier}/is-valid")
  @ApiOperation(value = "Checks validity of a credit card", nickname = "is valid")
  @Operation(operationId = "isValid", summary = "Checks validity of a credit card in an account")
  public ResponseDTO<CreditCardValidationResponse> isValid(
      @Parameter(required = true, description = ACCOUNT_PARAM_MESSAGE) @NotNull @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @Parameter(required = true, description = "Credit Card Identifier") @NotNull @PathParam(
          "identifier") @NotNull String creditCardIdentifier) {
    return ResponseDTO.newResponse(CreditCardValidationResponse.builder()
                                       .isValid(creditCardService.isValid(accountIdentifier, creditCardIdentifier))
                                       .build());
  }
}
