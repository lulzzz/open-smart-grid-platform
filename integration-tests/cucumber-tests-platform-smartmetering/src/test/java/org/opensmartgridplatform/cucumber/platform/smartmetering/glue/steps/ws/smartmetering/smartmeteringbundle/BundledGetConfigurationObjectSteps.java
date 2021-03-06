/**
 * Copyright 2017 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.opensmartgridplatform.cucumber.platform.smartmetering.glue.steps.ws.smartmetering.smartmeteringbundle;

import static org.opensmartgridplatform.cucumber.core.ReadSettingsHelper.getBoolean;

import java.util.Map;

import org.junit.Assert;

import org.opensmartgridplatform.adapter.ws.schema.smartmetering.bundle.GetConfigurationObjectRequest;
import org.opensmartgridplatform.adapter.ws.schema.smartmetering.bundle.GetConfigurationObjectResponse;
import org.opensmartgridplatform.adapter.ws.schema.smartmetering.common.Response;
import org.opensmartgridplatform.adapter.ws.schema.smartmetering.configuration.ConfigurationFlag;
import org.opensmartgridplatform.adapter.ws.schema.smartmetering.configuration.ConfigurationObject;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;

public class BundledGetConfigurationObjectSteps extends BaseBundleSteps {

    @Given("^the bundle request contains a get configuration object action$")
    public void theBundleRequestContainsAGetConfigurationObject() throws Throwable {

        final GetConfigurationObjectRequest action = new GetConfigurationObjectRequest();

        this.addActionToBundleRequest(action);
    }

    @Then("^the bundle response should contain a get configuration object response$")
    public void theBundleResponseShouldContainAConfigurationObjectResponse() throws Throwable {

        final Response response = this.getNextBundleResponse();

        Assert.assertTrue("response should be a GetConfigurationResponse object",
                response instanceof GetConfigurationObjectResponse);
    }

    @Then("^the bundle response should contain a get configuration object response with values$")
    public void theBundleResponseShouldContainAConfigurationObjectResponse(final Map<String, String> values)
            throws Throwable {

        final Response response = this.getNextBundleResponse();

        Assert.assertTrue("response should be a GetConfigurationResponse object",
                response instanceof GetConfigurationObjectResponse);
        final ConfigurationObject configurationObject = ((GetConfigurationObjectResponse) response)
                .getConfigurationObject();

        Assert.assertEquals("The gprs operation mode is not equal", values.get("GprsOperationMode"),
                configurationObject.getGprsOperationMode().toString());
        configurationObject.getConfigurationFlags().getConfigurationFlag()
                .forEach(f -> this.testConfigurationFlag(f, values));
    }

    private void testConfigurationFlag(final ConfigurationFlag configFlag, final Map<String, String> settings) {
        final String key = configFlag.getConfigurationFlagType().name();
        final boolean value = getBoolean(settings, key);
        Assert.assertEquals("The enabled value for configuration flag " + key, value, configFlag.isEnabled());
    }
}
