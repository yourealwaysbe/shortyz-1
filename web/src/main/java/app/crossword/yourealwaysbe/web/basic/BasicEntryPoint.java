/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package app.crossword.yourealwaysbe.web.basic;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;
import app.crossword.yourealwaysbe.web.client.Game;
import app.crossword.yourealwaysbe.web.client.WASDCodes;




/**
 *
 * @author kebernet
 */
public class BasicEntryPoint implements EntryPoint {
    static final WASDCodes CODES = GWT.create(WASDCodes.class);
    

    @Override
    public void onModuleLoad() {
        Element e = DOM.getElementById("loadingIndicator");

        if (e != null) {
            e.removeFromParent();
        }


        Game g = Injector.INSTANCE.game();
        //g.setSmallView(true);
        g.loadList();
        
    }

    


   
}
