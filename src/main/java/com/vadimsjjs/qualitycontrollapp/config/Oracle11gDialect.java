package com.vadimsjjs.qualitycontrollapp.config;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.pagination.LegacyOracleLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.query.sqm.FetchClauseType;

/**
 * Диалект Hibernate для Oracle 11g.
 *
 * <p>Нужен потому, что в Oracle 11g нет синтаксиса OFFSET/FETCH: Hibernate должен
 * генерировать ограничение строк через ROWNUM. Указывается в application.properties:
 * spring.jpa.properties.hibernate.dialect=...config.Oracle11gDialect
 *
 * <p>Важно: в Hibernate 6 постраничная выборка (в т.ч. setMaxResults(1) для
 * existsBy...) рендерит «fetch first ? rows only» напрямую, если
 * {@link #supportsFetchClause(FetchClauseType)} возвращает true. Наш родительский
 * OracleDialect всегда отвечает true, из-за чего на Oracle 11g возникала
 * ошибка ORA-00933 (неверное завершение SQL-предложения). Поэтому этот метод
 * переопределён на false — тогда Hibernate использует getLimitHandler() с ROWNUM.
 */
public class Oracle11gDialect extends OracleDialect {

    public Oracle11gDialect() {
        super(DatabaseVersion.make(11, 2));
    }

    @Override
    public LimitHandler getLimitHandler() {
        return new LegacyOracleLimitHandler(DatabaseVersion.make(11, 2));
    }

    /**
     * Говорим Hibernate, что FETCH FIRST ... ROWS ONLY не поддерживается (Oracle 11g),
     * чтобы лимит рендерился через ROWNUM, а не через FETCH FIRST.
     */
    @Override
    public boolean supportsFetchClause(FetchClauseType type) {
        return false;
    }
}
