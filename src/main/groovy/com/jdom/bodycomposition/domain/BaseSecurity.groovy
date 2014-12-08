package com.jdom.bodycomposition.domain

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

import javax.persistence.*
/**
 * Created by djohnson on 11/15/14.
 */
@MappedSuperclass
@ToString(includes = 'symbol')
@EqualsAndHashCode(includes = 'symbol')
class BaseSecurity implements Serializable {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    Long id

    @Column
    String symbol

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
