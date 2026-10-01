package com.vadimsjjs.qualitycontrollapp.config;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.pagination.LegacyOracleLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;

/**
 * Диалект Hibernate для Oracle 11g.
 *
 * <p>Нужен потому, что в Oracle 11g нет OFFSET/FETCH: Hibernate должен генерировать
 * постраничную выборку через ROWNUM. Указывается в application.properties:
 * spring.jpa.database-platform=...config.Oracle11gDialect
 */
public class Oracle11gDialect extends OracleDialect {

    public Oracle11gDialect() {
        super(DatabaseVersion.make(11, 2));
    }

    @Override
    public LimitHandler getLimitHandler() {
        return new LegacyOracleLimitHandler(DatabaseVersion.make(11, 2));
    }
}
