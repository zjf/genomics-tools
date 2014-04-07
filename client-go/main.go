// Copyright 2014 Google Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
)

type GenomicsApi struct {
	BaseUrl       string
	NextPageToken string
	/* Perhaps an OAuth2 bearer token too
	Oauth2Token string */
}

type GenomicsApiReadResource struct {
	Id                        int
	Name                      string
	ReadsetId                 string
	Flags                     int
	ReferenceSequenceName     string
	Position                  int
	MappingQuality            int
	Cigar                     string
	MateReferenceSequenceName string
	MatePosition              int
	TemplateLength            int
	OriginalBases             string
	AlignedBases              string
	BaseQuality               string
	Tags                      []map[string][]string
}

type GenomicsApiReadsSearchResponse struct {
	Reads         []GenomicsApiReadResource
	NextPageToken string
}

func (api *GenomicsApi) invoke(
	endpoint string,
	body []byte,
	returnValue interface{}) error {

	requestUrl := api.BaseUrl + endpoint
	request, err := http.NewRequest("POST", requestUrl, bytes.NewBuffer(body))
	if err != nil {
		return err
	}
	request.Header.Set("Content-type", "application/json")

	client := &http.Client{}
	response, err := client.Do(request)
	if err != nil {
		return err
	}

	answerBytes, err := ioutil.ReadAll(response.Body)
	response.Body.Close()
	if err != nil {
		return err
	}

	err = json.Unmarshal(answerBytes, returnValue)
	if err != nil {
		return err
	}

	return nil
}

func (api *GenomicsApi) ReadsSearch(
	datasetIds []string,
	readsetIds []string,
	sequenceName string,
	sequenceStart, sequenceEnd int) (*GenomicsApiReadsSearchResponse, error) {

	body, _ := json.Marshal(map[string]interface{}{
		"datasetIds":    datasetIds,
		"readsetIds":    readsetIds,
		"sequenceName":  sequenceName,
		"sequenceStart": sequenceStart,
		"sequenceEnd":   sequenceEnd,
		"pageToken":     api.NextPageToken,
	})

	answer := new(GenomicsApiReadsSearchResponse)
	err := api.invoke("/reads/search", body, answer)
	if err != nil {
		return nil, err
	}

	api.NextPageToken = answer.NextPageToken
	return answer, nil
}

func main() {
	baseApi := GenomicsApi{"http://trace.ncbi.nlm.nih.gov/Traces/gg", ""}
	datasetIds := []string{""}
	readsetIds := []string{"SRR960599"}
	sequenceName := "gi|32265499|ref|NC_004917.1|"
	answer, err := baseApi.ReadsSearch(datasetIds, readsetIds,
		sequenceName, 888425, 888470)
	if err != nil {
		fmt.Errorf("Error: %v", err)
	}
	fmt.Printf("Found %v reads.\n", len(answer.Reads))
	for readIndex := range answer.Reads {
		read := answer.Reads[readIndex]
		fmt.Printf("\t%v\n", read.Id)
	}
}
