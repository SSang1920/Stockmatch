package com.stockmatch.stock.importer;

import com.stockmatch.stock.domain.Exchange;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "import_file",
        uniqueConstraints = @UniqueConstraint(name = "uk_import_file_location", columnNames = "location"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
@Builder
public class ImportFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Exchange exchange;

    @Column(nullable = false, length = 64)
    private String checksum;

    @Builder.Default
    private Integer processed = 0;
    @Builder.Default
    private Integer success = 0;
    @Builder.Default
    private Integer skipped = 0;
    @Builder.Default
    private Integer error = 0;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime importedAt = LocalDateTime.now();

    public void markImported(String newChecksum, int processed, int success, int skipped, int error) {
        this.checksum = newChecksum;
        this.processed = processed;
        this.success = success;
        this.skipped = skipped;
        this.error = error;
        this.importedAt = LocalDateTime.now();
    }
}
