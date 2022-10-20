package com.yugabyte.app.messenger.data.repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.springframework.stereotype.Repository;

@Repository
public class SessionManagementRepository {
    @PersistenceContext
    EntityManager entityManager;

    public void switchToReadWriteTxMode() {
        Query query = entityManager.createNativeQuery(
                "set transaction read write;");
        query.executeUpdate();
    }

    public void switchToReadOnlyTxMode() {
        Query query = entityManager.createNativeQuery(
                "set transaction read only;");
        query.executeUpdate();
    }
}
