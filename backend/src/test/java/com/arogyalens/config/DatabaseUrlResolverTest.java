package com.arogyalens.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseUrlResolverTest {

    @Test
    void convertsRenderStyleUrlAndRequiresTls() {
        var s = DatabaseUrlResolver.toJdbc("postgresql://app:p%40ss@dpg-x.oregon-postgres.render.com/arogya", null).orElseThrow();
        assertThat(s.url()).isEqualTo("jdbc:postgresql://dpg-x.oregon-postgres.render.com:5432/arogya?sslmode=require");
        assertThat(s.username()).isEqualTo("app");
        assertThat(s.password()).isEqualTo("p@ss");
    }

    @Test
    void keepsExistingQueryAndExplicitSslMode() {
        assertThat(DatabaseUrlResolver.toJdbc("postgres://u:p@h:6543/db?sslmode=verify-full", "disable")
                .orElseThrow().url()).isEqualTo("jdbc:postgresql://h:6543/db?sslmode=verify-full");
        assertThat(DatabaseUrlResolver.toJdbc("postgres://u:p@h/db?application_name=a", "prefer")
                .orElseThrow().url()).isEqualTo("jdbc:postgresql://h:5432/db?application_name=a&sslmode=prefer");
    }

    @Test
    void emptyOrJdbcUrls() {
        assertThat(DatabaseUrlResolver.toJdbc(" ", null)).isEmpty();
        assertThat(DatabaseUrlResolver.toJdbc("jdbc:postgresql://h/db", null).orElseThrow().username()).isNull();
        assertThatThrownBy(() -> DatabaseUrlResolver.toJdbc("mysql://h/db", null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
