/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package app.crossword.yourealwaysbe.web.facebook;

import com.google.gwt.core.client.GWT;
import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;
import app.crossword.yourealwaysbe.web.client.BoxView;
import app.crossword.yourealwaysbe.web.client.Game;
import app.crossword.yourealwaysbe.web.client.PuzzleDescriptorView;
import app.crossword.yourealwaysbe.web.client.PuzzleListView;
import app.crossword.yourealwaysbe.web.client.Renderer;

import app.crossword.yourealwaysbe.web.client.resources.Resources;
import app.crossword.yourealwaysbe.web.shared.PuzzleServiceAsync;


/**
 *
 * @author kebernet
 */
@GinModules(Module.class)
public interface Injector extends Ginjector {
    public static final Injector INSTANCE = GWT.create(Injector.class);

    BoxView boxView();

    Game game();

    PuzzleDescriptorView puzzleDescriptorView();

    PuzzleListView puzzleListView();

    Renderer renderer();

    Resources resources();

    PuzzleServiceAsync service();
}
