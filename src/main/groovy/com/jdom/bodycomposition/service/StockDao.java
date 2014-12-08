package com.jdom.bodycomposition.service;

import com.jdom.bodycomposition.domain.Stock;
import org.springframework.stereotype.Repository;

/**
 * Created by djohnson on 11/15/14.
 */
@Repository
public interface StockDao extends BaseSecurityDao<Stock> {
}
