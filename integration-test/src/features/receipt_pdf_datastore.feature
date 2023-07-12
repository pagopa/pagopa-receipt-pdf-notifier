Feature: All about payment events consumed by Azure functions receipt-pdf-notifier

  Scenario: a receipt stored on biz-events datastore is stored into receipts datastore is notified to a io user
    Given a random receipt with id "receipt-datastore-test-id-1" stored on receipt datastore with generated pdf
    When receipt has been properly stored into receipt datastore after 10000 ms with eventId "receipt-datastore-test-id-1"
    Then the receipt has not the status "GENERATED"
    And the receipt has not the status "UNABLE_TO_SEND"
