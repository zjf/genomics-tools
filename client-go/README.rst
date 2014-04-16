client-go
=========

The file ``main.go`` demonstrates how easy it is to use the `Google Genomics
API`_ with the `Go programming language`_.

.. _Google Genomics Api: https://developers.google.com/genomics/
.. _Go programming language: http://www.golang.org

Getting started
---------------

* First, you'll need a valid client ID and secret. Follow the `authentication
  instructions <https://developers.google.com/genomics#authenticate>`_ and
  download the JSON file for ``installed application``.

* Install `goauth2 <http://code.google.com/p/goauth2>`_::

  go get code.google.com/p/goauth2/oauth

* Run the program with::

  go run main.go -use_oauth client_secret.json


