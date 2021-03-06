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
 * The variant data import request.
 *
 * <p> This is the Java data model class that specifies how to parse/serialize into the JSON that is
 * transmitted over HTTP when working with the Genomics API. For a detailed explanation see:
 * <a href="http://code.google.com/p/google-http-java-client/wiki/JSON">http://code.google.com/p/google-http-java-client/wiki/JSON</a>
 * </p>
 *
 */
@SuppressWarnings("javadoc")
public final class ImportVariantsRequest extends com.google.api.client.json.GenericJson {

  /**
   * Required. The dataset to which variant data should be imported.
   * The value may be {@code null}.
   */
  @com.google.api.client.util.Key
  private java.lang.String datasetId;

  /**
   * A list of URIs pointing at VCF files in Google Cloud Storage. See the VCF Specification for
   * more details on the input format.
   * The value may be {@code null}.
   */
  @com.google.api.client.util.Key
  private java.util.List<java.lang.String> sourceUris;

  /**
   * Required. The dataset to which variant data should be imported.
   * @return value or {@code null} for none
   */
  public java.lang.String getDatasetId() {
    return datasetId;
  }

  /**
   * Required. The dataset to which variant data should be imported.
   * @param datasetId datasetId or {@code null} for none
   */
  public ImportVariantsRequest setDatasetId(java.lang.String datasetId) {
    this.datasetId = datasetId;
    return this;
  }

  /**
   * A list of URIs pointing at VCF files in Google Cloud Storage. See the VCF Specification for
   * more details on the input format.
   * @return value or {@code null} for none
   */
  public java.util.List<java.lang.String> getSourceUris() {
    return sourceUris;
  }

  /**
   * A list of URIs pointing at VCF files in Google Cloud Storage. See the VCF Specification for
   * more details on the input format.
   * @param sourceUris sourceUris or {@code null} for none
   */
  public ImportVariantsRequest setSourceUris(java.util.List<java.lang.String> sourceUris) {
    this.sourceUris = sourceUris;
    return this;
  }

  @Override
  public ImportVariantsRequest set(String fieldName, Object value) {
    return (ImportVariantsRequest) super.set(fieldName, value);
  }

  @Override
  public ImportVariantsRequest clone() {
    return (ImportVariantsRequest) super.clone();
  }

}
