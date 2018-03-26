package edu.ualberta.storyteller.core.summarygenerator;

import com.hankcs.hanlp.HanLP;
import edu.ualberta.storyteller.core.dataloader.Document;
import edu.ualberta.storyteller.core.storymaker.StoryNode;
import edu.ualberta.storyteller.core.storymaker.StoryTree;
import edu.ualberta.storyteller.core.storymaker.TreeTraversalOrderEnum;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * This class contains algorithms to summarize a story.
 * <p>
 * @author Bang Liu <bang3@ualberta.ca>
 * @version 2017.0314
 */
public class StorySummaryGenerator {

    /**
     * Summarize a story by text rank algorithm.
     * <p>
     * Select a percentage of titles from event titles using text rank algorithm.
     * <p>
     * @param st Story tree to summarize.
     * @return Summarize titles.
     */
    public static String getStorySummaryByTextRank(StoryTree st) {
        String result = "";
        if (st.isEmpty()) {
            return result;
        }

        ArrayList<StoryNode> sns = st.build(TreeTraversalOrderEnum.PRE_ORDER);

        ArrayList<Document> ds = new ArrayList<>();
        for (StoryNode sn: sns) {
            if (sn.isRoot() || sn.e.docs.size() == 0) {
                continue;
            }
            String selectedDocTitle = EventSummaryGenerator.getEventSummaryByTextRank(sn.e);

            for (Document d: sn.e.docs.values()) {
                if (d.title.contains(selectedDocTitle)) {
                    ds.add(d);
                    break;
                }
            }
        }

        if (ds.size() == 0) {
            return result;
        }

        // Sorting documents. Document with small publishTime value will put first.
        Collections.sort(ds, new Comparator<Document>() {
            @Override
            public int compare(Document d2, Document d1)
            {
                Long publishDate1 = new Long(-d1.publishTime.getTime());
                Long publishDate2 = new Long(-d2.publishTime.getTime());
                return  publishDate1.compareTo(publishDate2);
            }
        });

        String allTitles = "";
        for (Document d: ds) {
            allTitles += d.title + "\n";
        }

        List<String> titles = HanLP.extractSummary(allTitles, (int) Math.max(1, ds.size() * 0.3));
        for (String t: titles) {
            result += t + "\n";
        }

        st.summary = result;

        return result;
    }
}
