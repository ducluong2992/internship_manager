package com.qlnv.modules.ai.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ai_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Builder.Default
    private String provider = "Google Gemini";

    @Column(name = "api_key")
    private String apiKey;

    @Builder.Default
    @Column(name = "chat_model")
    private String chatModel = "gemini-2.5-flash";

    @Builder.Default
    @Column(name = "embedding_model")
    private String embeddingModel = "text-embedding-004";

    @Builder.Default
    @Column(name = "top_k")
    private Integer topK = 5;

    @Builder.Default
    @Column(name = "chunk_size")
    private Integer chunkSize = 1000;

    @Builder.Default
    private Integer overlap = 150;

    @Builder.Default
    private Float temperature = 0.2f;
}
