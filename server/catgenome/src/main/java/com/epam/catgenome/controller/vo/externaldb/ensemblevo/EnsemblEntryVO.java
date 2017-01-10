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

package com.epam.catgenome.controller.vo.externaldb.ensemblevo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Source: EnsemblEntryVO
 * Created: 3.02.16 
 * Project: CATGenome Browser 
 * Make: Eclipse Mars.1, JDK 1.8
 *
 * <p>
 * class for extarnale DB data
 * </p>
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnsemblEntryVO extends EnsemblFullBaseVO {

    @JsonProperty(value = "description")
    private String description;

    @JsonProperty(value = "feature_type")
    private String featureType;

    @JsonProperty(value = "alleles")
    private String [] alleles;

    @JsonProperty(value = "Transcript")
    private EnsemblTranscriptVO[] transcript;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public EnsemblTranscriptVO[] getTranscript() {
        return transcript;
    }

    public void setTranscript(EnsemblTranscriptVO[] transcript) {
        this.transcript = transcript;
    }

    public String getFeatureType() {
        return featureType;
    }

    public void setFeatureType(String featureType) {
        this.featureType = featureType;
    }

    public String[] getAlleles() {
        return alleles;
    }

    public void setAlleles(String[] alleles) {
        this.alleles = alleles;
    }
    @Override
    public String toString() {
        return "EnsemblEntryVO{" +
                "source='" + getSource() + '\'' +
                ", objectType='" + getObjectType() + '\'' +
                ", logicName='" + getLogicName() + '\'' +
                ", version=" + getVersion() +
                ", species='" + getSpecies() + '\'' +
                ", description='" + description + '\'' +
                ", displayName='" + getDisplayName() + '\'' +
                ", assemblyName='" + getAssemblyName() + '\'' +
                ", biotype='" + getBioType() + '\'' +
                ", start='" + getStart() + '\'' +
                ", end='" + getEnd() + '\'' +
                ", seqRegionName='" + getSeqRegionName() + '\'' +
                ", dbType='" + getDbType() + '\'' +
                ", strand='" + getStrand() + '\'' +
                ", id='" + getId() + '\'' +
                ", transcript='" + transcript + '\'' +
                '}';
    }

}
