package bookshelf

import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

class BookSpec extends Specification implements DomainUnitTest<Book> {

    def setup() {
    }

    def cleanup() {
    }

    void "test isbn validation"() {
        expect:
            new Book(isbn: bookIsbn).validate(['isbn']) == shouldBeValid

        where:
            bookIsbn | shouldBeValid
            null     | false
            ''       | false
            ' '      | false
            'a' * 35 | false
            'a' * 36 | true
            'a' * 37 | false
    }

    void "test isbn unique constraint"() {
        given: 'a book with the isbn is created'
            String isbn = 'b' * 36

            mockDomain Book
            new Book(isbn: isbn, name: 'on the road', price: 12.23, description: "book description").save(flush: true)

        expect: 'the book with the same isbn is not valid'
            new Book(isbn: isbn).validate(['isbn']) == false
    }

    void "test name validation"() {
        expect:
            new Book(name: bookName).validate(['name']) == shouldBeValid

        where:
            bookName  | shouldBeValid
            null      | false
            ""        | false
            " "       | false
            'a' * 255 | true
            'a' * 256 | false
    }

    void "test price validation"() {
        expect:
            new Book(price: bookPrice).validate(['price']) == shouldBeValid

        where:
            bookPrice | shouldBeValid
            null      | false
            -1        | false
            123.321   | true
    }

    void "test description validation"() {
        expect:
            new Book(description: bookDescription).validate(['description']) == shouldBeValid

        where:
            bookDescription | shouldBeValid
            null            | true
            ""              | true
            " "             | true
            'a' * 255       | true
            'a' * 256       | false
    }
}
