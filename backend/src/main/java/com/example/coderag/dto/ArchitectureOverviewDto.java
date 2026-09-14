package com.example.coderag.dto;

import com.example.coderag.model.ArchitectureOverview;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ArchitectureOverviewDto {

    private UUID id;
    private UUID repositoryId;
    private String commitSha;
    private String overviewText;
    private List<String> technologies;
    private List<String> modules;
    private OffsetDateTime createdAt;

    public ArchitectureOverviewDto() {
    }

    public ArchitectureOverviewDto(UUID id, UUID repositoryId, String commitSha, String overviewText, List<String> technologies, List<String> modules, OffsetDateTime createdAt) {
        this.id = id;
        this.repositoryId = repositoryId;
        this.commitSha = commitSha;
        this.overviewText = overviewText;
        this.technologies = technologies;
        this.modules = modules;
        this.createdAt = createdAt;
    }

    public static ArchitectureOverviewDto fromEntity(ArchitectureOverview entity) {
        if (entity == null) {
            return null;
        }

        List<String> techs = (entity.getTechnologies() != null && !entity.getTechnologies().isBlank())
                ? Arrays.stream(entity.getTechnologies().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList())
                : Collections.emptyList();

        List<String> mods = (entity.getModules() != null && !entity.getModules().isBlank())
                ? Arrays.stream(entity.getModules().split("\n"))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList())
                : Collections.emptyList();

        return new ArchitectureOverviewDto(
                entity.getId(),
                entity.getRepositoryId(),
                entity.getCommitSha(),
                entity.getOverviewText(),
                techs,
                mods,
                entity.getCreatedAt()
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private UUID repositoryId;
        private String commitSha;
        private String overviewText;
        private List<String> technologies;
        private List<String> modules;
        private OffsetDateTime createdAt;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder repositoryId(UUID repositoryId) {
            this.repositoryId = repositoryId;
            return this;
        }

        public Builder commitSha(String commitSha) {
            this.commitSha = commitSha;
            return this;
        }

        public Builder overviewText(String overviewText) {
            this.overviewText = overviewText;
            return this;
        }

        public Builder technologies(List<String> technologies) {
            this.technologies = technologies;
            return this;
        }

        public Builder modules(List<String> modules) {
            this.modules = modules;
            return this;
        }

        public Builder createdAt(OffsetDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public ArchitectureOverviewDto build() {
            return new ArchitectureOverviewDto(id, repositoryId, commitSha, overviewText, technologies, modules, createdAt);
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(UUID repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getCommitSha() {
        return commitSha;
    }

    public void setCommitSha(String commitSha) {
        this.commitSha = commitSha;
    }

    public String getOverviewText() {
        return overviewText;
    }

    public void setOverviewText(String overviewText) {
        this.overviewText = overviewText;
    }

    public List<String> getTechnologies() {
        return technologies;
    }

    public void setTechnologies(List<String> technologies) {
        this.technologies = technologies;
    }

    public List<String> getModules() {
        return modules;
    }

    public void setModules(List<String> modules) {
        this.modules = modules;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
