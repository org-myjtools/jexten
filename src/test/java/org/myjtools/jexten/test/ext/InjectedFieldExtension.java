package org.myjtools.jexten.test.ext;

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Inject;
import org.myjtools.jexten.PostConstruct;

import java.util.*;

@Extension
public class InjectedFieldExtension implements SimpleExtensionPoint {

    @Inject
    private InjectableExtensionPoint injectedExtension;

    @Inject
    private List<InjectableExtensionPoint> injectedList;

    @Inject
    private Set<InjectableExtensionPoint> injectedSet;

    @Inject
    private Collection<InjectableExtensionPoint> injectedCollection;

    @Inject
    private InjectableExtensionPoint[] injectedArray;

    @Inject
    private ExternalInjection externalInjection;


    private InjectableExtensionPoint postConstructInjectedExtension;


    @PostConstruct
    public void postConstruct() {
        this.postConstructInjectedExtension = this.injectedExtension;
    }

    public InjectableExtensionPoint injectedExtension() {
        return injectedExtension;
    }


    public List<InjectableExtensionPoint> injectedList() {
        return injectedList;
    }

    public Set<InjectableExtensionPoint> injectedSet() {
        return injectedSet;
    }

    public Collection<InjectableExtensionPoint> injectedCollection() {
        return injectedCollection;
    }

    public InjectableExtensionPoint[] injectedArray() {
        return injectedArray;
    }


    public InjectableExtensionPoint postConstructInjectedExtension() {
        return postConstructInjectedExtension;
    }


    public ExternalInjection externalInjection() {
        return externalInjection;
    }


    @Override
    public String provideStuff() {
        return "Stuff from InjectedFieldExtension";
    }

}
