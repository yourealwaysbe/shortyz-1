/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package app.crossword.yourealwaysbe.web.basic;

import app.crossword.yourealwaysbe.web.client.LocalStorageServiceProxy;
import com.google.gwt.core.client.GWT;
import com.google.gwt.inject.client.AbstractGinModule;
import com.google.gwt.user.client.ui.RootPanel;

import com.google.inject.Inject;
import com.google.inject.Provider;
import app.crossword.yourealwaysbe.web.client.BoxView;
import app.crossword.yourealwaysbe.web.client.Game;
import app.crossword.yourealwaysbe.web.basic.Module.RootPanelProvider;
import app.crossword.yourealwaysbe.web.client.PuzzleDescriptorView;
import app.crossword.yourealwaysbe.web.client.PuzzleListView;
import app.crossword.yourealwaysbe.web.client.PuzzleServiceProxy;
import app.crossword.yourealwaysbe.web.client.Renderer;

import app.crossword.yourealwaysbe.web.client.resources.Resources;
import app.crossword.yourealwaysbe.web.shared.PuzzleService;
import app.crossword.yourealwaysbe.web.shared.PuzzleServiceAsync;


/**
 *
 * @author kebernet
 */
public class Module extends AbstractGinModule {
    @Override
    protected void configure() {
        this.bind(Resources.class).toProvider(ResourcesProvider.class);
        this.bind(Renderer.class);
        this.bind(PuzzleServiceAsync.class)
            .toProvider(PuzzleServiceProvider.class);
        this.bind(BoxView.class);
        this.bind(PuzzleDescriptorView.class);
        this.bind(PuzzleListView.class);
        this.bind(Game.class).asEagerSingleton();
        this.bind(PuzzleServiceProxy.class)
            .toProvider(PuzzleServiceProxyProvider.class).asEagerSingleton();
        this.bind(RootPanel.class).toProvider(RootPanelProvider.class);
    }

    public static class RootPanelProvider implements Provider<RootPanel> {

        @Override
        public RootPanel get() {
            return RootPanel.get();
        }

    }

    public static class PuzzleServiceProvider implements Provider<PuzzleServiceAsync> {
        public static PuzzleServiceAsync INSTANCE = null;

        @Override
        public PuzzleServiceAsync get() {
            return (INSTANCE == null)
            ? (INSTANCE = GWT.create(PuzzleService.class)) : INSTANCE;
        }
    }

    public static class PuzzleServiceProxyProvider implements Provider<PuzzleServiceProxy> {
        PuzzleServiceAsync service;

        @Inject
        PuzzleServiceProxyProvider(PuzzleServiceAsync service) {
            this.service = service;
        }

        @Override
        public PuzzleServiceProxy get() {
            return new LocalStorageServiceProxy(service, null);
        }
    }

    public static class ResourcesProvider implements Provider<Resources> {
        Resources instance = null;

        @Override
        public Resources get() {
            return (instance == null) ? (instance = GWT.create(Resources.class))
                                      : instance;
        }
    }
}
