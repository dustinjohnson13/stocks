package com.jdom.bodycomposition.service;

import com.jdom.bodycomposition.domain.BaseTicker;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * Created by djohnson on 11/15/14.
 */
@NoRepositoryBean
public interface BaseTickerDao<T extends BaseTicker> extends PagingAndSortingRepository<T, Long> {

}
