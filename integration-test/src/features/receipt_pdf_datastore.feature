Feature: All about payment events consumed by Azure functions receipt-pdf-notifier

  Scenario: a receipt stored on receipt datastore is notified to a io user
    Given a random receipt with id "receipt-notifier-test-id-1" stored on receipt datastore with generated pdf and status GENERATED
    When receipt has been properly stored into receipt datastore after 10000 ms with eventId "receipt-notifier-test-id-1"
    Then the receipt has not the status "GENERATED"
    And the receipt has not the status "UNABLE_TO_SEND"

  Scenario: a receipt to notify to io user is retried after on error queue
    Given a random receipt with id "receipt-notifier-test-id-2" stored on receipt datastore with generated pdf and status GENERATED
    And a random receipt with id "receipt-notifier-test-id-2" enqueued on notification error queue
    When receipt has been properly enqueued into error queue after 10000 ms with eventId "receipt-notifier-test-id-2"
    Then the receipt has not the status "GENERATED"
    And the receipt has not the status "UNABLE_TO_SEND"