/*
 * MIT License
 *
 * Copyright (c) 2016 EPAM Systems
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.epam.catgenome.entity.gene;

import java.util.List;
import java.util.Map;

import com.epam.catgenome.entity.protein.ProteinSequence;
import com.epam.catgenome.entity.track.Block;
import com.epam.catgenome.manager.gene.parser.GeneFeature;
import com.epam.catgenome.manager.gene.parser.StrandSerializable;

/**
 * Created: 3/21/2016
 * Project: CATGenome Browser
 *
 * <p>
 * A value object, representing lower lvers of gene hierarchy
 * </p>
 */
public class GeneLowLevel extends Block {
    private String feature;
    private StrandSerializable strand;
    private Map<String, String> attributes;
    private List<GeneLowLevel> items;
    private List<ProteinSequence> psList;

    private Long exonsCount;
    private Long aminoacidLength;
    private Boolean mapped;
    private boolean canonical;

    public GeneLowLevel() {
        // no-op
    }

    public GeneLowLevel(final GeneFeature geneFeature) {
        setStartIndex(geneFeature.getStart());
        setEndIndex(geneFeature.getEnd());

        feature = geneFeature.getFeature();
        strand = geneFeature.getStrand();
        attributes = geneFeature.getAttributes();
    }

    public GeneLowLevel(final GeneHighLevel geneHighLevel) {
        setStartIndex(geneHighLevel.getStartIndex());
        setEndIndex(geneHighLevel.getEndIndex());

        feature = geneHighLevel.getFeature();
        strand = geneHighLevel.getStrand();
        attributes = geneHighLevel.getAttributes();
        mapped = geneHighLevel.getMapped();
    }

    public GeneLowLevel(final Gene gene) {
        setStartIndex(gene.getStartIndex());
        setEndIndex(gene.getEndIndex());

        feature = gene.getFeature();
        strand = gene.getStrand();
        attributes = gene.getAttributes();
        exonsCount = gene.getExonsCount();
        aminoacidLength = gene.getAminoacidLength();
        mapped = gene.isMapped();
        canonical = gene.isCanonical();
    }

    public String getFeature() {
        return feature;
    }

    public StrandSerializable getStrand() {
        return strand;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public List<GeneLowLevel> getItems() {
        return items;
    }

    public void setFeature(final String feature) {
        this.feature = feature;
    }

    public void setStrand(final StrandSerializable strand) {
        this.strand = strand;
    }

    public void setAttributes(final Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public void setItems(final List<GeneLowLevel> items) {
        this.items = items;
    }

    public List<ProteinSequence> getPsList() {
        return psList;
    }

    public void setPsList(List<ProteinSequence> psList) {
        this.psList = psList;
    }

    public Long getExonsCount() {
        return exonsCount;
    }

    public void setExonsCount(Long exonsCount) {
        this.exonsCount = exonsCount;
    }

    public Long getAminoacidLength() {
        return aminoacidLength;
    }

    public void setAminoacidLength(Long aminoacidLength) {
        this.aminoacidLength = aminoacidLength;
    }

    public Boolean getMapped() {
        return mapped;
    }

    public void setMapped(Boolean mapped) {
        this.mapped = mapped;
    }

    public boolean isCanonical() {
        return canonical;
    }

    public void setCanonical(boolean canonical) {
        this.canonical = canonical;
    }
}
