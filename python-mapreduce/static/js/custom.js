/*
Copyright 2014 Google Inc. All rights reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

/*
 * Updates the form that runs MapReduce jobs once the user selects their input
 * data from the list of input files. Exists because we have two separate forms
 * on our HTML - one that allows users to upload new input files, and one that
 * allows users to run MapReduce jobs given a certain input file. Since the
 * latter form cannot see which input file has been selected (that button is
 * out of this form's scope), we throw some quick JavaScript in to sync the
 * value of the user's choice with a hidden field in the form as well as a
 * visible label displaying the input file's name for the user to see.
 * @param {string} filekey The internal key that the Datastore uses to reference
 *     this input file.
 * @param {string} blobkey The Blobstore key associated with the input file
 *     whose key is filekey.
 * @param {string} filename The name that the user has chosen to give this input
 *     file upon uploading it.
 */
function updateForm(filekey, blobkey, filename) {
  $('#jobName').text(filename);
  $('#filekey').val(filekey);
  $('#blobkey').val(blobkey);

  $('#word_count').removeAttr('disabled');
  $('#index').removeAttr('disabled');
  $('#phrases').removeAttr('disabled');
}

