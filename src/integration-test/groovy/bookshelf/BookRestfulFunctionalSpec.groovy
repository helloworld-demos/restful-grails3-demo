package bookshelf

import geb.spock.GebSpec
import grails.plugins.rest.client.RestBuilder
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback

import static grails.web.http.HttpHeaders.CONTENT_TYPE
import static org.springframework.http.HttpStatus.*

@Integration
@Rollback
class BookRestfulFunctionalSpec extends GebSpec {

    Book testBook

    def setup() {
        // Below line would persist and not roll back; setup is in another transaction which will be committed in the end of setup()
        testBook = new Book(isbn: '4ba57136-edb8-11e7-8c3f-9a214cf093ae', name: 'on the road', price: 12.23, description: "book description").save()
    }

    def cleanup() {
        Book deletedBook = Book.get(testBook.id)

        if (deletedBook != null) {
            deletedBook.delete()
        }
    }

    List<Book> setupBook(int num) {
        // it could be replaced by using @Transactional(propagation = Propagation.REQUIRES_NEW)
        Book.withNewTransaction {
            (1..num).collect {
                new Book(isbn: UUID.randomUUID(), name: "book-name-${it}", price: 12.23, description: "book-description-${it}").save()
            }
        }
    }

    void deleteBooks(List<Long> ids) {
        Book.withNewTransaction {
            ids.each {
                Book.findById(it).delete()
            }
        }
    }


    void "GET books"() {
        when:
            def resp = restBuilder().get("$baseUrl/books")

        then:
            resp.status == OK.value()
            resp.headers[CONTENT_TYPE] == ['application/json;charset=UTF-8']

            resp.json.size() == 1
    }

    void "GET books with a page size"() {
        given: " a list of books are generated in database"
            List<Book> testBooks = setupBook(50)

            int defaultPageSize = 10
            int aPageSize = 30

        when: "GET books with the default page size"
            def resp1 = restBuilder().get("$baseUrl/books")

        then: "the default page size is 10"
            resp1.status == OK.value()
            resp1.headers[CONTENT_TYPE] == ['application/json;charset=UTF-8']

            resp1.json.size() == defaultPageSize

        when: "GET books with a specified page size"
            def resp2 = restBuilder().("$baseUrl/books?max=${aPageSize}")

        then:
            resp2.status == OK.value()
            resp2.headers[CONTENT_TYPE] == ['application/json;charset=UTF-8']

            resp2.json.size() == aPageSize

        then: "delete generated data"
            deleteBooks(testBooks.collect { it.id })
    }

    void "GET books with pagination"() {
        given: " a list of books are generated in database"
            int numOfBooks = 50
            List<Book> testBooks = setupBook(numOfBooks)

            int defaultPageSize = 10

        when: "first page is retrieved"
            def resp1 = restBuilder().get("$baseUrl/books")

        then:
            resp1.status == OK.value()
            resp1.headers[CONTENT_TYPE] == ['application/json;charset=UTF-8']

            resp1.json.size() == defaultPageSize

            int firstBookId1 = resp1.json[0].id
            int lastBookId1 = resp1.json[-1].id

        when: "second page is retrieved"
            def resp2 = restBuilder().get("$baseUrl/books?offset=${defaultPageSize}")

        then:
            resp2.status == OK.value()
            resp2.headers[CONTENT_TYPE] == ['application/json;charset=UTF-8']

            resp2.json.size() == defaultPageSize

            int firstBookId2 = resp2.json[0].id

            firstBookId1 != firstBookId2
            firstBookId2 == lastBookId1 + 1

        when: "another page for non-existing data"
            def resp3 = restBuilder().get("$baseUrl/books?offset=${numOfBooks * 2}")

        then: "empty page returns"
            resp3.status == OK.value()
            resp3.headers[CONTENT_TYPE] == ['application/json;charset=UTF-8']

            resp3.json.size() == 0

        then: "delete generated data"
            deleteBooks(testBooks.collect { it.id })
    }

    void "GET a book"() {
        when:
            def resp = restBuilder().get("$baseUrl/books/$testBook.id")

        then:
            resp.status == OK.value()
            resp.headers[CONTENT_TYPE] == ['application/json;charset=UTF-8']

            resp.json.id == testBook.id
            resp.json.isbn == testBook.isbn
            resp.json.name == testBook.name
            resp.json.price == testBook.price
            resp.json.description == testBook.description
    }

