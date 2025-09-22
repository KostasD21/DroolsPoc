package com.example.drools_poc.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "TAX_RULE")
public class TaxRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String xmlContent;

    public TaxRule(String xmlContent) {
        this.xmlContent = xmlContent;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getXmlContent() {
        return xmlContent;
    }

    public void setXmlContent(String xmlContent) {
        this.xmlContent = xmlContent;
    }
}
