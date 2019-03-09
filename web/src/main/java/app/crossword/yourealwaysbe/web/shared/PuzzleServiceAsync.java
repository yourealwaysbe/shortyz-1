/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package app.crossword.yourealwaysbe.web.shared;

import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.user.client.rpc.AsyncCallback;
import app.crossword.yourealwaysbe.puz.Puzzle;

/**
 *
 * @author kebernet
 */
public interface PuzzleServiceAsync {
    
    public RequestBuilder findPuzzle(Long puzzleId, AsyncCallback<Puzzle> callback);
    public RequestBuilder listPuzzles(AsyncCallback<PuzzleDescriptor[]> callback);
    public RequestBuilder savePuzzle(Long listingId, Puzzle puzzle, AsyncCallback callback);
}
