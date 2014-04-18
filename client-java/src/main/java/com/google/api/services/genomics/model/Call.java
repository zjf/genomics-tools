/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
/*
 * This code was generated by https://code.google.com/p/google-apis-client-generator/
 * Modify at your own risk.
 */

package com.google.api.services.genomics.model;

/**
 * A Call represents the determination of genotype with respect to a particular variant. It may
 * include associated information such as quality and phasing. For example, a Call might assign a
 * probability of 0.32 to the occurrence of a SNP named rs1234 in a callset with the name NA12345.
 *
 * <p> This is the Java data model class that specifies how to parse/serialize into the JSON that is
 * transmitted over HTTP when working with the Genomics API. For a detailed explanation see:
 * <a href="http://code.google.com/p/google-http-java-client/wiki/JSON">http://code.google.com/p/google-http-java-client/wiki/JSON</a>
 * </p>
 *
 */
@SuppressWarnings("javadoc")
public final class Call extends com.google.api.client.json.GenericJson {

  /**
   * The ID of the callset this variant call belongs to.
   * The value may be {@code null}.
   */
  @com.google.api.client.util.Key
  private java.lang.String callsetId;

  /**
   * The name of the callset this variant call belongs to.
   * The value may be {@code null}.
   */
  @com.google.api.client.util.Key
  private java.lang.String callsetName;

  /**
   * The genotype of this variant call. Each value represents either the value of the referenceBases
   * field or is a 1-based index into alternateBases. If a variant had a referenceBases field of
   * "T", an alternateBases value of ["A", "C"], and the genotype was [2, 1], that would mean the
   * call represented the heterozygous value "CA" for this variant. If the genotype was instead [0,
   * 1] the represented value would be "TA". Ordering of the genotype values is important if the
   * phaseset field is present.
   * The value may be {@code null}.
   */
  @com.google.api.client.util.Key @com.google.api.client.json.JsonString
  private java.util.List<java.lang.Long> genotype;

  /**
   * The genotype likelihoods for this variant call. Each array entry represents how likely a
   * specific genotype is for this call. The value ordering is defined by the GL tag in the VCF
   * spec.
   * The value may be {@code null}.
   */
  @com.google.api.client.util.Key
  private java.util.List<java.lang.Double> genotypeLikelihood;

  /**
   * A map of additional variant call information.
   * The value may be {@code null}.
   */
  @com.google.api.client.util.Key
  private java.util.Map<String, java.util.List<java.lang.String>> info;

  /**
   * If this field is present, this variant call's genotype ordering implies the phase of the bases
   * and is consistent with any other variant calls on the same contig which have the same phaseset
   * value.
   * The value may be {@code null}.
   */
  @com.google.api.client.util.Key
  private java.lang.String phaseset;

  /**
   * The ID of the callset this variant call belongs to.
   * @return value or {@code null} for none
   */
  public java.lang.String getCallsetId() {
    return callsetId;
  }

  /**
   * The ID of the callset this variant call belongs to.
   * @param callsetId callsetId or {@code null} for none
   */
  public Call setCallsetId(java.lang.String callsetId) {
    this.callsetId = callsetId;
    return this;
  }

  /**
   * The name of the callset this variant call belongs to.
   * @return value or {@code null} for none
   */
  public java.lang.String getCallsetName() {
    return callsetName;
  }

  /**
   * The name of the callset this variant call belongs to.
   * @param callsetName callsetName or {@code null} for none
   */
  public Call setCallsetName(java.lang.String callsetName) {
    this.callsetName = callsetName;
    return this;
  }

  /**
   * The genotype of this variant call. Each value represents either the value of the referenceBases
   * field or is a 1-based index into alternateBases. If a variant had a referenceBases field of
   * "T", an alternateBases value of ["A", "C"], and the genotype was [2, 1], that would mean the
   * call represented the heterozygous value "CA" for this variant. If the genotype was instead [0,
   * 1] the represented value would be "TA". Ordering of the genotype values is important if the
   * phaseset field is present.
   * @return value or {@code null} for none
   */
  public java.util.List<java.lang.Long> getGenotype() {
    return genotype;
  }

  /**
   * The genotype of this variant call. Each value represents either the value of the referenceBases
   * field or is a 1-based index into alternateBases. If a variant had a referenceBases field of
   * "T", an alternateBases value of ["A", "C"], and the genotype was [2, 1], that would mean the
   * call represented the heterozygous value "CA" for this variant. If the genotype was instead [0,
   * 1] the represented value would be "TA". Ordering of the genotype values is important if the
   * phaseset field is present.
   * @param genotype genotype or {@code null} for none
   */
  public Call setGenotype(java.util.List<java.lang.Long> genotype) {
    this.genotype = genotype;
    return this;
  }

  /**
   * The genotype likelihoods for this variant call. Each array entry represents how likely a
   * specific genotype is for this call. The value ordering is defined by the GL tag in the VCF
   * spec.
   * @return value or {@code null} for none
   */
  public java.util.List<java.lang.Double> getGenotypeLikelihood() {
    return genotypeLikelihood;
  }

  /**
   * The genotype likelihoods for this variant call. Each array entry represents how likely a
   * specific genotype is for this call. The value ordering is defined by the GL tag in the VCF
   * spec.
   * @param genotypeLikelihood genotypeLikelihood or {@code null} for none
   */
  public Call setGenotypeLikelihood(java.util.List<java.lang.Double> genotypeLikelihood) {
    this.genotypeLikelihood = genotypeLikelihood;
    return this;
  }

  /**
   * A map of additional variant call information.
   * @return value or {@code null} for none
   */
  public java.util.Map<String, java.util.List<java.lang.String>> getInfo() {
    return info;
  }

  /**
   * A map of additional variant call information.
   * @param info info or {@code null} for none
   */
  public Call setInfo(java.util.Map<String, java.util.List<java.lang.String>> info) {
    this.info = info;
    return this;
  }

  /**
   * If this field is present, this variant call's genotype ordering implies the phase of the bases
   * and is consistent with any other variant calls on the same contig which have the same phaseset
   * value.
   * @return value or {@code null} for none
   */
  public java.lang.String getPhaseset() {
    return phaseset;
  }

  /**
   * If this field is present, this variant call's genotype ordering implies the phase of the bases
   * and is consistent with any other variant calls on the same contig which have the same phaseset
   * value.
   * @param phaseset phaseset or {@code null} for none
   */
  public Call setPhaseset(java.lang.String phaseset) {
    this.phaseset = phaseset;
    return this;
  }

  @Override
  public Call set(String fieldName, Object value) {
    return (Call) super.set(fieldName, value);
  }

  @Override
  public Call clone() {
    return (Call) super.clone();
  }

}
