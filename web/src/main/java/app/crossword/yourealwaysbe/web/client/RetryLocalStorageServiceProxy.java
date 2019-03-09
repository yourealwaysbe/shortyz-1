/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package app.crossword.yourealwaysbe.web.client;

import com.google.gwt.http.client.Request;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.InvocationException;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.web.client.LocalStorageServiceProxy;
import app.crossword.yourealwaysbe.web.shared.PuzzleDescriptor;
import app.crossword.yourealwaysbe.web.shared.PuzzleServiceAsync;

/**
 *
 * @author kebernet
 */
public class RetryLocalStorageServiceProxy extends LocalStorageServiceProxy {

    public RetryLocalStorageServiceProxy(PuzzleServiceAsync service, CallStrategy strat){
        super(service, strat);
    }

    @Override
    public Request listPuzzles(final AsyncCallback<PuzzleDescriptor[]> callback) {
        return super.listPuzzles( new AsyncCallback<PuzzleDescriptor[]>(){

            @Override
            public void onFailure(Throwable caught) {
                if(caught instanceof InvocationException){
                    RetryLocalStorageServiceProxy.super.listPuzzles(callback);
                } else {
                    callback.onFailure(caught);
                }
            }

            @Override
            public void onSuccess(PuzzleDescriptor[] result) {
                callback.onSuccess(result);
            }

        });
    }

    @Override
    public Request findPuzzle(final Long puzzleId, final AsyncCallback<Puzzle> callback) {
        return super.findPuzzle(puzzleId, new AsyncCallback<Puzzle>(){

            @Override
            public void onFailure(Throwable caught) {
                if(caught instanceof InvocationException ){
                    RetryLocalStorageServiceProxy.super.findPuzzle(puzzleId, callback);
                } else {
                    callback.onFailure(caught);
                }
            }

            @Override
            public void onSuccess(Puzzle result) {
                callback.onSuccess(result);
            }

        });
    }





}
