package com.marketingagent.domain.magazine;

import com.marketingagent.domain.common.BaseEntity;
import com.marketingagent.domain.tenant.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.List;

/**
 * Represents an extracted story or article from a Magazine.
 */
@Entity
@Table(
        name = "stories",
        indexes = {
                @Index(name = "idx_stories_magazine", columnList = "magazine_id")
        }
)
public class Story extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "magazine_id", nullable = false)
    private Magazine magazine;

    @NotBlank
    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "summary", columnDefinition = "text")
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "keywords", columnDefinition = "jsonb")
    private List<String> keywords;

    @Column(name = "content", columnDefinition = "text")
    private String content;

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(name = "section", length = 255)
    private String section;

    @Column(name = "content_angle", length = 255)
    private String contentAngle;

    protected Story() {
    }

    public Story(Tenant tenant, Magazine magazine, String title) {
        this.tenant = tenant;
        this.magazine = magazine;
        this.title = title;
    }

    public Tenant getTenant() { return tenant; }
    public Magazine getMagazine() { return magazine; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getPageNumber() { return pageNumber; }
    public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public String getContentAngle() { return contentAngle; }
    public void setContentAngle(String contentAngle) { this.contentAngle = contentAngle; }
}
