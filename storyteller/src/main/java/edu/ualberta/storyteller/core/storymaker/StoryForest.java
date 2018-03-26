package edu.ualberta.storyteller.core.storymaker;

import edu.ualberta.storyteller.core.dataloader.*;
import edu.ualberta.storyteller.core.eventdetector.*;
import java.io.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class StoryForest implements Serializable {
    //! Story tree list.
    public ArrayList<StoryTree> storyTrees;

    //! Corpus.
    public Corpus corpus = new Corpus();

    //! Cumulative DF. It records all DF from all the documents that generates this story forest.
    public HashMap<String, Double> cumulativeDF = new HashMap<String, Double>();

    //! Cumulative document number. It counts all the document numbers to calculate IDF from cumulative DF.
    public int cumulativeDocAmount;

    /**
     * Default constructor.
     */
    public StoryForest() {
        storyTrees = new ArrayList<StoryTree>();
    }


    public ArrayList<Event> getAllEvents() {
        ArrayList<Event> result = new ArrayList<>();
        for (StoryTree st: storyTrees) {
            ArrayList<StoryNode> storyNodes = st.build(TreeTraversalOrderEnum.PRE_ORDER);
            for (int i = 1; i < storyNodes.size(); i++) {
                result.add(storyNodes.get(i).e);
            }
        }
        return result;
    }


    /**
     * Print story forest.
     * @param out The print stream.
     */
    public void printAll(PrintStream out) {
        for (StoryTree st: storyTrees) {
            st.print(out);
            out.println("\n");
        }
    }

    /**
     * Print story forest trees that contains more than 2 story nodes.
     * @param out The print stream.b
     * @param minStorySize Stories with number of nodes (not include root) bigger than this will be print.
     * @param maxStaleAge Stories with stale age smaller than this will be print.
     * @param format Story format. "tree" or "graph" or "none".
     */
    public void print(PrintStream out, int minStorySize, int maxStaleAge, String format) {
        for (StoryTree st: storyTrees) {
            if (st.getNumberOfNodes() > minStorySize && st.staleAge <= maxStaleAge) {
                switch (format) {
                    case "tree":
                        st.printStoryTree(out); break;
                    case "graph":
                        st.printDAG(out); break;
                    case "none":
                        break;
                    default:
                        out.println("Please input correct print format parameter.");
                }
                out.println("\n");
            }
        }

    }

    /**
     * Filter docs outside the time section.
     * @param t Threshold time.
     */
    public void filterStoryTreesByTime(Timestamp t) {
        for (Iterator<StoryTree> iter = storyTrees.iterator(); iter.hasNext(); ) {
            StoryTree st = iter.next();
            Timestamp storyEndTime = new Timestamp(st.endTimestamp);
            if (storyEndTime.before(t)) {
                iter.remove();
            }
        }
    }


}
