import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class predict {
    private static Logger logger = Logger.getLogger(predict.class.getName());

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("number of arguments are not valid");
            System.exit(1);
        }
        condNode root = null;
        try {
            root = condNodeReaderWriter.readCondNodeFromFile(args[0]);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

        if (root == null) {
            System.err.println("node from input file is not valid");
            System.exit(1);
        }
        List<Integer[]> csvData = learntree.readCsv(args[1]);
        if (csvData == null) {
            System.err.println("Could not load file \"" + args[1] + "\".");
            System.exit(1);
        }

        for(Integer[] testline: csvData) {
           int num = predictLabel(testline, root);
           System.out.println(num);
        }
    }


    private static int predictLabel(Integer[] testline,condNode node) {
        while (!node.isLeaf()){
            int [] test = new int[testline.length];
            for(int i=0;i<testline.length;i++){
                test[i]=testline[i];
            }
            boolean condResult=node.getCondition().checkCond(test);
            if(condResult) {
                node = node.getRight();
            }
            else {
                node = node.getLeft();
            }
        }
        return node.getLabel();
    }
}
