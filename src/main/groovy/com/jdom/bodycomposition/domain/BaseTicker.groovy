package com.jdom.bodycomposition.domain

import groovy.transform.ToString

import javax.persistence.*
/**
 * Created by djohnson on 11/15/14.
 */
@MappedSuperclass
@ToString
class BaseTicker implements Serializable {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    Long id

    @Column
    String ticker

    @Column
    String name

    @Column
    String exchange

    @Column
    String category

    @Column
    String country

    @Column(name = 'category_number')
    int categoryNumber
}