    void "GET a non existing book"() {
        given:
            int nonExistingBookId = 100
            !Book.exists(nonExistingBookId)

        when:
            def resp = restBuilder().get("$baseUrl/books/$nonExistingBookId")

        then:
            resp.status == NOT_FOUND.value()
            resp.headers[CONTENT_TYPE] == ['application/json;charset=UTF-8']
    }

    void 'POST a book'() {
        given:
            Map testBookData = [
                    isbn       : '4ba57136-edb8-11e7-8c3f-9a214cf093aa',
                    name       : 'test-book-name',
                    price      : 123.231,
                    description: 'test-book-description'
            ]

        when:
            def resp = restBuilder().post("$baseUrl/books") {
                json {
                    isbn = testBookData.isbn
                    name = testBookData.name
                    price = testBookData.price
                    description = testBookData.description
                }
            }

        then:
            def actualBook = Book.findByIsbn(testBookData.isbn)

            actualBook.isbn == testBookData.isbn
            actualBook.name == testBookData.name
            actualBook.price == testBookData.price
            actualBook.description == testBookData.description

            resp.status == CREATED.value()
            resp.headers[CONTENT_TYPE] == ['application/json;charset=UTF-8']

            resp.json.id == actualBook.id
            resp.json.isbn == testBookData.isbn
            resp.json.name == testBookData.name
            resp.json.price == testBookData.price
            resp.json.description == testBookData.description
    }

    void 'POST an invalid book'() {
        given: "isbn is not unique"
            Map testBookData = [
                    isbn       : testBook.isbn,
                    name       : 'test-book-name',
                    price      : 123.321,
                    description: 'test-book-description'
            ]

        when:
            def resp = restBuilder().post("$baseUrl/books") {
                json {
                    isbn = testBookData.isbn
                    name = testBookData.name
                    price = testBookData.price
                    description = testBookData.description
                }
            }

        then: "the book is not saved in database"
            !Book.exists(testBookData.isbn)

            resp.status == UNPROCESSABLE_ENTITY.value()
            resp.headers[CONTENT_TYPE] == ['application/json;charset=UTF-8']

            resp.json.toString().contains('must be unique')
    }


    void "DELETE a book"() {
        when:
            Book.exists(testBook.id)

            def resp = restBuilder().delete("$baseUrl/books/$testBook.id")

        then:
            resp.status == NO_CONTENT.value()
            resp.body == null

            !Book.exists(testBook.id)
    }

    void "DELETE a non existing book"() {
        given:
            int nonExistingBookId = 100

            !Book.exists(nonExistingBookId)

        when:
            def resp = restBuilder().delete("$baseUrl/books/${nonExistingBookId}")

        then:
            resp.status == NOT_FOUND.value()
    }

    // PUT can actually just update one attribute
    void "PUT a book"() {
        given:
            String updatedBookName = 'updated book name'

        when:
            def resp = restBuilder().put("$baseUrl/books/$testBook.id") {
                json {
                    name = updatedBookName
                }
            }

        then:
            resp.status == OK.value()
            resp.headers[CONTENT_TYPE] == ['application/json;charset=UTF-8']

            resp.json.id == testBook.id
            resp.json.isbn == testBook.isbn
            resp.json.name == updatedBookName
            resp.json.price == testBook.price
            resp.json.description == testBook.description
    }

    void "PUT a book with an invalid attribute"() {
        given:
            String invalidPrice = -1

        when:
            def resp = restBuilder().put("$baseUrl/books/$testBook.id") {
                json {
                    price = invalidPrice
                }
            }

        then:
            resp.status == UNPROCESSABLE_ENTITY.value()
            resp.headers[CONTENT_TYPE] == ['application/json;charset=UTF-8']

            resp.json.toString().contains('is less than minimum value')

            Book.findById(testBook.id).price == testBook.price
    }

    void "PUT an non existing book"() {
        given:
            int nonExistingBookId = 100

            !Book.exists(nonExistingBookId)

        when:
            def resp = restBuilder().put("$baseUrl/books/${nonExistingBookId}")

        then:
            resp.status == NOT_FOUND.value()
    }

    RestBuilder restBuilder() {
        new RestBuilder()
    }
}
