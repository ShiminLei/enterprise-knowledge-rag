package com.aishare.knowledgerag.persistence;

import com.aishare.knowledgerag.prompt.PromptTemplate;
import com.aishare.knowledgerag.prompt.PromptTemplateRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcPromptTemplateRepository implements PromptTemplateRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcPromptTemplateRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<PromptTemplate> findActive(String promptKey) {
        return jdbcTemplate.query("""
                        SELECT prompt_key, version, content, checksum,
                               active, created_by, created_at
                        FROM prompt_template
                        WHERE prompt_key = :promptKey AND active = TRUE
                        """,
                new MapSqlParameterSource("promptKey", promptKey),
                this::mapRow
        ).stream().findFirst();
    }

    @Override
    public Optional<PromptTemplate> findByVersion(String promptKey, String version) {
        return jdbcTemplate.query("""
                        SELECT prompt_key, version, content, checksum,
                               active, created_by, created_at
                        FROM prompt_template
                        WHERE prompt_key = :promptKey AND version = :version
                        """,
                new MapSqlParameterSource()
                        .addValue("promptKey", promptKey)
                        .addValue("version", version),
                this::mapRow
        ).stream().findFirst();
    }

    @Override
    public List<PromptTemplate> findAll(String promptKey) {
        return jdbcTemplate.query("""
                        SELECT prompt_key, version, content, checksum,
                               active, created_by, created_at
                        FROM prompt_template
                        WHERE prompt_key = :promptKey
                        ORDER BY created_at DESC, version DESC
                        """,
                new MapSqlParameterSource("promptKey", promptKey),
                this::mapRow
        );
    }

    @Override
    public void lockVersions(String promptKey) {
        jdbcTemplate.query("""
                        SELECT id
                        FROM prompt_template
                        WHERE prompt_key = :promptKey
                        FOR UPDATE
                        """,
                new MapSqlParameterSource("promptKey", promptKey),
                (resultSet, rowNumber) -> resultSet.getObject("id", UUID.class)
        );
    }

    @Override
    public void insert(PromptTemplate template) {
        jdbcTemplate.update("""
                        INSERT INTO prompt_template (
                            prompt_key, version, content, checksum,
                            active, created_by, created_at
                        ) VALUES (
                            :promptKey, :version, :content, :checksum,
                            :active, :createdBy, :createdAt
                        )
                        """,
                new MapSqlParameterSource()
                        .addValue("promptKey", template.key())
                        .addValue("version", template.version())
                        .addValue("content", template.content())
                        .addValue("checksum", template.checksum())
                        .addValue("active", template.active())
                        .addValue("createdBy", template.createdBy())
                        .addValue("createdAt", template.createdAt())
        );
    }

    @Override
    public void deactivateAll(String promptKey) {
        jdbcTemplate.update("""
                        UPDATE prompt_template
                        SET active = FALSE
                        WHERE prompt_key = :promptKey AND active = TRUE
                        """,
                new MapSqlParameterSource("promptKey", promptKey)
        );
    }

    @Override
    public int activate(String promptKey, String version) {
        return jdbcTemplate.update("""
                        UPDATE prompt_template
                        SET active = TRUE
                        WHERE prompt_key = :promptKey AND version = :version
                        """,
                new MapSqlParameterSource()
                        .addValue("promptKey", promptKey)
                        .addValue("version", version)
        );
    }

    private PromptTemplate mapRow(
            java.sql.ResultSet resultSet,
            int rowNumber
    ) throws java.sql.SQLException {
        return new PromptTemplate(
                resultSet.getString("prompt_key"),
                resultSet.getString("version"),
                resultSet.getString("content"),
                resultSet.getString("checksum"),
                resultSet.getBoolean("active"),
                resultSet.getString("created_by"),
                resultSet.getTimestamp("created_at").toInstant()
        );
    }
}
