/**
 * Copyright 2016 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package com.alliander.osgp.platform.cucumber.steps.ws.admin.devicemanagement;

import static com.alliander.osgp.platform.cucumber.core.Helpers.getBoolean;
import static com.alliander.osgp.platform.cucumber.core.Helpers.getEnum;
import static com.alliander.osgp.platform.cucumber.core.Helpers.getString;

import java.util.Map;

import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ws.soap.client.SoapFaultClientException;

import com.alliander.osgp.adapter.ws.schema.admin.devicemanagement.CreateOrganizationRequest;
import com.alliander.osgp.adapter.ws.schema.admin.devicemanagement.CreateOrganizationResponse;
import com.alliander.osgp.adapter.ws.schema.admin.devicemanagement.Organisation;
import com.alliander.osgp.adapter.ws.schema.admin.devicemanagement.PlatformDomain;
import com.alliander.osgp.adapter.ws.schema.admin.devicemanagement.PlatformFunctionGroup;
import com.alliander.osgp.platform.cucumber.core.ScenarioContext;
import com.alliander.osgp.platform.cucumber.steps.Defaults;
import com.alliander.osgp.platform.cucumber.steps.Keys;
import com.alliander.osgp.platform.cucumber.steps.ws.GenericResponseSteps;
import com.alliander.osgp.platform.cucumber.support.ws.admin.AdminDeviceManagementClient;

import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

/**
 * Class with all the create organization requests steps
 */
public class CreateOrganizationSteps {

    @Autowired
    private AdminDeviceManagementClient client;

    /**
     *
     * @throws Throwable
     */
    @When("^receiving a create organization request$")
    public void receiving_a_create_organization_request(final Map<String, String> requestSettings) throws Throwable {

        final CreateOrganizationRequest request = new CreateOrganizationRequest();
        final Organisation organization = new Organisation();
        organization.setEnabled(getBoolean(requestSettings, Keys.KEY_ENABLED, Defaults.DEFAULT_ORGANIZATION_ENABLED));
        organization.setName(getString(requestSettings, Keys.KEY_NAME, Defaults.DEFAULT_ORGANIZATION_NAME));
        organization.setOrganisationIdentification(getString(requestSettings, Keys.KEY_ORGANIZATION_IDENTIFICATION,
                Defaults.DEFAULT_ORGANIZATION_IDENTIFICATION));
        organization.setPrefix(getString(requestSettings, Keys.KEY_PREFIX, Defaults.DEFAULT_ORGANIZATION_PREFIX));
        if (organization.getPrefix().isEmpty()) {
            organization.setPrefix(Defaults.DEFAULT_ORGANIZATION_PREFIX);
        }

        final PlatformFunctionGroup platformFunctionGroup = getEnum(requestSettings, Keys.KEY_PLATFORM_FUNCTION_GROUP,
                PlatformFunctionGroup.class, Defaults.DEFAULT_NEW_ORGANIZATION_PLATFORMFUNCTIONGROUP);
        organization.setFunctionGroup(platformFunctionGroup);

        String domains = getString(requestSettings, Keys.KEY_DOMAINS, Defaults.DEFAULT_DOMAINS);
        if (domains.isEmpty()) {
            domains = Defaults.DEFAULT_DOMAINS;
        }
        for (final String domain : domains.split(";")) {
            organization.getDomains().add(Enum.valueOf(PlatformDomain.class, domain));
        }

        request.setOrganisation(organization);

        try {
            ScenarioContext.Current().put(Keys.RESPONSE, this.client.createOrganization(request));
        } catch (final SoapFaultClientException e) {
            ScenarioContext.Current().put(Keys.RESPONSE, e);
        }
    }

    /**
     *
     * @throws Throwable
     */
    @When("^receiving a create organization request as an unauthorized organization$")
    public void receiving_a_create_organization_request_as_an_unauthorized_organization(
            final Map<String, String> requestSettings) throws Throwable {

        // Force WSTF to use a different organization to send the requests with.
        // (Cerificate is used from the certificates directory).
        ScenarioContext.Current().put(Keys.KEY_ORGANIZATION_IDENTIFICATION, "unknown-organization");

        this.receiving_a_create_organization_request(requestSettings);
    }

    /**
     * Verify that the create organization response is successful.
     *
     * @throws Throwable
     */
    @Then("^the create organization response is successfull$")
    public void the_create_organization_response_is_successfull() throws Throwable {

        final Object obj = ScenarioContext.Current().get(Keys.RESPONSE);

        Assert.assertTrue(ScenarioContext.Current().get(Keys.RESPONSE) instanceof CreateOrganizationResponse);
    }

    /**
     * Verify that the create organization response contains the fault with the
     * given expectedResult parameters.
     *
     * @param expectedResult
     * @throws Throwable
     */
    @Then("^the create organization response contains$")
    public void the_create_organization_response_contains(final Map<String, String> expectedResult) throws Throwable {
        GenericResponseSteps.VerifySoapFault(expectedResult);
    }
}