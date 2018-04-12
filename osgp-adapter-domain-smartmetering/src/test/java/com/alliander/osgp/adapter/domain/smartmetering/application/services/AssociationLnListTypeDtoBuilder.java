/**
 * Copyright 2018 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.alliander.osgp.adapter.domain.smartmetering.application.services;

import java.util.ArrayList;
import java.util.List;

import com.alliander.osgp.dto.valueobjects.smartmetering.AssociationLnListElementDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.AssociationLnListTypeDto;

public class AssociationLnListTypeDtoBuilder {

    private List<AssociationLnListElementDto> associationLnListElementDto = new ArrayList<>();

    public AssociationLnListTypeDto build() {
        return new AssociationLnListTypeDto(this.associationLnListElementDto);
    }

    public AssociationLnListTypeDtoBuilder withEmptyLists(
            final List<AssociationLnListElementDto> associationLnListElementDto) {
        this.associationLnListElementDto = associationLnListElementDto;
        return this;
    }

    public AssociationLnListTypeDtoBuilder withFilledArguments(
            final AssociationLnListElementDto associationLnListElementDto) {
        this.associationLnListElementDto = new ArrayList<>();
        this.associationLnListElementDto.add(associationLnListElementDto);
        return this;
    }

}
