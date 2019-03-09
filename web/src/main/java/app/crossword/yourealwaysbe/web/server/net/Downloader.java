/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package app.crossword.yourealwaysbe.web.server.net;

import app.crossword.yourealwaysbe.puz.Puzzle;
import java.util.Date;

/**
 *
 * @author kebernet
 */
public interface Downloader {


    Puzzle download(Date date);
    String getName();

}
