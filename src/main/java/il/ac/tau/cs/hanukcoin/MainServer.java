package il.ac.tau.cs.hanukcoin;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;


public class MainServer {
	// TODO add timer + hashmap shit

    public static final int BEEF_BEEF = 0xbeefBeef;
    public static final int DEAD_DEAD = 0xdeadDead;
    public static final String BEERI_IP_HOME_PC = "147.235.212.60";
    public static final String BEERI_IP_UNI = "172.30.103.105";
    public static final String OMER_IP_UNI = "172.30.96.77";
    
    public static void log(String fmt, Object... args) {
        println(fmt, args);
    }
    public static void println(String fmt, Object... args) {
        System.out.format(fmt + "\n", args);
    }


    static class NodeInfo {
        // FRANJI: Discussion - public members - pro/cons. What is POJO
        public String name;
        public String host;
        public int port;
        public int lastSeenTS;
        // TODO(students): add more fields you may need such as number of connection attempts failed
        //  last time connection was attempted, if this node is new ot alive etc.

        public static String readLenStr(DataInputStream dis) throws IOException {
            byte strLen = dis.readByte();
            byte[] strBytes = new byte[strLen];
            dis.readFully(strBytes);
            return new String(strBytes, "utf-8");
        }

        public static NodeInfo readFrom(DataInputStream dis) throws IOException {
            NodeInfo n = new NodeInfo();
            n.name = readLenStr(dis);
            n.host = readLenStr(dis);
            n.port = dis.readShort();
            n.lastSeenTS =dis.readInt();
            // TODO(students): update extra fields
            return n;
        }
    }
    
    static class OtherClient{
    	public Socket soc;
    	
        public DataInputStream dataInput;
        public DataOutputStream dataOutput;
        
        public OtherClient(Socket socket) {
        	soc = socket;
        	try {
        		dataInput = new DataInputStream(socket.getInputStream());
        		dataOutput = new DataOutputStream(socket.getOutputStream());
        		
        	} catch (IOException e) {
        		throw new RuntimeException("FATAL = cannot create data streams", e);
        	}
        }
    }


    static class ClientConnection {
    	private ArrayList<NodeInfo> nodes = new ArrayList<NodeInfo>();
    	private ArrayList<Block> blocks = new ArrayList<Block>();
    	private ArrayList<OtherClient> otherClients = new ArrayList<OtherClient>();
    	
    	private static NodeInfo self = new NodeInfo();
    	
        private DataInputStream dataInput;
        private DataOutputStream dataOutput;
        

        
        
        public ClientConnection(Socket connectionSocket) {
            try {
                dataInput = new DataInputStream(connectionSocket.getInputStream());
                dataOutput = new DataOutputStream(connectionSocket.getOutputStream());
                otherClients.add(new OtherClient(connectionSocket));
                this.initClients();

            } catch (IOException e) {
               throw new RuntimeException("FATAL = cannot create data streams", e);
            }
            self.name = "LordFarquad";
            self.host = BEERI_IP_UNI;
            self.port = 8088;
            self.lastSeenTS = (int)(System.currentTimeMillis() / 1000);
            
            nodes.add(self);
        }
        
        public void initClients() throws IOException {
        	for(NodeInfo node : nodes) {
        		if(node != self) {
        			Socket soc = new Socket(node.host, node.port);
        			otherClients.add(new OtherClient(soc));
        		}	
        	}
        }

        public void sendReceive() {
            try {
            	initClients();
                for(OtherClient client : otherClients) {
                    sendRequest(1, client.dataOutput);
                    parseMessage(client.dataInput, client.dataOutput);
                }
                

            } catch (IOException e) {
                throw new RuntimeException("send/recieve error", e);
            }
        }
        
