#!/bin/sh
set -e
set -x

#(cd .. && grails run-app)

# no book
curl --request GET \
  --url http://localhost:8080/books
echo

# create two books
curl --request POST \
  --url http://localhost:8080/books \
  --header 'Content-Type: application/json' \
  --data @book1.json

echo

curl --request POST \
  --url http://localhost:8080/books \
  --header 'Content-Type: application/json' \
  --data @book2.json

echo

# two book
curl --request GET \
  --url http://localhost:8080/books

echo

# book with id=1
curl --request GET \
  --url http://localhost:8080/books/1

echo

# book with id=100
curl --request GET \
  --url http://localhost:8080/books/100

echo

# update book with id=1
curl --request PUT \
  --url http://localhost:8080/books/1 \
  --header 'Content-Type: application/json' \
  --data '{ "price": 100.123 }'

echo

# get book with id=1
curl --request GET \
  --url http://localhost:8080/books/1

echo

# delete book with id=1
curl --request DELETE \
  --url http://localhost:8080/books/1

echo

# get book with id=1
curl --request GET \
  --url http://localhost:8080/books/1

echo

# one book
curl --request GET \
  --url http://localhost:8080/books

echo