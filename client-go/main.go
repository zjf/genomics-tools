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
	"bufio"
	"bytes"
	"encoding/json"
	"flag"
	"fmt"
	"io/ioutil"
	"log"
	"net/http"
	"net/url"
	"os"
	"strings"

	"code.google.com/p/goauth2/oauth"
)

type GenomicsApi struct {
	BaseUrl       string
	Client        *http.Client // OAuth2-produced Client
	NextPageToken string
}

func NewGenomicsApi(baseUrl string, client *http.Client) *GenomicsApi {
	return &GenomicsApi{baseUrl, client, ""}
}

type GenomicsApiReadResource struct {
	Id                        string
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
	Tags                      map[string][]string
}

type GenomicsApiReadsSearchResponse struct {
	Reads         []GenomicsApiReadResource
	NextPageToken string
}

type GenomicsApiReadsetResource struct {
	Id        string
	Name      string
	DatasetId string
	Created   int64
	ReadCount uint64
	FileData  []struct {
		FileUri string
		Headers []struct {
			Version      string
			SortingOrder string
		}
		RefSequences []struct {
			Name        string
			Length      int
			AssemblyId  string
			Md5Checksum string
			Species     string
			Uri         string
		}
		ReadGroups []struct {
			Id                   string
			SequencingCenterName string
			Description          string
			Date                 string
			FlowOrder            string
			KeySequence          string
			Library              string
			ProcessingProgram    string
			PredictedInsertSize  int
			SequencingTechnology string
			PlatformUnit         string
			Sample               string
		}
		Programs []struct {
			Id            string
			Name          string
			CommandLine   string
			PrevProgramId string
			Version       string
		}
		Comments []string
	}
}

type GenomicsApiReadsetsSearchResponse struct {
	Readsets      []GenomicsApiReadsetResource
	NextPageToken string
}

func (api *GenomicsApi) Invoke(
	method string,
	endpoint string,
	body []byte,
	returnValue interface{}) error {

	requestUrl := api.BaseUrl + endpoint
	request, err := http.NewRequest(method, requestUrl, bytes.NewBuffer(body))
	if err != nil {
		return err
	}
	if len(body) > 0 {
		request.Header.Set("Content-type", "application/json")
	}

	response, err := api.Client.Do(request)
	if err != nil {
		return err
	}

	answerBytes, err := ioutil.ReadAll(response.Body)
	response.Body.Close()

	if response.StatusCode != 200 {
		err = fmt.Errorf("%q%q", response.Status, string(answerBytes))
	}
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
	sequenceStart uint64,
	sequenceEnd uint64) (*GenomicsApiReadsSearchResponse, error) {

	data := make(map[string]interface{})
	if len(datasetIds) > 0 {
		data["datasetIds"] = datasetIds
	}
	if len(readsetIds) > 0 {
		data["readsetIds"] = readsetIds
	}
	data["sequenceName"] = sequenceName
	data["sequenceStart"] = sequenceStart
	data["sequenceEnd"] = sequenceEnd
	if api.NextPageToken != "" {
		data["pageToken"] = api.NextPageToken
	}
	body, _ := json.Marshal(data)

	answer := new(GenomicsApiReadsSearchResponse)
	err := api.Invoke("POST", "/reads/search", body, answer)
	if err != nil {
		return nil, err
	}

	api.NextPageToken = answer.NextPageToken
	return answer, nil
}

func (api *GenomicsApi) ReadsetsGet(
	readsetId string) (*GenomicsApiReadsetResource, error) {

	var readset GenomicsApiReadsetResource
	err := api.Invoke("GET", "/readsets/"+url.QueryEscape(readsetId),
		[]byte{}, &readset)
	if err != nil {
		return nil, err
	}
	return &readset, nil
}

func (api *GenomicsApi) ReadsetsSearch(
	datasetIds []string,
	name string) (*GenomicsApiReadsetsSearchResponse, error) {

	data := make(map[string]interface{})
	data["datasetIds"] = datasetIds
	data["name"] = name
	if api.NextPageToken != "" {
		data["pageToken"] = api.NextPageToken
	}
	body, _ := json.Marshal(data)

	answer := new(GenomicsApiReadsetsSearchResponse)
	err := api.Invoke("POST", "/readsets/search", body, answer)
	if err != nil {
		return nil, err
	}

	api.NextPageToken = answer.NextPageToken
	return answer, nil
}

var (
	oauthJsonFile = flag.String("use_oauth", "",
		"Path to client_secrets.json")
)

func obtainOauthCode(url string) string {
	fmt.Println("Please visit the below URL to obtain OAuth2 code.")
	fmt.Println()
	fmt.Println(url)
	fmt.Println()
	fmt.Println("Please enter the code here:")

	line, _, _ := bufio.NewReader(os.Stdin).ReadLine()

	return string(line)
}

func prepareClient() (*http.Client, error) {
	if *oauthJsonFile == "" {
		return &http.Client{}, nil
	}

	jsonData, err := ioutil.ReadFile(*oauthJsonFile)
	if err != nil {
		return nil, err
	}

	var data struct {
		Installed struct {
			Client_Id     string
			Client_Secret string
			Redirect_Uris []string
			Auth_Uri      string
			Token_Uri     string
		}
	}
	err = json.Unmarshal(jsonData, &data)
	if err != nil {
		return nil, err
	}

	config := &oauth.Config{
		ClientId:     data.Installed.Client_Id,
		ClientSecret: data.Installed.Client_Secret,
		RedirectURL:  data.Installed.Redirect_Uris[0],
		Scope: strings.Join([]string{
			"https://www.googleapis.com/auth/genomics",
			"https://www.googleapis.com/auth/devstorage.read_write",
		}, " "),
		AuthURL:    data.Installed.Auth_Uri,
		TokenURL:   data.Installed.Token_Uri,
		TokenCache: oauth.CacheFile(".oauth2_cache.json"),
	}

	transport := &oauth.Transport{Config: config}
	token, err := config.TokenCache.Token()
	if err != nil {
		url := config.AuthCodeURL("")
		code := obtainOauthCode(url)
		token, err = transport.Exchange(code)
		if err != nil {
			return nil, err
		}
	}

	transport.Token = token
	client := transport.Client()

	return client, nil
}

func main() {
	flag.Parse()
	client, err := prepareClient()
	if err != nil {
		log.Fatal(err)
	}
	baseApi := NewGenomicsApi("https://www.googleapis.com/genomics/v1beta",
		client)

	fmt.Println("Searching for readsets in 1000 Genomes project...")
	readsets, err := baseApi.ReadsetsSearch([]string{"376902546192"}, "")
	if err != nil {
		log.Fatal(err)
	}
	readset := readsets.Readsets[0]
	readsetId := readset.Id
	fmt.Printf("The first readset ID is %q.\n", readsetId)

	_, err = baseApi.ReadsetsGet(readsetId)

	fmt.Println("Searching for reads in the first sequence...")
	readsetIds := []string{readsetId}
	sequenceName := readset.FileData[0].RefSequences[0].Name
	baseApi.NextPageToken = ""
	for i := 1; i <= 2; i++ {
		reads, err := baseApi.ReadsSearch([]string{}, readsetIds,
			sequenceName, 1, ^uint64(0));
		if err != nil {
			log.Fatal(err)
		}
		fmt.Printf("Found %v reads in page %v:\n", len(reads.Reads), i)
		for readIndex := range reads.Reads {
			read := reads.Reads[readIndex]
			fmt.Printf("\tId: %v\tName: %v\n", read.Id, read.Name)
		}
		fmt.Printf("Next page token is %q.\n", baseApi.NextPageToken)
	}
}
