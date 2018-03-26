package edu.ualberta.storyteller.core.summarygenerator;

import edu.ualberta.storyteller.core.dataloader.Document;
import edu.ualberta.storyteller.core.eventdetector.Event;
import com.hankcs.hanlp.HanLP;

/**
 * This class contains algorithms to summarize an event.
 * <p>
 * @author Bang Liu <bang3@ualberta.ca>
 * @version 2017.0314
 */
public class EventSummaryGenerator {

    /**
     * Summarize an event by text rank algorithm.
     * <p>
     * Select a title from document titles contained in this event using text rank algorithm.
     * <p>
     * @param e Event to summarize.
     * @return Summarize title.
     */
    public static String getEventSummaryByTextRank(Event e) {
        String result = "";
        if (e.docs.size() == 0) {
            return result;
        }

        String allTitles = "";
        for (Document d: e.docs.values()) {
            allTitles += d.title + "\n";
        }

        result = HanLP.extractSummary(allTitles, 1).get(0);

        for (Document d: e.docs.values()) {
            if (d.title.contains(result)) {
                result = d.title;
                break;
            }
        }

        e.summary = result;

        return result;
    }

}
