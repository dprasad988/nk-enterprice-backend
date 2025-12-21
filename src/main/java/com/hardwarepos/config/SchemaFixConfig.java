package com.hardwarepos.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class SchemaFixConfig implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(SchemaFixConfig.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        logger.info("Checking for stale unique constraints...");

        // Constraint name from the user's error log
        dropIndexIfExists("product", "UK_44c6umvphppa3226vhmagmviu"); 
        
        // Also try to drop the SKU constraint if it exists (usually it has a similar random name, 
        // but since we don't know it exactly, we might need to look it up or just warn. 
        // However, standard hibernate naming often uses column name if not specified.
        // Let's try dropping by column name default index name just in case? 
        // The error log showed a specific hash. Let's rely on that first.
        
        // Note: For SKU, if it hits, we might need another fix. 
        // But the user specifically complained about barcode '123'.
    }

    private void dropIndexIfExists(String tableName, String indexName) {
        try {
            logger.info("Attempting to drop index {} from table {}...", indexName, tableName);
            jdbcTemplate.execute("ALTER TABLE " + tableName + " DROP INDEX " + indexName);
            logger.info("Successfully dropped index: {}", indexName);
        } catch (Exception e) {
            if (e.getMessage().contains("check that column/key exists")) {
                 logger.info("Index {} does not exist (already removed).", indexName);
            } else {
                 logger.warn("Could not drop index {}. It might not exist or another error occurred: {}", indexName, e.getMessage());
            }
        }
    }
}
