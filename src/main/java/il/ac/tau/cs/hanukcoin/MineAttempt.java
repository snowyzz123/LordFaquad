package il.ac.tau.cs.hanukcoin;
import java.util.ArrayList;
public class MineAttempt {
    public static void main(String[] args) {
        Block genesis = HanukCoinUtils.createBlock0forTestStage();
        ArrayList<Block> blockList = new ArrayList<Block>();
        Block prevBlock = genesis;
        for (int i=1; i<10; i++){
            prevBlock = HanukCoinUtils.mineCoinAtteempt(i, prevBlock, 100000000);
            blockList.add(prevBlock);
            System.out.println("block " + i + " mined");
        }

    }
}
