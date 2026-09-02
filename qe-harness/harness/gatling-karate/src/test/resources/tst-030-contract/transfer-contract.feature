Feature: transfer contract compatibility

  Background:
    * url baseUrl

  Scenario: v2 response satisfies its published schema
    Given path 'v2', 'transfers'
    And request { from: 'ACC-000001', to: 'ACC-000002', amountMinor: 500 }
    When method post
    Then status 201
    And match response == { transferRef: '#uuid', status: '#string', settledAt: '#string' }

  Scenario: v1 remains backward compatible
    Given path 'v1', 'transfers'
    And request { from: 'ACC-000001', to: 'ACC-000002', amountMinor: 500 }
    When method post
    Then status 201
    And match response contains { transferRef: '#uuid', status: '#string' }
