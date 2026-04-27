package com.example.kitchensink.migrations;

import com.example.kitchensink.domain.Member;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

@ChangeUnit(id = "init-schema", order = "001", author = "modernization-factory")
public class InitMigration {

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        mongoTemplate.indexOps(Member.class)
                .ensureIndex(new Index("email", Sort.Direction.ASC).unique());
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        mongoTemplate.indexOps(Member.class).dropIndex("email");
    }
}
