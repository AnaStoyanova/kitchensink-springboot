package com.example.kitchensink.migrations;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

@ChangeUnit(id = "add-legacyId-index", order = "002", author = "modernization-factory")
public class LegacyIdMigration {

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        mongoTemplate.indexOps("members")
                .createIndex(new Index().on("legacyId", Sort.Direction.ASC).unique().sparse());
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        mongoTemplate.indexOps("members").dropIndex("legacyId");
    }
}
