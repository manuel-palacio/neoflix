package net.palacesoft.movies

import org.junit.Test
import groovy.json.JsonSlurper

class TmdbTest {

    def movieKey = System.getProperty("TMDB_KEY")


    @Test
    public void findMovie() {
        def movieResponse = new JsonSlurper()

        def result = movieResponse.parseText(new URL("http://api.themoviedb.org/3/search/movie?api_key=${movieKey}&query=${URLEncoder.encode("Dead Poets Society")}").text)

        def movieUrl = "http://www.themoviedb.org/movie/${result.results.id[0]}"
        def poster = "http://cf2.imgobject.com/t/p/w185${result.results.poster_path[0]}"
        def tagLine = ""
        def rating = result.results.vote_average[0]
        def certification = ""
        def overview = ""

        assert result

    }
}
