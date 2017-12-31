package bookshelf

import grails.rest.Resource

@Resource(uri = '/books')
class Book {
    String isbn
    String name
    double price
    String description

    // by default Grails databinding will convert blank strings to null
    static constraints = {
        isbn unique: true, size: 36..36
        name size: 1..255
        price min: 0d
        description nullable: true, size: 1..255
    }

    //    static mapping = {
    //        version false
    //        table 'orders'
    //        id column: 'id', generator:'native', params:[sequence:'order_seq']
    //    }
}