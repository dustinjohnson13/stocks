package com.jdom.bodycomposition.web

import org.apache.wicket.protocol.http.WebApplication
import org.apache.wicket.spring.injection.annot.SpringComponentInjector
import org.springframework.beans.BeansException
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Component

@Component
class WicketApplication extends WebApplication implements ApplicationContextAware {

    private ApplicationContext applicationContext

    @Override
    protected void init() {
        super.init()

        getComponentInstantiationListeners().add(new SpringComponentInjector(this, applicationContext, true))
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * @see org.apache.wicket.Application#getHomePage()
     */
    public Class<HomePage> getHomePage() {
        return HomePage.class;
    }

}
