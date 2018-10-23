/**
 * Copyright 2014-2016 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.opensmartgridplatform.adapter.protocol.iec61850.infra.networking;

import java.net.InetAddress;
import java.util.Map;
import java.util.UUID;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.opensmartgridplatform.adapter.protocol.iec61850.application.services.DeviceRegistrationService;
import org.opensmartgridplatform.adapter.protocol.iec61850.infra.messaging.OsgpRequestMessageSender;
import org.opensmartgridplatform.adapter.protocol.iec61850.infra.networking.helper.IED;
import org.opensmartgridplatform.core.db.api.iec61850.entities.Ssld;
import org.opensmartgridplatform.dto.valueobjects.DeviceFunctionDto;
import org.opensmartgridplatform.dto.valueobjects.DeviceRegistrationDataDto;
import org.opensmartgridplatform.iec61850.RegisterDeviceRequest;
import org.opensmartgridplatform.shared.infra.jms.RequestMessage;

public class Iec61850ChannelHandlerServer extends Iec61850ChannelHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Iec61850ChannelHandlerServer.class);

    @Autowired
    private OsgpRequestMessageSender osgpRequestMessageSender;

    @Autowired
    private DeviceRegistrationService deviceRegistrationService;

    /**
     * Convert list in property files to {@code Map}.
     *
     * See the SpEL documentation for more information:
     * https://docs.spring.io/spring/docs/3.0.x/reference/expressions.html
     */
    @Value("#{${test.device.ips}}")
    private Map<String, String> testDeviceIps;

    public Iec61850ChannelHandlerServer() {
        super(LOGGER);
    }

    @Override
    public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception {

        final RegisterDeviceRequest message = (RegisterDeviceRequest) e.getMessage();

        final String correlationId = UUID.randomUUID().toString().replace("-", "");

        this.processRegistrationMessage(message, correlationId);
    }

    private void processRegistrationMessage(final RegisterDeviceRequest message, final String correlationId) {

        this.logMessage(message);

        final String deviceIdentification = message.getDeviceIdentification();
        final IED ied = IED.FLEX_OVL;
        String ipAddress;

        // In case the optional properties 'testDeviceId' and 'testDeviceIp' are
        // set, the values will be used to set an IP address for a device.
        if (this.testDeviceIps != null && this.testDeviceIps.containsKey(deviceIdentification)) {
            final String testDeviceIp = this.testDeviceIps.get(deviceIdentification);
            LOGGER.info("Using testDeviceId: {} and testDeviceIp: {}", deviceIdentification, testDeviceIp);
            ipAddress = testDeviceIp;
        } else {
            ipAddress = message.getIpAddress();
        }

        final DeviceRegistrationDataDto deviceRegistrationData = new DeviceRegistrationDataDto(ipAddress,
                Ssld.SSLD_TYPE, true);

        final RequestMessage requestMessage = new RequestMessage(correlationId, "no-organisation", deviceIdentification,
                ipAddress, deviceRegistrationData);

        LOGGER.info("Sending register device request to OSGP with correlation ID: " + correlationId);
        this.osgpRequestMessageSender.send(requestMessage, DeviceFunctionDto.REGISTER_DEVICE.name());

        try {
            this.deviceRegistrationService.disableRegistration(deviceIdentification, InetAddress.getByName(ipAddress),
                    ied, ied.getDescription());
            LOGGER.info("Disabled registration for device: {}, at IP address: {}", deviceIdentification, ipAddress);
        } catch (final Exception e) {
            LOGGER.error("Failed to disable registration for device: {}, at IP address: {}", deviceIdentification,
                    ipAddress, e);
        }
    }
}
