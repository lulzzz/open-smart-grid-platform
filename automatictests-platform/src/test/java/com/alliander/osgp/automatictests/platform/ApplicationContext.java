/**
 * Copyright 2017 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.alliander.osgp.automatictests.platform;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

import com.alliander.osgp.automatictests.platform.config.ws.microgrids.MicrogridsNotificationWebServiceConfig;

/**
 * Scan all automatic tests packages.
 */
@ComponentScan(basePackages={
        "com.alliander.osgp.automatictests"
    },
    excludeFilters = @ComponentScan.Filter(value = MicrogridsNotificationWebServiceConfig.class, type = FilterType.ASSIGNABLE_TYPE)
)
public class ApplicationContext {

    public ApplicationContext() {
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Constructing ApplicationContext");
    }
}
