/**
 * Copyright 2015 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.opensmartgridplatform.adapter.protocol.oslp.elster.infra.networking;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.codec.binary.Base64;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.joda.time.Instant;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.opensmartgridplatform.adapter.protocol.oslp.elster.application.services.DeviceManagementService;
import org.opensmartgridplatform.adapter.protocol.oslp.elster.application.services.DeviceRegistrationService;
import org.opensmartgridplatform.adapter.protocol.oslp.elster.application.services.oslp.OslpDeviceSettingsService;
import org.opensmartgridplatform.adapter.protocol.oslp.elster.application.services.oslp.OslpSigningService;
import org.opensmartgridplatform.adapter.protocol.oslp.elster.domain.entities.OslpDevice;
import org.opensmartgridplatform.adapter.protocol.oslp.elster.exceptions.ProtocolAdapterException;
import org.opensmartgridplatform.core.db.api.application.services.DeviceDataService;
import org.opensmartgridplatform.dto.valueobjects.GpsCoordinatesDto;
import org.opensmartgridplatform.oslp.Oslp;
import org.opensmartgridplatform.oslp.Oslp.EventNotificationRequest;
import org.opensmartgridplatform.oslp.Oslp.LocationInfo;
import org.opensmartgridplatform.oslp.Oslp.Message;
import org.opensmartgridplatform.oslp.OslpEnvelope;
import org.opensmartgridplatform.oslp.SignedOslpEnvelopeDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public class OslpChannelHandlerServer extends OslpChannelHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(OslpChannelHandlerServer.class);

    private static DateTimeFormatter format = DateTimeFormat.forPattern("yyyyMMddHHmmss");

    @Autowired
    private DeviceRegistrationService deviceRegistrationService;

    @Autowired
    private DeviceManagementService deviceManagementService;

    @Autowired
    private Integer sequenceNumberWindow;

    @Autowired
    private Integer timeZoneOffsetMinutes;

    @Autowired
    private Float defaultLatitude;

    @Autowired
    private Float defaultLongitude;

    @Autowired
    private OslpDeviceSettingsService oslpDeviceSettingsService;

    @Autowired
    private DeviceDataService deviceDataService;

    @Autowired
    private OslpSigningService oslpSigningService;

    /**
     * Convert list in property files to {@code Map}.
     *
     * See the SpEL documentation for more information:
     * https://docs.spring.io/spring/docs/3.0.x/reference/expressions.html
     */
    @Value("#{${test.device.ips}}")
    private Map<String, String> testDeviceIps;

    private final ConcurrentMap<Integer, Channel> channelMap = new ConcurrentHashMap<>();

    public OslpChannelHandlerServer() {
        super(LOGGER);
    }

    public OslpChannelHandlerServer(final int maxConcurrentIncomingMessages) {
        super(LOGGER, maxConcurrentIncomingMessages);
    }

    private Channel findChannel(final Integer channelId) {
        return this.channelMap.get(channelId);
    }

    private void cacheChannel(final Integer channelId, final Channel channel) {
        this.channelMap.put(channelId, channel);
    }

    public void setDeviceManagementService(final DeviceManagementService deviceManagementService) {
        this.deviceManagementService = deviceManagementService;
    }

    public void setDeviceRegistrationService(final DeviceRegistrationService deviceRegistrationService) {
        this.deviceRegistrationService = deviceRegistrationService;
    }

    public void setSequenceNumberWindow(final Integer sequenceNumberWindow) {
        this.sequenceNumberWindow = sequenceNumberWindow;
    }

    public void setTimeZoneOffsetMinutes(final int timeZoneOffsetMinutes) {
        this.timeZoneOffsetMinutes = timeZoneOffsetMinutes;
    }

    @Override
    public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception {

        final OslpEnvelope message = (OslpEnvelope) e.getMessage();
        this.logMessage(message, true);

        final Integer channelId = e.getChannel().getId();
        if (message.isValid()) {
            if (this.isOslpResponse(message)) {
                LOGGER.warn("{} Received OSLP Response, which is not expected: {}", channelId,
                        message.getPayloadMessage());
            } else {
                LOGGER.info("{} Received OSLP Request: {}", channelId, message.getPayloadMessage());

                // Response pay-load to send to device.
                Message payload;

                // Check which request the device has sent and handle it.
                if (message.getPayloadMessage().hasRegisterDeviceRequest()) {
                    payload = this.handleRegisterDeviceRequest(message.getDeviceId(), message.getSequenceNumber(),
                            message.getPayloadMessage().getRegisterDeviceRequest());
                } else if (message.getPayloadMessage().hasConfirmRegisterDeviceRequest()) {
                    payload = this.handleConfirmRegisterDeviceRequest(message.getDeviceId(),
                            message.getSequenceNumber(), message.getPayloadMessage().getConfirmRegisterDeviceRequest());
                } else if (message.getPayloadMessage().hasEventNotificationRequest()) {
                    payload = this.handleEventNotificationRequest(message.getDeviceId(), message.getSequenceNumber(),
                            message.getPayloadMessage().getEventNotificationRequest());
                } else {
                    LOGGER.warn("{} Received unknown payload. Received: {}.", channelId,
                            message.getPayloadMessage().toString());
                    // Optional extra: return error code to device.
                    return;
                }

                // Cache the channel so we can write the response to it later.
                this.cacheChannel(channelId, e.getChannel());

                // Send message to signing server to get our response signed.
                this.oslpSigningService.buildAndSignEnvelope(message.getDeviceId(), message.getSequenceNumber(),
                        payload, channelId, this);
            }
        } else {
            LOGGER.warn("{} Received message wasn't properly secured.", channelId);
        }
    }

    /**
     * Called when a signed OSLP envelope arrives from signing server. The
     * envelope will be sent to the device which is waiting for a response. The
     * channel for the waiting device should be present in the channelMap.
     *
     * @param signedOslpEnvelopeDto
     *            DTO containing signed OslpEnvelope.
     */
    public void processSignedOslpEnvelope(final SignedOslpEnvelopeDto signedOslpEnvelopeDto) {

        // Try to find the channel.
        final Integer channelId = Integer
                .parseInt(signedOslpEnvelopeDto.getUnsignedOslpEnvelopeDto().getCorrelationUid());
        final Channel channel = this.findChannel(channelId);
        if (channel == null) {
            LOGGER.error("Unable to find channel for channelId: {}. Can't send response message to device.", channelId);
            return;
        }

        // Get signed envelope, log it and send it to device.
        final OslpEnvelope response = signedOslpEnvelopeDto.getOslpEnvelope();
        this.logMessage(response, false);
        channel.write(response);

        LOGGER.info("{} Send OSLP Response: {}", channelId, response.getPayloadMessage());
    }

    private Oslp.Message handleRegisterDeviceRequest(final byte[] deviceUid, final byte[] sequenceNumber,
            final Oslp.RegisterDeviceRequest registerRequest) throws UnknownHostException {

        final String deviceIdentification = registerRequest.getDeviceIdentification();
        final String deviceType = registerRequest.getDeviceType().toString();
        final boolean hasSchedule = registerRequest.getHasSchedule();
        InetAddress inetAddress;

        // In case the optional properties 'testDeviceId' and 'testDeviceIp' are
        // set, the values will be used to set an IP address for a device.
        if (this.testDeviceIps != null && this.testDeviceIps.containsKey(deviceIdentification)) {
            final String testDeviceIp = this.testDeviceIps.get(deviceIdentification);
            LOGGER.info("Using testDeviceId: {} and testDeviceIp: {}", deviceIdentification, testDeviceIp);
            inetAddress = InetAddress.getByName(testDeviceIp);
        } else {
            inetAddress = InetAddress.getByAddress(registerRequest.getIpAddress().toByteArray());
        }

        // Send message to OSGP-CORE to save IP Address, device type and has
        // schedule values in OSGP-CORE database.
        this.deviceRegistrationService.sendDeviceRegisterRequest(inetAddress, deviceType, hasSchedule,
                deviceIdentification);

        OslpDevice oslpDevice = this.oslpDeviceSettingsService
                .getDeviceByDeviceIdentification(registerRequest.getDeviceIdentification());

        // Save the security related values in the OSLP database.
        oslpDevice.updateRegistrationData(deviceUid, registerRequest.getDeviceType().toString(),
                registerRequest.getRandomDevice());
        oslpDevice.setSequenceNumber(SequenceNumberUtils.convertByteArrayToInteger(sequenceNumber));
        oslpDevice = this.oslpDeviceSettingsService.updateDevice(oslpDevice);

        // Return current date and time in UTC so the device can sync the clock.
        final Oslp.RegisterDeviceResponse.Builder responseBuilder = Oslp.RegisterDeviceResponse.newBuilder()
                .setStatus(Oslp.Status.OK).setCurrentTime(Instant.now().toString(format))
                .setRandomDevice(registerRequest.getRandomDevice()).setRandomPlatform(oslpDevice.getRandomPlatform());

        // Return local time zone information of the platform. Devices can use
        // this to convert UTC times to local times.
        final LocationInfo.Builder locationInfo = LocationInfo.newBuilder();
        locationInfo.setTimeOffset(this.timeZoneOffsetMinutes);

        // Get the GPS values from OSGP-CORE database.
        final GpsCoordinatesDto gpsCoordinates = this.deviceDataService
                .getGpsCoordinatesForDevice(deviceIdentification);
        if (gpsCoordinates != null && gpsCoordinates.getLatitude() != null && gpsCoordinates.getLongitude() != null) {
            // Add GPS information when available in meta data.
            locationInfo.setLatitude(this.convertGpsCoordinateFromFloatToInt(gpsCoordinates.getLatitude()))
                    .setLongitude(this.convertGpsCoordinateFromFloatToInt(gpsCoordinates.getLongitude()));
        } else {
            // Otherwise use default GPS information.
            locationInfo.setLatitude(this.convertGpsCoordinateFromFloatToInt(this.defaultLatitude))
                    .setLongitude(this.convertGpsCoordinateFromFloatToInt(this.defaultLongitude));
        }

        responseBuilder.setLocationInfo(locationInfo);

        return Oslp.Message.newBuilder().setRegisterDeviceResponse(responseBuilder.build()).build();
    }

    private int convertGpsCoordinateFromFloatToInt(final Float input) {
        return (int) (input * 1000000);
    }

    private Oslp.Message handleConfirmRegisterDeviceRequest(final byte[] deviceId, final byte[] sequenceNumber,
            final Oslp.ConfirmRegisterDeviceRequest confirmRegisterDeviceRequest) throws ProtocolAdapterException {

        try {
            this.deviceRegistrationService.confirmRegisterDevice(deviceId,
                    SequenceNumberUtils.convertByteArrayToInteger(sequenceNumber),
                    confirmRegisterDeviceRequest.getRandomDevice(), confirmRegisterDeviceRequest.getRandomPlatform());
        } catch (final Exception e) {
            LOGGER.error("handle confirm register device request exception", e);
            throw new ProtocolAdapterException("ConfirmRegisterDevice failed", e);
        }

        return Oslp.Message.newBuilder()
                .setConfirmRegisterDeviceResponse(Oslp.ConfirmRegisterDeviceResponse.newBuilder()
                        .setStatus(Oslp.Status.OK).setRandomDevice(confirmRegisterDeviceRequest.getRandomDevice())
                        .setRandomPlatform(confirmRegisterDeviceRequest.getRandomPlatform())
                        .setSequenceWindow(this.sequenceNumberWindow))
                .build();
    }

    private Oslp.Message handleEventNotificationRequest(final byte[] deviceId, final byte[] sequenceNumber,
            final EventNotificationRequest request) throws ProtocolAdapterException {

        // Check & update sequence number first
        try {
            this.deviceRegistrationService.updateDeviceSequenceNumber(deviceId,
                    SequenceNumberUtils.convertByteArrayToInteger(sequenceNumber));
        } catch (final ProtocolAdapterException ex) {
            LOGGER.error("handle event notification request exception", ex);
            return Oslp.Message.newBuilder().setEventNotificationResponse(
                    Oslp.EventNotificationResponse.newBuilder().setStatus(Oslp.Status.REJECTED)).build();
        }

        final Oslp.Status oslpStatus = Oslp.Status.OK;
        final String deviceUid = Base64.encodeBase64String(deviceId);

        this.deviceManagementService.addEventNotifications(deviceUid, request.getNotificationsList());

        return Oslp.Message.newBuilder()
                .setEventNotificationResponse(Oslp.EventNotificationResponse.newBuilder().setStatus(oslpStatus))
                .build();
    }
}
