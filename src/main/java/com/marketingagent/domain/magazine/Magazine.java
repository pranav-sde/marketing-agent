package com.marketingagent.domain.magazine;

import com.marketingagent.domain.common.BaseEntity;
import com.marketingagent.domain.tenant.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Represents an uploaded magazine PDF. Each magazine goes through a processing
 * pipeline: UPLOADED → EXTRACTING → PROCESSED (or FAILED).
 */
@Entity
@Table(
        name = "magazines",
        indexes = {
                @Index(name = "idx_magazines_tenant_status", columnList = "tenant_id,processing_status"),
                @Index(name = "idx_magazines_upload_date", columnList = "tenant_id,created_at")
        }
)
public class Magazine extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @NotBlank
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @NotBlank
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "extracted_text", columnDefinition = "text")
    private String extractedText;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 50)
    private MagazineStatus processingStatus = MagazineStatus.UPLOADED;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    protected Magazine() {
    }

    public Magazine(Tenant tenant, String title, String filePath) {
        this.tenant = tenant;
        this.title = title;
        this.filePath = filePath;
    }

    public Tenant getTenant() { return tenant; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getFilePath() { return filePath; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public String getExtractedText() { return extractedText; }
    public void setExtractedText(String extractedText) { this.extractedText = extractedText; }

    public MagazineStatus getProcessingStatus() { return processingStatus; }
    public void setProcessingStatus(MagazineStatus processingStatus) { this.processingStatus = processingStatus; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
