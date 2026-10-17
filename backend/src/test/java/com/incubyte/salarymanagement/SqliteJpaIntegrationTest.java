package com.incubyte.salarymanagement;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import jakarta.persistence.Table;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:sqlite::memory:",
    "spring.datasource.driver-class-name=org.sqlite.JDBC"
})
class SqliteJpaIntegrationTest {

    @Entity
    @Table(name = "test_entity")
    static class TestEntity {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        private String name;

        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    @Transactional
    void testJpaSqliteIntegration() {
        // Create and persist an entity
        TestEntity entity = new TestEntity();
        entity.setName("test");
        entityManager.persist(entity);
        entityManager.flush();

        // Verify the entity has an ID
        assertNotNull(entity.getId(), "Entity should have an ID after persist");

        // Retrieve the entity by ID
        TestEntity found = entityManager.find(TestEntity.class, entity.getId());
        assertNotNull(found, "Entity should be found by ID");
        assertEquals("test", found.getName(), "Entity name should match");
    }
}