        public static byte[] nodeBuilder(NodeInfo currNode){
        	String myname = currNode.name;
        	String myhost = currNode.host; 
        	int port = currNode.port;
        	int timestamp = currNode.lastSeenTS;
        	
            int n = myname.length();
            int m = myhost.length();
            byte[] node = new byte[8+n+m];
            node[0] = (byte)n;
            for (int i = 0; i<n; i++){
                node[i+1] = (byte)myname.charAt(i);
            }
            node[n+1] = (byte)m;
            for (int i = 0; i<m; i++){
                node[n+i+2] = (byte)myhost.charAt(i);
            }
            node[n + m + 2] = (byte)((port >>> 8) & 0xFF);
            node[n + m + 3] = (byte)((port) & 0xFF);
            HanukCoinUtils.intIntoBytes(node, n+m+4 , timestamp);
            return node;
        }
        

        public void parseMessage(DataInputStream dataInput, DataOutputStream dataOutput) throws IOException  {
            int cmd = dataInput.readInt(); // skip command field
            if(cmd == 1) {
            	this.sendRequest(2, dataOutput);
            	return;
            }
            
            int beefBeef = dataInput.readInt();
            if (beefBeef != BEEF_BEEF) {
                throw new IOException("Bad message no BeefBeef");
            }
            int nodesCount = dataInput.readInt();
            // FRANJI: discussion - create a new list in memory or update global list?
            ArrayList<NodeInfo> receivedNodes =  new ArrayList<>();
            for (int ni = 0; ni < nodesCount; ni++) {
                NodeInfo newInfo = NodeInfo.readFrom(dataInput);
                receivedNodes.add(newInfo);
            }
            int deadDead = dataInput.readInt();
            if (deadDead != DEAD_DEAD) {
                throw new IOException("Bad message no DeadDead");
            }
            int blockCount = dataInput.readInt();
            // FRANJI: discussion - create a new list in memory or update global list?
            ArrayList<Block> receivedBlocks =  new ArrayList<>();
            for (int bi = 0; bi < blockCount; bi++) {
                Block newBlock = Block.readFrom(dataInput);
                receivedBlocks.add(newBlock);
            }
            if(receivedBlocks.size() >= blocks.size()) {
            	nodes = receivedNodes;
            	blocks = receivedBlocks;
            }
            
            printMessage(receivedNodes, receivedBlocks);
            
        }

        private void printMessage(List<NodeInfo> receivedNodes, List<Block> receivedBlocks) {
            println("==== Nodes ====");
            for (NodeInfo ni : receivedNodes) {
                println("%20s\t%s:%s\t%d",ni.name,  ni.host, ni.port, ni.lastSeenTS);
            }
            println("==== Blocks ====");
            for (Block b : receivedBlocks) {
                println("%5d\t0x%08x\t%s", b.getSerialNumber(), b.getWalletNumber(), b.binDump().replace("\n", "  "));
            }
        }

        private void sendRequest(int cmd, DataOutputStream dos) throws IOException {
        	self.lastSeenTS = (int)(System.currentTimeMillis() / 1000);
        	
            dos.writeInt(cmd);
            dos.writeInt(BEEF_BEEF);
            int activeNodes = nodes.size();
            // TODO(students): calculate number of active (not new) nodes
            dos.writeInt(activeNodes);
            for(NodeInfo node : nodes) {
            	byte[] bytes = nodeBuilder(node);
            	dos.write(bytes);

            }
            // TODO(students): sendRequest data of active (not new) nodes
            dos.writeInt(DEAD_DEAD);
            int blockChain_size = 0;
            dos.writeInt(blockChain_size);
            for(Block block : blocks) {
            	dos.write(block.data);
            }
            // TODO(students): sendRequest data of blocks
        }
    }


    public static void sendReceive(String host, int port){
        try {
            log("INFO - Sending request message to %s:%d", host, port);
            Socket soc = new Socket(host, port);
            ClientConnection connection = new ClientConnection(soc);
            connection.sendReceive();
        } catch (IOException e) {
            log("WARN - open socket exception connecting to %s:%d: %s", host, port, e.toString());
        }
    
    }
	public static void main(String[] argv) {
		// TODO Auto-generated method stub
        if (argv.length != 1 || !argv[0].contains(":")){
            println("ERROR - please provide HOST:PORT");
            return;
        }
        String[] parts = argv[0].split(":");
        String addr = parts[0];
        int port = Integer.parseInt(parts[1]);
        sendReceive(addr, port);
	}

}