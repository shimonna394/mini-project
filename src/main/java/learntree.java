import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class learntree {
    public static boolean[][] condAnswer;
    public static int[] examplesLabels, initExamples, initLabels, validationIndexArr, mergedExamples, mergedExamplesLabels;
    public static HashMap<String, Integer> condsCounter;
    public static void main(String[] args) throws IOException {
        condsCounter = new HashMap<>();
        Logger logger = Logger.getLogger(learntree.class.getName());
        FileHandler fileHandler = new FileHandler(logger.getName() + ".txt");
        fileHandler.setFormatter(new SimpleFormatter());
        logger.addHandler(fileHandler);
        logger.setLevel(Level.ALL);
        logger.info("start time");
        if (args.length != 5) {
            System.err.println("number of arguments are not valid");
            System.exit(1);
        }
        List<Integer[]> examples = readCsv(args[3]);

//        check.statistics(examples);
///*
        if (examples == null) {
            System.err.println("Could not load file \"" + args[3] + "\".");
            System.exit(1);
        }
//        logger.info("finish loading");
        int opt = Integer.parseInt(args[0]);
        int P = Integer.parseInt(args[1]);
        int L = Integer.parseInt(args[2]);
        int validationNum = (int) (((double) P * (double) examples.size()) / 100.0);
        int[][] examplesData = new int[examples.size() - validationNum][];
        for (int i = 0; i < examplesData.length; i++) {
            Integer[] data = examples.remove((int) (Math.random() * (double) examples.size()));
            examplesData[i] = new int[data.length];
            for (int j = 0; j < examplesData[i].length; j++) {
                examplesData[i][j] = data[j];
            }
        }
        int[][] validationArr = new int[validationNum][];
        for (int i = 0; i < validationNum; i++) {
            Integer[] data = examples.get(i);
            validationArr[i] = new int[data.length];
            for (int j = 0; j < validationArr[i].length; j++) {
                validationArr[i][j] = data[j];
            }
        }

        List<cond> condList = opt == 1 ? generateDefaultConds() : generateImprovedConds();
        init(examplesData, validationArr, condList);
//        logger.info("finish load conds");
        int T = 1;
        int maxT = 1;
        int maxTRate = -1;
//        logger.info("finish first Node");
        condNode root = new condNode(initExamples, initLabels);
        root.calcMaxIG(condList);
//        logger.info("finish first IG " + root.getMaxIG());
        PriorityQueue<condNode> leaves = new PriorityQueue<>((condNode x, condNode y) -> x.getMaxIG() < y.getMaxIG() ? 1 : -1);
        leaves.add(root);
        for (int i = 0; i <= L; i++) {
            for (int j = 1; j <= T; j++) {
                condNode maxLeaf = leaves.poll();
                //substitute max leaf in Node with 2 children
                maxLeaf.addLeafes();
//                logger.info("split leaves " + j + " " + i);
                maxLeaf.getRight().calcMaxIG(condList);
                maxLeaf.getLeft().calcMaxIG(condList);
//                logger.info("calc new leaves " + j + " " + i + " " + maxLeaf.getRight().getMaxIG() + " " + maxLeaf.getLeft().getMaxIG());
                leaves.add(maxLeaf.getRight());
                leaves.add(maxLeaf.getLeft());
//                logger.info("finish iteration " + j + " " + i);
            }

            int currRate = checkValidation(root, validationIndexArr);
            if (currRate > maxTRate) {
                maxTRate = currRate;
                maxT = (int) Math.pow(2, i);
            }
            if (i != 0)
                T = T * 2;
        }

        logger.info("finish check T " + maxT);
        mergeExamples(examplesData.length + validationNum);
        condNode newRoot = new condNode(mergedExamples, mergedExamplesLabels);
        newRoot.calcMaxIG(condList);
        PriorityQueue<condNode> newLeaves = new PriorityQueue<>((condNode x, condNode y) -> x.getMaxIG() < y.getMaxIG() ? 1 : -1);
        newLeaves.add(newRoot);

        for (int i = 1; i <= maxT; i++) {
            condNode maxLeaf = newLeaves.poll();
            //substitute max leaf in Node with 2 children
//            logger.info("split leaves" + i);
            maxLeaf.addLeafes();
            maxLeaf.setExamplesArrivedSoFar(null);
            maxLeaf.setLabels(null);
            maxLeaf.getRight().calcMaxIG(condList);
            maxLeaf.getLeft().calcMaxIG(condList);
//            logger.info("calc new leaves");
            newLeaves.add(maxLeaf.getRight());
            newLeaves.add(maxLeaf.getLeft());
//            logger.info("finish iteration");
        }


        for (condNode l : newLeaves) {
            l.setExamplesArrivedSoFar(null);
            l.setLabels(null);
        }
        try {
            condNodeReaderWriter.writeCondNodeToFile(newRoot, args[4]);
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

      /*  traverse(newRoot);
        condsCounter.entrySet().forEach(entry->{
            System.out.println(entry.getKey() + " " + entry.getValue());
        }); */

        System.out.println("num: " + mergedExamples.length);
        System.out.println("error: " + (int) (100 - ((double) checkValidation(newRoot, mergedExamples) / (double) (mergedExamples.length) * 100)));
        System.out.println("size: " + maxT);
//*/

        logger.info("end time");
    }

    public static void printNode(condNode root) {
        System.out.println("the label is " + root.getLabel());
        System.out.println("the entropy is " + root.getEntropy());
        System.out.println("the maxIG is " + root.getMaxIG());
        if (root.getLeft() != null)
            printNode(root.getLeft());
        if (root.getRight() != null)
            printNode(root.getRight());
    }

    public static void mergeExamples(int size) {
        mergedExamples = new int[size];
        mergedExamplesLabels = new int[10];
        for (int i = 0; i < size; i++) {
            mergedExamples[i] = i;
            mergedExamplesLabels[examplesLabels[mergedExamples[i]]]++;
        }
    }

    public static int checkValidation(condNode root, int[] validationIndexArr) {
        int counter = 0;
        for (int e : validationIndexArr) {
            if (checkExample(root, e))
                counter++;
        }
        return counter;
    }

    public static boolean checkExample(condNode root, int validationIndex) {
        condNode checkRoot = root;
        while (checkRoot.getRight() != null && checkRoot.getLeft() != null) {
            if (checkRoot.getCondition().getCondAns(validationIndex))
                checkRoot = checkRoot.getRight();
            else
                checkRoot = checkRoot.getLeft();
        }
        return checkRoot.getLabel() == examplesLabels[validationIndex];
    }

    public static void init(int[][] examples, int[][] validationArr, List<cond> condList) {
        condAnswer = new boolean[examples.length + validationArr.length][condList.size()];
        examplesLabels = new int[examples.length + validationArr.length];
        initExamples = new int[examples.length];
        initLabels = new int[10];
        validationIndexArr = new int[validationArr.length];
        for (int i = 0; i < examples.length; i++) {
            boolean[] exampleAns = condAnswer[i];
            initExamples[i] = i;
            examplesLabels[i] = examples[i][0];
            initLabels[examplesLabels[i]]++;
            for (int j = 0; j < condList.size(); j++) {
                exampleAns[j] = condList.get(j).checkCond(examples[i]);
            }
        }
        for (int i = 0; i < validationArr.length; i++) {
            boolean[] exampleAns = condAnswer[examples.length + i];
            examplesLabels[examples.length + i] = validationArr[i][0];
            validationIndexArr[i] = examples.length + i;
            for (int j = 0; j < condList.size(); j++) {
                exampleAns[j] = condList.get(j).checkCond(validationArr[i]);
            }
        }
        for(int i=0;i<condList.size();i++){
            condList.get(i).setIndex(i+1);
        }
    }

    public static List<cond> generateDefaultConds() {
        List<cond> ans = new ArrayList<>();
        for (int i = 1; i <= 28 * 28; i++) {
            ans.add(new DefaultCond(i));
        }
        return ans;
    }

    public static List<cond> generateImprovedConds() {
        List<cond> ans = new ArrayList<>();
        int colCounter = 0;
        int rowCounter = 0;

//        for(int j=4;j<5;j++) {
//            for (int i = 1; i < 27; i++) {
//                ans.add(new verticalNode(i, "row", j));
//                ans.add(new verticalNode(i, "col", j));
//            }
//        }

        for (int i = 1; i <= 28 * 28; i++) {
            int row = rowCounter % 28;
            int col = colCounter % 28;

//            System.out.println("row "+row+" col "+col);
            ans.add(new ImprovedCond(i-1,i));

//            for(int j=2;j<3;j++) {
//                if(row<28-j-1 && col<28-j-5)
//                  ans.add(new blockCond(i,"r",j));
//
//                if(col<28-j-1 && row<28-j-5)
//                    ans.add(new blockCond(i,"c",j));
//            }


            colCounter++;
            if (colCounter % 28 == 0)
                rowCounter++;

                if (row >= 0 && row < 28-2 && col >= 0 && col < 28-6)
                    ans.add(new blockCond(i-1,i-1,2,6,3));

        }
        for(int i=1;i<=28;i++){
            for(int k=1;k<=10;k++){
                ans.add(new verticalNode(i-1,i,"row",k));
                ans.add(new verticalNode(i-1,i,"col",k));
            }
        }
        condsCounter.put("cubeCond",0);
        condsCounter.put("ImprovedCond",0);
        condsCounter.put("colCond",0);
        condsCounter.put("rowCond",0);
        condsCounter.put("blockCond",0);
        condsCounter.put("verticalNode",0);

        return ans;
    }

    public static ArrayList<Integer[]> readCsv(String path) {
        ArrayList<Integer[]> ret = new ArrayList<Integer[]>();
        ret.ensureCapacity(1 << 16);

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;

            while ((line = br.readLine()) != null) {
                String[] sVals = line.split(",");
                ret.add(new Integer[sVals.length]);

                for (int j = 0; j < sVals.length; j++)
                    ret.get(ret.size() - 1)[j] = Integer.parseInt(sVals[j]);
            }
        } catch (Exception e) {
            return null;
        }

        return ret;
    }

    public static void traverse(condNode newRoot){
        if(newRoot!=null){


            /* first recur on left child */
            traverse(newRoot.getLeft());

            /* then print the data of node */
            cond cond=newRoot.getCondition();
            if(cond!=null) {
                String chosen=cond.getClass().toString().split(" ")[1];
                Integer count =condsCounter.get(chosen);
                condsCounter.put(chosen,count+1);
            }

            /* now recur on right child */
            traverse(newRoot.getRight());

        }
    }

}



