@SmartMetering @Platform
Feature: SmartMetering Installation - Add smart meter
  As a grid operator
  I want to be able to add a smart meter

  Scenario: Add a new device
    When receiving a smartmetering add device request
      | DeviceIdentification  | TEST1024000000001                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
      | DeviceType            | SMART_METER_E                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
      | CommunicationMethod   | GPRS                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
      | CommunicationProvider | KPN                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
      | ICC_id                |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             1234 |
      | DSMR_version          | 4.2.2                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
      | Supplier              | Kaifa                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
      | HLS3_active           | false                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
      | HLS4_active           | false                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
      | HLS5_active           | true                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
      | Master_key            | 6fa7f5f19812391b2803a142f17c67aa0e3fc23b537ae6f9cd34a850d4fd5f4d60a3b2bdd6f8cb356e00e6c4e104fb5ea521eeabd8cb69d8f7a5cbe2b20e010c089ee346aaa13c9abdc5e0c9ba0fcafff53d2dcd3c1b7a8ee3c3f76e0d00fcd043940586f055c5e19a0fa7eeff6a7894e128029eaf11c1734565f3f5b614bfab9ea5ce24bf34d2e59878dc2401bd175333315ce197d4243dced9c4e28a23bc91dca432985debe81cf5912df7e99b28f596f335e80678d7b5d1edc93be8bf22d77b2e172ccd7c6907454a983999840bf540343d281e8f9871386f005fe40065fcbe218bdc605be4e759cb1b8d5760eab7b8ceb95cfae2224c15045834962f9b6b |
      | Authentication_key    | 9eab9df8169a9c22d694067435b584d573b1a57d62d491b58fd9058e994861666831fb9f5ddbf5aba9ef169256cffc8e540c34b3f92246d062889eca13639fe317e92beec86b48b14d5ef4b74682497eed7d8ea3ae6ea3dfa1877045653cb989146f826b2d97a3294a2aa22f804b1f389d0684482dde33e6cdfc51700156e3be94fc8d5b3a1302b3f3992564982e7cd7885c26fa96eeb7cab5a13d6d7fd341f665d61581dd71f652dc278823216ab75b5a430edc826021c4a2dc9de95fbdfb0e79421e2662743650690bc6b69b0b91035e96cb6396626aa1c252cddf87046dc53b9da0c8d74b517c2845b2e8eaaf72e97d41df1c4ce232e7bb082c82154e9ae5 |
      | Encryption_key        | 4e6fb5bd62d7a21f87438c04f518939cce7cfe8259ff40d9e3ff4a3a8c3befdad191eb066c8332d6d3066a2ed866774616c2b893da4543998eb57fcf35323cd2b41960e857c1a99f5cb59405081712ab23da97353014f500046756eab2620d13a269b83cbefbdfb5e275862b34dd407fd745a1bca18f1b66cb114641212579c6da03e86be2973f8dd6988b15bb6e9ef0f5637827829fc2241891c050a95ef5fc787f740a40aa2d528c69f99c76ad380bba3725929fcbe11ab72cf61e342ab95fc3b883372c110830f28144894aa2919a590822b1e594b807e86f49093982b871c658db0b6c08a90bae55c731efb3d40f245d8c0ad1478b55fa68cced3c1386a7 |
      | ManufacturerCode      | Test                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
      | ModelCode             | Test                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
    Then the add device response should be returned
      | DeviceIdentification | TEST1024000000001 |
      | Result               | OK                |
    And the dlms device with identification "TEST1024000000001" exists with device model
      | ManufacturerCode | Test |
      | ModelCode        | Test |
    And a request to the device can be performed after activation
    And the stored keys are not equal to the received keys

  Scenario: Add a new device with incorrectly encrypted keys
    When receiving a smartmetering add device request
      | DeviceIdentification  | TEST1024000000001 |
      | DeviceType            | SMART_METER_E     |
      | CommunicationMethod   | GPRS              |
      | CommunicationProvider | KPN               |
      | ICC_id                |              1234 |
      | DSMR_version          | 4.2.2             |
      | Supplier              | Kaifa             |
      | HLS3_active           | false             |
      | HLS4_active           | false             |
      | HLS5_active           | true              |
      | Master_key            | abcdef0123456789  |
      | Authentication_key    | def0123456789abc  |
      | Encryption_key        | abc0123456789def  |
    Then retrieving the AddDevice response results in an exception
    And a SOAP fault should have been returned
      | Code         |                                                           804 |
      | Message      | DECRYPTION_EXCEPTION                                          |
      | InnerMessage | Unexpected exception during decryption of E_METER_MASTER key. |
    And the dlms device with identification "TEST1024000000001" does not exist
