Feature: Organization management
  As a ...
  I want to manage the Organizations in the platform
  In order ...

  Scenario Outline: Create A New Organization
    When receiving a create organization request
      | OrganizationIdentification | <OrganizationIdentification> |
      | Name                       | <Name>                       |
      | Prefix                     | <Prefix>                     |
      | FunctionGroup              | <FunctionGroup>              |
      | Enabled                    | <Enabled>                    |
      | Domains                    | <Domains>                    |
    Then the create organization response is successfull
    And the entity organization exists
      | OrganizationIdentification | <OrganizationIdentification> |
      | Name                       | <Name>                       |
      | Prefix                     | <Prefix>                     |
      | FunctionGroup              | <FunctionGroup>              |
      | Enabled                    | <Enabled>                    |
      | Domains                    | <Domains>                    |

    Examples: 
      | OrganizationIdentification | Name                | Prefix | FunctionGroup | Enabled | Domains |
      | ATestOrganization          | A Test Organization | MAA    | ADMIN         | true    | COMMON  |
      | Heerlen                    | Gemeente Heerlen    |        | USER          |         |         |

  Scenario Outline: Create An Already Existing Organization
    Given an organization
      | OrganizationIdentification | <OrganizationIdentification> |
      | Name                       | <Original Name>              |
      | Prefix                     | <Prefix>                     |
      | FunctionGroup              | <FunctionGroup>              |
      | Enabled                    | <Enabled>                    |
      | Domains                    | <Domains>                    |
    When receiving a create organization request
      | OrganizationIdentification | <OrganizationIdentification> |
      | Name                       | <Different Name>             |
      | Prefix                     | <Prefix>                     |
      | FunctionGroup              | <FunctionGroup>              |
      | Enabled                    | <Enabled>                    |
      | Domains                    | <Domains>                    |
    Then the create organization response contains
      | Message | com.alliander.osgp.shared.exceptionhandling.TechnicalException |
    And the organization with name "Different Organization" should not be created

    Examples: 
      | OrganizationIdentification | Original Name       | Prefix | FunctionGroup | Enabled | Domains | Different Name         |
      | ATestOrganization          | A Test Organization | MAA    | ADMIN         | true    | COMMON  | Different Organization |

  Scenario Outline: Create An Organization As An Unauthorized Organization
    When receiving a create organization request as an unauthorized organization
      | OrganizationIdentification | <OrganizationIdentification> |
      | Name                       | <Name>                       |
      | Prefix                     | <Prefix>                     |
      | FunctionGroup              | <FunctionGroup>              |
      | Enabled                    | <Enabled>                    |
      | Domains                    | <Domains>                    |
    Then the create organization response contains
      | Message | UNKNOWN_ORGANISATION |
    And the organization with name "<Name>" should not be created

    Examples: 
      | OrganizationIdentification | Name            | Prefix | FunctionGroup | Enabled | Domains |
      | ATestOrganization          | An Organization | MAA    | ADMIN         | true    | COMMON  |

  Scenario Outline: Creating An Organization With An Invalid Organization Identification
    When receiving a create organization request
      | OrganizationIdentification | <Invalid Organization Identification> |
      | Name                       | <Name>                                |
      | Prefix                     | <Prefix>                              |
      | FunctionGroup              | <FunctionGroup>                       |
      | Enabled                    | <Enabled>                             |
      | Domains                    | <Domains>                             |
    Then the create organization response contains
      | Message | Validation error |
    And the organization with name "<Name>" should not be created

    # Note: The validation errors are ; separated if there are multiple.
    Examples: 
      | Invalid Organization Identification | Name                | Prefix | FunctionGroup | Enabled | Domains | FaultCode       | FaultString      | FaultType       | Validation Errors                                                                                                                                                                                                                           |
      | A Test Organization                 | A Test Organization | MAA    | ADMIN         | true    | COMMON  | SOAP-ENV:Client | Validation error | ValidationError | cvc-pattern-valid: Value 'A Test Organization' is not facet-valid with respect to pattern '[^ ]{0,40}' for type 'Identification'.;cvc-type.3.1.3: The value 'A Test Organization' of element 'ns1:OrganizationIdentification' is not valid. |
      |                                     | A Test Organization | MAA    | ADMIN         | true    | COMMON  | SOAP-ENV:Client | Validation error | ValidationError | cvc-minLength-valid: Value '' with length = '0' is not facet-valid with respect to minLength '1' for type 'Identification'.;cvc-type.3.1.3: The value '' of element 'ns1:OrganizationIdentification' is not valid.                          |

  Scenario Outline: Change Data Of Organization
    Given an organization
      | OrganizationIdentification | <OrganizationIdentification> |
      | Name                       | <OldName>                    |
      | FunctionGroup              | <FunctionGroup>              |
      | Domains                    | <Domains>                    |
    When receiving an update organization request
      | OrganizationIdentification    | <OrganizationIdentification> |
      | NewOrganizationIdentification | <OrganizationIdentification> |
      | Name                          | <NewName>                    |
      | FunctionGroup                 | <FunctionGroup>              |
      | Domains                       | <Domains>                    |
    Then the update organization response is successfull
    And the organization exists
      | OrganizationIdentification | <OrganizationIdentification> |
      | Name                       | <NewName>                    |
      | PlatformFunctionGroup      | <FunctionGroup>              |
      | Domains                    | <Domains>                    |

    Examples: 
      | OrganizationIdentification | OldName             | NewName                       | FunctionGroup | Domains |
      | ATestOrganization          | A Test Organization | A Different Test Organization | ADMIN         | COMMON  |

  Scenario: Change Data Of Non Existing Organization
    When receiving an update organization request
      | OrganizationIdentification | org-test      |
      | Name                       | org-test-name |
    Then the update organization response contains
      | Message | UNKNOWN_ORGANISATION |
    And the organization with name "org-test-name" should not be changed

	# Note: Does only work if all the words 'organisation' are changed to 'organization', or it has to remain the same as it is.	
  Scenario Outline: Remove An Existing Organization
    Given an organization
      | OrganizationIdentification | <OrganizationIdentification> |
      | Name                       | <Name>                       |
      | Prefix                     | <Prefix>                     |
      | FunctionGroup              | <FunctionGroup>              |
      | Enabled                    | <Enabled>                    |
      | Domains                    | <Domains>                    |
    When receiving a remove organization request
      | OrganizationIdentification | <OrganizationIdentification> |
      | Name                       | <Name>                       |
      | FunctionGroup              | <FunctionGroup>              |
    Then the remove organization response is successfull
    And the organization with organization identification "<OrganizationIdentification>" should be disabled

    Examples: 
      | OrganizationIdentification | Name                | Prefix | FunctionGroup | Enabled | Domains |
      | ATestOrganization          | A Test Organization | MAA    | ADMIN         | true    | COMMON  |

    Scenario Outline: Remove A Non Existing Organization
    When receiving a remove organization request
      | OrganizationIdentification | <OrganizationIdentification> |
      | Name                       | <Name>                       |
      | FunctionGroup              | <FunctionGroup>              |
    Then the remove organization response contains
      | Message | UNKNOWN_ORGANISATION |

    Examples: 
      | OrganizationIdentification | Name                | FunctionGroup |
      | ATestOrganization          | A Test Organization | ADMIN         |
