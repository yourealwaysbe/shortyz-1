package app.crossword.yourealwaysbe.net;

public class CruScraper extends AbstractPageScraper {
    public CruScraper() {
        super(
            // certificate doesn't seem to work for me
            // "https://theworld.com/~wij/puzzles/cru/index.html",
            "https://archive.nytimes.com/www.nytimes.com/premium/xword/cryptic-archive.html",
            "Cryptic Cru Workshop Archive",
            "https://archive.nytimes.com/www.nytimes.com/premium/xword/cryptic-archive.html"
        );
    }
}
