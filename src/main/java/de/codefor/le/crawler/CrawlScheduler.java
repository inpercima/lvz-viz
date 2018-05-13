package de.codefor.le.crawler;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.commons.lang.math.NumberUtils;
import org.elasticsearch.common.geo.GeoPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import de.codefor.le.crawler.model.Nominatim;
import de.codefor.le.model.PoliceTicker;
import de.codefor.le.ner.NER;
import de.codefor.le.repositories.PoliceTickerRepository;
import lombok.RequiredArgsConstructor;

@Component
@Profile("crawl")
@RequiredArgsConstructor
public class CrawlScheduler {

    private static final Logger logger = LoggerFactory.getLogger(CrawlScheduler.class);

    private final PoliceTickerRepository policeTickerRepository;

    private final LvzPoliceTickerCrawler crawler;

    private final LvzPoliceTickerDetailViewCrawler detailCrawler;

    private final NER ner;

    private final NominatimAsker nominatimAsker;

    // 1_800_000ms = 30min
    @Scheduled(fixedDelay = 1_800_000, initialDelay = 1000)
    public void crawlSchedule() throws ExecutionException, InterruptedException {
        logger.info("Start crawling police ticker.");
        int page = 1;
        while (crawler.isMoreToCrawl()) {
            final Iterable<String> detailPageUrls = crawlMainPage(page++);
            if (detailPageUrls.iterator().hasNext()) {
                final Iterable<PoliceTicker> details = crawlDetailPages(detailPageUrls);
                if (ner != null) {
                    addCoordsToPoliceTickerInformation(details);
                }
                if (details.iterator().hasNext()) {
                    policeTickerRepository.save(details);
                }
            }
        }
        logger.info("Finished crawling police ticker.");
        // else the crawler will not start again after the delay
        crawler.resetCrawler();
    }

    private Iterable<String> crawlMainPage(final int page) throws InterruptedException, ExecutionException {
        final Future<Iterable<String>> mainFuture = crawler.execute(page);
        final Iterable<String> result = mainFuture.get();
        return result;
    }

    private Iterable<PoliceTicker> crawlDetailPages(final Iterable<String> detailPageUrls) throws InterruptedException, ExecutionException {
        final Future<Iterable<PoliceTicker>> detailFuture = detailCrawler.execute(detailPageUrls);
        final Iterable<PoliceTicker> details = detailFuture.get();
        return details;
    }

    void addCoordsToPoliceTickerInformation(final Iterable<PoliceTicker> articles) throws InterruptedException, ExecutionException {
        logger.debug("addCoordsToPoliceTickerInformation for various articles");
        for (final PoliceTicker policeTicker : articles) {
            logger.debug("process article {}", policeTicker.getUrl());
            boolean coordsFound = false;
            // TODO - replace to bulk threading (not every page in one thread)
            for (final String location : ner.getLocations(policeTicker.getArticle(), true)) {
                logger.debug("search {} in nominatim", location);
                final Future<List<Nominatim>> nomFutures = nominatimAsker.execute(NominatimAsker.NOMINATIM_SEARCH_CITY_PREFIX + location);
                final List<Nominatim> nominatim = nomFutures.get();
                logger.debug("{} coords: {}", policeTicker.getUrl(), nominatim);
                if (!nominatim.isEmpty()) {
                    for (final Nominatim n : nominatim) {
                        if (setCoordsIfValid(policeTicker, n)) {
                            coordsFound = true;
                            break;
                        }
                    }
                }
                // TODO better looping without two breaks
                if (coordsFound) {
                    break;
                }
            }
        }
    }

    /**
     * Set coordinates from nominatim to policeTicker if valid
     *
     * @param policeTicker PoliceTicker
     * @param nominatim Nominatim
     * @return true if nominatim contains valid coordinates
     */
    private static boolean setCoordsIfValid(final PoliceTicker policeTicker, final Nominatim nominatim) {
        final String lat = nominatim.getLat();
        final String lon = nominatim.getLon();
        if (NumberUtils.isNumber(lat) && NumberUtils.isNumber(lon)) {
            final GeoPoint g = new GeoPoint(Double.valueOf(lat), Double.valueOf(lon));
            logger.debug("set geoPoint {} to article", g);
            policeTicker.setCoords(g);
            return true;
        }
        logger.warn("latitude {} and longitude {} must be non-empty numeric", lat, lon);
        return false;
    }
}
