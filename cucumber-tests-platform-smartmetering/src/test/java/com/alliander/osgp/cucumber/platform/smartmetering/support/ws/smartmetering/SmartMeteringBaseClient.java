/**
 * Copyright 2017 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.alliander.osgp.cucumber.platform.smartmetering.support.ws.smartmetering;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.alliander.osgp.adapter.ws.schema.smartmetering.notification.Notification;
import com.alliander.osgp.cucumber.platform.smartmetering.support.ws.smartmetering.notification.NotificationService;
import com.alliander.osgp.cucumber.platform.support.ws.BaseClient;

public abstract class SmartMeteringBaseClient extends BaseClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmartMeteringBaseClient.class);

    @Autowired
    private NotificationService notificationService;

    @Value("${smartmetering.response.wait.fail.duration:30000}")
    private int waitFailMillis;

    public void setWaitFailMillis(final int waitFailMillis) {
        this.waitFailMillis = waitFailMillis;
    }

    protected void waitForNotification(final String correlationUid) {
        LOGGER.info("Waiting for a notification for correlation UID {} for at most {} milliseconds.", correlationUid,
                this.waitFailMillis);

        final Notification notification = this.notificationService.getNotification(correlationUid, this.waitFailMillis,
                TimeUnit.MILLISECONDS);

        if (notification == null) {
            throw new AssertionError("Did not receive a notification for correlation UID: " + correlationUid + " within "
                    + this.waitFailMillis + " milliseconds");
        }
    }
}
