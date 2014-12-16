package com.jdom.bodycomposition.domain

import groovy.transform.EqualsAndHashCode

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.OneToOne
import javax.persistence.Table

/**
 * Created by djohnson on 11/15/14.
 */
@Entity
@Table(name = 'security_details')
@EqualsAndHashCode(includes = 'id')
class SecurityDetails implements Serializable {

    @Id
    @OneToOne
    Stock id

    @Column
    String name

    @Column
    String category

    @Column
    String country

    @Column(name = 'category_number')
    int categoryNumber

}
