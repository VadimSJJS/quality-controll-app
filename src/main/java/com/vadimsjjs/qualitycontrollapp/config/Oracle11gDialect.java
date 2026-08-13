package com.vadimsjjs.qualitycontrollapp.config;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.pagination.LegacyOracleLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;

public class Oracle11gDialect extends OracleDialect {

    public Oracle11gDialect() {
        super(DatabaseVersion.make(11, 2));
    }

    @Override
    public LimitHandler getLimitHandler() {
        return new LegacyOracleLimitHandler(DatabaseVersion.make(11, 2));
    }
}
