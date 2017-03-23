/*
 * Copyright 2017 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package com.smartsocietyservices.osgp.dto.da;

import java.io.Serializable;

public class MeasurementIdentifierDto implements Serializable {

    private static final long serialVersionUID = 5587798706867134143L;

    private int id;
    private String node;

    public MeasurementIdentifierDto(final int id, final String node) {
        this.id = id;
        this.node = node;
    }

    public int getId() {
        return this.id;
    }

    public String getNode() {
        return this.node;
    }
}
