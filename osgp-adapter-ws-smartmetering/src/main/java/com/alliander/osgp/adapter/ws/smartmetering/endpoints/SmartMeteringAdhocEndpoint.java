/**
 * Copyright 2015 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package com.alliander.osgp.adapter.ws.smartmetering.endpoints;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import com.alliander.osgp.adapter.ws.endpointinterceptors.MessagePriority;
import com.alliander.osgp.adapter.ws.endpointinterceptors.OrganisationIdentification;
import com.alliander.osgp.adapter.ws.endpointinterceptors.ResponseUrl;
import com.alliander.osgp.adapter.ws.endpointinterceptors.ScheduleTime;
import com.alliander.osgp.adapter.ws.schema.smartmetering.adhoc.GetAllAttributeValuesAsyncRequest;
import com.alliander.osgp.adapter.ws.schema.smartmetering.adhoc.GetAllAttributeValuesAsyncResponse;
import com.alliander.osgp.adapter.ws.schema.smartmetering.adhoc.GetAllAttributeValuesRequest;
import com.alliander.osgp.adapter.ws.schema.smartmetering.adhoc.GetAllAttributeValuesResponse;
import com.alliander.osgp.adapter.ws.schema.smartmetering.adhoc.GetAssociationLnObjectsAsyncRequest;
import com.alliander.osgp.adapter.ws.schema.smartmetering.adhoc.GetAssociationLnObjectsAsyncResponse;
import com.alliander.osgp.adapter.ws.schema.smartmetering.adhoc.GetAssociationLnObjectsRequest;
import com.alliander.osgp.adapter.ws.schema.smartmetering.adhoc.GetAssociationLnObjectsResponse;
import com.alliander.osgp.adapter.ws.schema.smartmetering.adhoc.GetSpecificAttributeValueAsyncRequest;
import com.alliander.osgp.adapter.ws.schema.smartmetering.adhoc.GetSpecificAttributeValueAsyncResponse;
import com.alliander.osgp.adapter.ws.schema.smartmetering.adhoc.GetSpecificAttributeValueRequest;
import com.alliander.osgp.adapter.ws.schema.smartmetering.adhoc.GetSpecificAttributeValueResponse;
import com.alliander.osgp.adapter.ws.schema.smartmetering.adhoc.SynchronizeTimeAsyncRequest;
import com.alliander.osgp.adapter.ws.schema.smartmetering.adhoc.SynchronizeTimeAsyncResponse;
import com.alliander.osgp.adapter.ws.schema.smartmetering.adhoc.SynchronizeTimeRequest;
import com.alliander.osgp.adapter.ws.schema.smartmetering.adhoc.SynchronizeTimeResponse;
import com.alliander.osgp.adapter.ws.schema.smartmetering.common.OsgpResultType;
import com.alliander.osgp.adapter.ws.smartmetering.application.mapping.AdhocMapper;
import com.alliander.osgp.adapter.ws.smartmetering.application.services.AdhocService;
import com.alliander.osgp.adapter.ws.smartmetering.domain.entities.MeterResponseData;
import com.alliander.osgp.domain.core.valueobjects.smartmetering.AssociationLnListType;
import com.alliander.osgp.shared.exceptionhandling.ComponentType;
import com.alliander.osgp.shared.exceptionhandling.OsgpException;
import com.alliander.osgp.shared.exceptionhandling.TechnicalException;
import com.alliander.osgp.shared.infra.jms.ResponseMessageResultType;
import com.alliander.osgp.shared.wsheaderattribute.priority.MessagePriorityEnum;

@Endpoint
public class SmartMeteringAdhocEndpoint extends SmartMeteringEndpoint {

    private static final String SMARTMETER_ADHOC_NAMESPACE = "http://www.alliander.com/schemas/osgp/smartmetering/sm-adhoc/2014/10";

    @Autowired
    private AdhocService adhocService;

    @Autowired
    private AdhocMapper adhocMapper;

    @PayloadRoot(localPart = "SynchronizeTimeRequest", namespace = SMARTMETER_ADHOC_NAMESPACE)
    @ResponsePayload
    public SynchronizeTimeAsyncResponse synchronizeTime(
            @OrganisationIdentification final String organisationIdentification,
            @RequestPayload final SynchronizeTimeRequest request, @MessagePriority final String messagePriority,
            @ResponseUrl final String responseUrl, @ScheduleTime final String scheduleTime) throws OsgpException {

        final SynchronizeTimeAsyncResponse response = new SynchronizeTimeAsyncResponse();

        final com.alliander.osgp.domain.core.valueobjects.smartmetering.SynchronizeTimeRequestData synchronizeTimeRequestData = this.adhocMapper
                .map(request.getSynchronizeTimeRequestData(),
                        com.alliander.osgp.domain.core.valueobjects.smartmetering.SynchronizeTimeRequestData.class);

        final String correlationUid = this.adhocService.enqueueSynchronizeTimeRequest(organisationIdentification,
                request.getDeviceIdentification(), synchronizeTimeRequestData,
                MessagePriorityEnum.getMessagePriority(messagePriority),
                this.adhocMapper.map(scheduleTime, Long.class));

        response.setCorrelationUid(correlationUid);
        response.setDeviceIdentification(request.getDeviceIdentification());

        this.saveResponseUrlIfNeeded(correlationUid, responseUrl);

        return response;
    }

    @PayloadRoot(localPart = "SynchronizeTimeAsyncRequest", namespace = SMARTMETER_ADHOC_NAMESPACE)
    @ResponsePayload
    public SynchronizeTimeResponse getSynchronizeTimeResponse(@RequestPayload final SynchronizeTimeAsyncRequest request)
            throws OsgpException {

        SynchronizeTimeResponse response = null;
        try {
            response = new SynchronizeTimeResponse();
            final MeterResponseData meterResponseData = this.meterResponseDataService
                    .dequeue(request.getCorrelationUid());

            response.setResult(OsgpResultType.fromValue(meterResponseData.getResultType().getValue()));
            if (meterResponseData.getMessageData() instanceof String) {
                response.setDescription((String) meterResponseData.getMessageData());
            }

        } catch (final Exception e) {
            this.handleException(e);
        }
        return response;
    }

    @PayloadRoot(localPart = "GetAllAttributeValuesRequest", namespace = SMARTMETER_ADHOC_NAMESPACE)
    @ResponsePayload
    public GetAllAttributeValuesAsyncResponse getAllAttributeValues(
            @OrganisationIdentification final String organisationIdentification,
            @RequestPayload final GetAllAttributeValuesRequest request, @MessagePriority final String messagePriority,
            @ScheduleTime final String scheduleTime, @ResponseUrl final String responseUrl) throws OsgpException {

        final GetAllAttributeValuesAsyncResponse response = new GetAllAttributeValuesAsyncResponse();

        final com.alliander.osgp.domain.core.valueobjects.smartmetering.GetAllAttributeValuesRequest getAllAttributeValuesRequest = new com.alliander.osgp.domain.core.valueobjects.smartmetering.GetAllAttributeValuesRequest(
                request.getDeviceIdentification());

        final String correlationUid = this.adhocService.enqueueGetAllAttributeValuesRequest(organisationIdentification,
                getAllAttributeValuesRequest.getDeviceIdentification(), getAllAttributeValuesRequest,
                MessagePriorityEnum.getMessagePriority(messagePriority),
                this.adhocMapper.map(scheduleTime, Long.class));

        response.setCorrelationUid(correlationUid);
        response.setDeviceIdentification(request.getDeviceIdentification());

        this.saveResponseUrlIfNeeded(correlationUid, responseUrl);

        return response;
    }

    @PayloadRoot(localPart = "GetAllAttributeValuesAsyncRequest", namespace = SMARTMETER_ADHOC_NAMESPACE)
    @ResponsePayload
    public GetAllAttributeValuesResponse getAllAttributeValuesResponse(
            @RequestPayload final GetAllAttributeValuesAsyncRequest request) throws OsgpException {

        GetAllAttributeValuesResponse response = null;
        try {
            response = new GetAllAttributeValuesResponse();
            final MeterResponseData meterResponseData = this.meterResponseDataService
                    .dequeue(request.getCorrelationUid());

            response.setResult(OsgpResultType.fromValue(meterResponseData.getResultType().getValue()));
            if (meterResponseData.getMessageData() instanceof String) {
                response.setOutput((String) meterResponseData.getMessageData());
            }

        } catch (final Exception e) {
            this.handleException(e);
        }
        return response;
    }

    @PayloadRoot(localPart = "GetSpecificAttributeValueRequest", namespace = SMARTMETER_ADHOC_NAMESPACE)
    @ResponsePayload
    public GetSpecificAttributeValueAsyncResponse getSpecificAttributeValue(
            @OrganisationIdentification final String organisationIdentification,
            @RequestPayload final GetSpecificAttributeValueRequest request,
            @MessagePriority final String messagePriority, @ScheduleTime final String scheduleTime,
            @ResponseUrl final String responseUrl) throws OsgpException {

        final GetSpecificAttributeValueAsyncResponse response = new GetSpecificAttributeValueAsyncResponse();

        final com.alliander.osgp.domain.core.valueobjects.smartmetering.SpecificAttributeValueRequest getSpecificAttributeValueRequest = this.adhocMapper
                .map(request,
                        com.alliander.osgp.domain.core.valueobjects.smartmetering.SpecificAttributeValueRequest.class);

        final String correlationUid = this.adhocService.enqueueSpecificAttributeValueRequest(organisationIdentification,
                request.getDeviceIdentification(), getSpecificAttributeValueRequest,
                MessagePriorityEnum.getMessagePriority(messagePriority),
                this.adhocMapper.map(scheduleTime, Long.class));

        response.setCorrelationUid(correlationUid);
        response.setDeviceIdentification(request.getDeviceIdentification());

        this.saveResponseUrlIfNeeded(correlationUid, responseUrl);

        return response;
    }

    @PayloadRoot(localPart = "GetSpecificAttributeValueAsyncRequest", namespace = SMARTMETER_ADHOC_NAMESPACE)
    @ResponsePayload
    public GetSpecificAttributeValueResponse getSpecificAttributeValueResponse(
            @RequestPayload final GetSpecificAttributeValueAsyncRequest request) throws OsgpException {

        GetSpecificAttributeValueResponse response = null;
        try {
            response = new GetSpecificAttributeValueResponse();
            final MeterResponseData meterResponseData = this.meterResponseDataService
                    .dequeue(request.getCorrelationUid());

            response.setResult(OsgpResultType.fromValue(meterResponseData.getResultType().getValue()));
            if (ResponseMessageResultType.OK == meterResponseData.getResultType()) {
                response.setAttributeValueData((String) meterResponseData.getMessageData());
            } else if (meterResponseData.getMessageData() instanceof OsgpException) {
                throw (OsgpException) meterResponseData.getMessageData();
            } else if (meterResponseData.getMessageData() instanceof Exception) {
                throw new TechnicalException(ComponentType.WS_SMART_METERING,
                        ("An exception occurred: Get specific attribute value"), (Exception) meterResponseData.getMessageData());
            } else {
                throw new TechnicalException(ComponentType.WS_SMART_METERING,
                        ("An exception occurred: Get specific attribute value"), null);
            }
        } catch (final Exception e) {
            this.handleException(e);
        }

        return response;
    }

    @PayloadRoot(localPart = "GetAssociationLnObjectsRequest", namespace = SMARTMETER_ADHOC_NAMESPACE)
    @ResponsePayload
    public GetAssociationLnObjectsAsyncResponse getAssociationLnObjects(
            @OrganisationIdentification final String organisationIdentification,
            @RequestPayload final GetAssociationLnObjectsRequest request, @MessagePriority final String messagePriority,
            @ScheduleTime final String scheduleTime, @ResponseUrl final String responseUrl) throws OsgpException {

        final GetAssociationLnObjectsAsyncResponse response = new GetAssociationLnObjectsAsyncResponse();

        final com.alliander.osgp.domain.core.valueobjects.smartmetering.GetAssociationLnObjectsRequest getAssociationLnObjectsRequest = new com.alliander.osgp.domain.core.valueobjects.smartmetering.GetAssociationLnObjectsRequest(
                request.getDeviceIdentification());

        final String correlationUid = this.adhocService.enqueueGetAssociationLnObjectsRequest(
                organisationIdentification, getAssociationLnObjectsRequest.getDeviceIdentification(),
                getAssociationLnObjectsRequest, MessagePriorityEnum.getMessagePriority(messagePriority),
                this.adhocMapper.map(scheduleTime, Long.class));

        response.setCorrelationUid(correlationUid);
        response.setDeviceIdentification(request.getDeviceIdentification());

        this.saveResponseUrlIfNeeded(correlationUid, responseUrl);

        return response;
    }

    @PayloadRoot(localPart = "GetAssociationLnObjectsAsyncRequest", namespace = SMARTMETER_ADHOC_NAMESPACE)
    @ResponsePayload
    public GetAssociationLnObjectsResponse getAssociationLnObjectsResponse(
            @RequestPayload final GetAssociationLnObjectsAsyncRequest request) throws OsgpException {

        GetAssociationLnObjectsResponse response = null;
        try {
            response = new GetAssociationLnObjectsResponse();
            final MeterResponseData meterResponseData = this.meterResponseDataService
                    .dequeue(request.getCorrelationUid());

            response.setResult(OsgpResultType.fromValue(meterResponseData.getResultType().getValue()));
            if (meterResponseData.getMessageData() instanceof AssociationLnListType) {
                response.setAssociationLnList(this.adhocMapper.map(meterResponseData.getMessageData(),
                        com.alliander.osgp.adapter.ws.schema.smartmetering.adhoc.AssociationLnListType.class));
            }

        } catch (final Exception e) {
            this.handleException(e);
        }
        return response;
    }
}
