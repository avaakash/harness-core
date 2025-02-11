/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.utils;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.pms.yaml.ParameterField;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_MIGRATOR})
public interface NGMigrationConstants {
  String DISCOVERY_IMAGE_PATH = "/tmp/viz-output/viz.png";
  String DEFAULT_ZIP_DIRECTORY = "/tmp/zip-output";
  String ZIP_FILE_PATH = "/yamls.zip";
  String VIZ_TEMP_DIR_PREFIX = "viz-output";
  String VIZ_FILE_NAME = "/viz.png";
  String PLEASE_FIX_ME = "__PLEASE_FIX_ME__";
  String RUNTIME_INPUT = "<+input>";
  ParameterField<String> RUNTIME_FIELD = ParameterField.createValueField(RUNTIME_INPUT);
  String SERVICE_COMMAND_TEMPLATE_SEPARATOR = "::";
  String UNKNOWN_SERVICE = "UNKNOWN_S";
  String SECRET_FORMAT = "<+secrets.getValue(\"%s\")>";
  String TRIGGER_TAG_VALUE_DEFAULT = "<+trigger.artifact.build>";

  String SERVICE_ID = "serviceId";

  String INFRA_DEFINITION_ID = "infraDefinitionId";
  String SERVICE_INPUTS = "serviceInputs";
  String INFRASTRUCTURE_DEFINITIONS = "infrastructureDefinitions";
  String AUTOGENERATED_RELEASE_NAME = "autogenerated_release_name";
  String AUTOGENERATED_RELEASE_NAME_STAGE_EXPRESSION = "<+stage.variables.autogenerated_release_name>";
  String LONG_RELEASE_NAME = "release-<+INFRA_KEY>";
  String DEFAULT_RELEASE_NAME = "release-<+INFRA_KEY_SHORT_ID>";
}
