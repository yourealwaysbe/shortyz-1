/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package app.crossword.yourealwaysbe.web.client;

import com.google.inject.Inject;
import com.google.inject.Provider;
import app.crossword.yourealwaysbe.web.client.resources.Resources;
import app.crossword.yourealwaysbe.web.shared.PuzzleDescriptor;
import com.totsp.gwittir.client.ui.BoundVerticalPanel;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;
import com.totsp.gwittir.client.ui.util.BoundWidgetTypeFactory;

/**
 *
 * @author kebernet
 */
public class PuzzleListView extends BoundVerticalPanel<PuzzleDescriptor>{

    private static BoundWidgetTypeFactory FACTORY = new BoundWidgetTypeFactory(false);
    


    @Inject
    public PuzzleListView(Resources resources, final Provider<PuzzleDescriptorView> provider){
        super(FACTORY, null);
        if(FACTORY.getWidgetProvider(PuzzleDescriptor.class) == null){
            FACTORY.add(PuzzleDescriptor.class, new BoundWidgetProvider<PuzzleDescriptorView>(){

                @Override
                public PuzzleDescriptorView get() {
                    return provider.get();
                }

            });
        }
        this.setStyleName(resources.css().puzzleListView());
    }


}
