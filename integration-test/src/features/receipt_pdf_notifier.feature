Feature: All about payment events consumed by Azure functions receipt-pdf-notifier

  Scenario: a receipt stored on receipt datastore is notified to a io user
    Given a random receipt with id "receipt-notifier-test-id-1" stored on receipt datastore with generated pdf and status GENERATED
    When receipt has been properly notified after 10000 ms
    Then the receipt has not the status "GENERATED"
    And the receipt has not the status "UNABLE_TO_SEND"
    And the receipt has the status "NOT_TO_NOTIFY"

  Scenario: a receipt to notify to io user is retried after on error queue
    Given a random receipt with id "receipt-notifier-test-id-2" enqueued on notification error queue
    When receipt has been properly notified after 30000 ms
    Then the receipt has not the status "GENERATED"
    And the receipt has not the status "UNABLE_TO_SEND"
    And the receipt has the status "NOT_TO_NOTIFY"

  Scenario: a cart receipt stored on cart receipt datastore is notified to a io user
    Given a random cart receipt with id "receipt-notifier-test-id-3" stored on cart receipt datastore with generated pdf and status GENERATED
    When cart receipt has been properly notified after 10000 ms
    Then the cart receipt has not the status "GENERATED"
    And the cart receipt has not the status "UNABLE_TO_SEND"
    And the cart receipt has the status "NOT_TO_NOTIFY"

  Scenario: a cart receipt to notify to io user is retried after on error queue
    Given a random cart receipt with id "receipt-notifier-test-id-4" enqueued on notification error queue for cart
    When cart receipt has been properly notified after 30000 ms
    Then the cart receipt has not the status "GENERATED"
    And the cart receipt has not the status "UNABLE_TO_SEND"
    And the cart receipt has the status "NOT_TO_NOTIFY"