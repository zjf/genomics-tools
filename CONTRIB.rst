How to become a contributor and submit your own code
====================================================

Contributor License Agreements
------------------------------

We'd love to accept your sample apps and patches! Before we can take them, we
have to jump a couple of legal hurdles.

Please fill out either the individual or corporate Contributor License Agreement
(CLA).

* If you are an individual writing original source code and you're sure you
  own the intellectual property, then you'll need to sign an `individual CLA
  <https://developers.google.com/open-source/cla/individual>`_.
* If you work for a company that wants to allow you to contribute your work,
  then you'll need to sign a `corporate CLA
  <https://developers.google.com/open-source/cla/corporate>`_.

Follow either of the two links above to access the appropriate CLA and
instructions for how to sign and return it. Once we receive it, we'll be able to
accept your pull requests.

Contributing A Patch
--------------------

#. Submit an issue describing your proposed change to the repo in question.
#. The repo owner will respond to your issue promptly.
#. If your proposed change is accepted, and you haven't already done so, sign a
   Contributor License Agreement (see details above).
#. Fork the desired repo, develop and test your code changes.
#. Ensure that your code adheres to the existing style in the sample to which
   you are contributing. Refer to the `Google Cloud Platform Samples Style
   Guide`_ for the recommended coding standards for this organization.
#. Ensure that your code has an appropriate set of unit tests which all pass.
#. Submit a pull request.

Contributing A New Sample App
-----------------------------

#. Submit an issue to the ``GoogleCloudPlatform/Template`` repo describing your
   proposed sample app.
#. The ``Template`` repo owner will respond to your enhancement issue promptly.
   Instructional value is the top priority when evaluating new app proposals for
   this collection of repos.
#. If your proposal is accepted, and you haven't already done so, sign a
   Contributor License Agreement (see details above).
#. Create your own repo for your app following this naming convention::

     {product}-{app-name}-{language}

   :product: ``appengine``, ``compute``, ``storage``, ``bigquery``,
             ``prediction``, ``cloudsql``

   For example: ``appengine-guestbook-python``

   For multi-product apps, concatenate the primary products, like this
   ``compute-appengine-demo-suite-python``.

   For multi-language apps, concatenate the primary languages like this
   ``appengine-sockets-python-java-go``.

#. Clone the ``README.md``, ``CONTRIB.md`` and ``LICENSE`` files from the
   `GoogleCloudPlatform/Template` repo.
#. Ensure that your code adheres to the existing style in the sample to which
   you are contributing. Refer to the `Google Cloud Platform Samples Style
   Guide`_ for the recommended coding standards for this organization.
#. Ensure that your code has an appropriate set of unit tests which all pass.
#. Submit a request to fork your repo in GoogleCloudPlatform organizationt via
   your proposal issue.

.. _Google Cloud Platform Samples Style Guide: https://github.com/GoogleCloudPlatform/Template/wiki/style.html
