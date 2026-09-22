package com.aishare.knowledgerag.persistence;

import com.aishare.knowledgerag.prompt.PromptTemplate;
import com.aishare.knowledgerag.prompt.PromptTemplateRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JdbcPromptTemplateRepository implements PromptTemplateRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcPromptTemplateRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<PromptTemplate> findActive(String promptKey) {
        return jdbcTemplate.query("""
                        SELECT prompt_key, version, content, checksum
                        FROM prompt_template
                        WHERE prompt_key = :promptKey AND active = TRUE
                        """,
                new MapSqlParameterSource("promptKey", promptKey),
                (resultSet, rowNumber) -> new PromptTemplate(
                        resultSet.getString("prompt_key"),
                        resultSet.getString("version"),
                        resultSet.getString("content"),
                        resultSet.getString("checksum")
                )
        ).stream().findFirst();
    }
}
