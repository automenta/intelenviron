/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package intelenviron;

import freeling.*;

public class FreelingAnalyzer {

    static final String FREELINGDIR = "/usr/local";     /// Modify this line to be your FreeLing installation directory
    static final String DATA = FREELINGDIR + "/share/freeling/";
    static final String LANG = "en";
    static final String libraryPath = "/usr/local/lib/libfreeling_javaAPI.so";
    private final tokenizer tk;
    private final splitter sp;
    private final maco mf;
    private final hmm_tagger tg;
    private final chart_parser parser;
    private final dep_txala dep;
    private final ukb_wrap dis;
    private final nec neclass;

    public FreelingAnalyzer() throws Exception {
        System.load(libraryPath);

        util.init_locale("default");

        // create options set for maco analyzer. Default values are Ok, except for data files.
        maco_options op = new maco_options(LANG);

        op.set_active_modules(false, true, true, true, true, true, true, true, true, true, false);
        op.set_data_files("", DATA + LANG + "/locucions.dat", DATA + LANG + "/quantities.dat",
                DATA + LANG + "/afixos.dat", DATA + LANG + "/probabilitats.dat",
                DATA + LANG + "/dicc.src", DATA + LANG + "/np.dat",
                DATA + "common/punct.dat", DATA + LANG + "/corrector/corrector.dat");

        // create analyzers
        tk = new tokenizer(DATA + LANG + "/tokenizer.dat");
        sp = new splitter(DATA + LANG + "/splitter.dat");
        mf = new maco(op);

        tg = new hmm_tagger(LANG, DATA + LANG + "/tagger.dat", true, 2);
        parser = new chart_parser(DATA + LANG + "/chunker/grammar-dep.dat");
        dep = new dep_txala(DATA + LANG + "/dep/dependences.dat", parser.get_start_symbol());

        neclass = new nec(DATA + LANG + "/nec/nec-ab.dat");

        dis = new ukb_wrap(DATA + LANG + "/ukb.dat");
        // Instead of a "disambiguator", you can use a "senses" object, that simply
        // gives all possible WN senses, sorted by frequency.
        // senses dis = new senses(DATA+LANG+"/senses.dat");


    }

    public void printAnalysis(String line) {

        ListWord l = tk.tokenize(line);      // tokenize
        ListSentence ls = sp.split(l, false);  // split sentences
        mf.analyze(ls);                       // morphological analysis
        tg.analyze(ls);                       // PoS tagging

        // NE classifier
        neclass.analyze(ls);

        // sen.analyze(ls);
        dis.analyze(ls);
        printResults(ls, "tagged");

        // Chunk parser
        parser.analyze(ls);
        printResults(ls, "parsed");

        // Dependency parser
        dep.analyze(ls);
        printResults(ls, "dep");


    }

    static void print_senses(word w) {
        // The senses for a FreeLing word are a list of
        // pair<string,double> (sense and page rank). From java, we
        // have to get them as a string with format
        // sense:rank/sense:rank/sense:rank 
        // which will have to be splitted to obtain the info.
        // Here, we just output it: 
        String s = w.get_senses_string();
        if (s.length() > 0)
            System.out.print("\n  senses: " + s);
    }

    static void printResults(ListSentence ls, String format) {

        if (format == "parsed") {
            System.out.println("-------- PARSE TREE results -----------");
            for (int i = 0; i < ls.size(); i++) {
                TreeNode tree = ls.get(i).get_parse_tree();
                printParseTree(0, tree);
            }
        } else if (format == "dep") {
            System.out.println("-------- DEPENDENCY PARSER results -----------");
            for (int i = 0; i < ls.size(); i++) {
                TreeDepnode deptree = ls.get(i).get_dep_tree();
                printDepTree(0, deptree);
            }
        } else {
            System.out.println("-------- TAGGER results -----------");
            // get the analyzed words out of ls.  
            for (int i = 0; i < ls.size(); i++) {
                sentence s = ls.get(i);
                for (int j = 0; j < s.size(); j++) {
                    word w = s.get(j);
                    System.out.print(w.get_form() + " " + w.get_lemma() + " " + w.get_tag());
                    print_senses(w);
                    System.out.println();
                }
                System.out.println();
            }
        }

    } // printResults

    static void printParseTree(int depth, TreeNode tr) {
        word w;
        TreeNode child;
        node nd;
        long nch;

        // indent
        for (int i = 0; i < depth; i++) {
            System.out.print("  ");
        }

        nch = tr.num_children();
        if (nch == 0) {
            // it's a leaf
            if (tr.get_info().is_head()) {
                System.out.print("+");
            }
            w = tr.get_info().get_word();
            System.out.print("(" + w.get_form() + " " + w.get_lemma() + " " + w.get_tag());
            print_senses(w);
            System.out.println(")");
        } else {
            // not a leaf
            if (tr.get_info().is_head()) {
                System.out.print("+");
            }
            System.out.println(tr.get_info().get_label() + "_[");

            for (int i = 0; i < nch; i++) {
                child = tr.nth_child_ref(i);
                if (child != null) {
                    printParseTree(depth + 1, child);
                } else {
                    System.err.println("ERROR: Unexpected NULL child.");
                }
            }
            for (int i = 0; i < depth; i++) {
                System.out.print("  ");
            }
            System.out.println("]");
        }
    } // printParseTree

    static void printDepTree(int depth, TreeDepnode tr) {
        TreeDepnode child = null;
        TreeDepnode fchild = null;
        depnode childnode;
        depnode nd;
        long nch;
        int last, min;
        Boolean trob;

        for (int i = 0; i < depth; i++) {
            System.out.print("  ");
        }

        System.out.print(tr.get_info().get_link_ref().get_info().get_label() + "/" + tr.get_info().get_label() + "/");
        word w = tr.get_info().get_word();
        System.out.print("(" + w.get_form() + " " + w.get_lemma() + " " + w.get_tag());
        print_senses(w);
        System.out.print(")");

        nch = tr.num_children();
        if (nch > 0) {
            System.out.println(" [");

            for (int i = 0; i < nch; i++) {
                child = tr.nth_child_ref(i);
                if (child != null) {
                    if (!child.get_info().is_chunk()) {
                        printDepTree(depth + 1, child);
                    }
                } else {
                    System.err.println("ERROR: Unexpected NULL child.");
                }
            }
            // print CHUNKS (in order)
            last = 0;
            trob = true;
            //while an unprinted chunk is found look, for the one with lower chunk_ord value
            while (trob) {
                trob = false;
                min = 9999;
                for (int i = 0; i < nch; i++) {
                    child = tr.nth_child_ref(i);
                    childnode = child.get_info();
                    if (childnode.is_chunk()) {
                        if ((childnode.get_chunk_ord() > last) && (childnode.get_chunk_ord() < min)) {
                            min = childnode.get_chunk_ord();
                            fchild = child;
                            trob = true;
                        }
                    }
                }
                if (trob && (child != null)) {
                    printDepTree(depth + 1, fchild);
                }
                last = min;
            }

            for (int i = 0; i < depth; i++) {
                System.out.print("  ");
            }
            System.out.print("]");
        }
        System.out.println("");

    } // printDepTree    
    
    public static void main(String[] args) throws Exception {
        new FreelingAnalyzer().printAnalysis("We are very happy now.  John and Jack are humans.  John is not Jack.");
    }
}

