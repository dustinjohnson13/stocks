package com.jdom.bodycomposition.service;

import com.jdom.bodycomposition.domain.BaseSecurity;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * Created by djohnson on 11/15/14.
 */
@NoRepositoryBean
public interface BaseSecurityDao<T extends BaseSecurity> extends PagingAndSortingRepository<T, Long> {
   T findBySymbol(final String symbol);
}
