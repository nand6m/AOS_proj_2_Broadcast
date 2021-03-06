import java.util.ArrayList;
import java.util.HashMap;

public class spanningTreeNode implements MsgListener
{
	int parent;
	ArrayList<Integer> children;
	int ackReceived;
	int nodeId;
	//Map to get sender from nodeId
	HashMap<Integer, Sender> senders;
	boolean terminated;

	public synchronized void addSender(int neighbor, Sender s)
	{
		senders.put(neighbor, s);
	}	

	public spanningTreeNode(int myNode)
	{
		this.nodeId = myNode;
		this.senders = new HashMap<Integer, Sender>();
		parent = -1;
		children = new ArrayList<Integer>();
		ackReceived = 0;
		terminated = false;
	}

	public spanningTreeNode(int myNode, HashMap<Integer, Sender> neighbors)
	{
		this.nodeId = myNode;
		this.senders = neighbors;
		parent = -1;
		children = new ArrayList<Integer>();
		ackReceived = 0;
	}

	public synchronized boolean receive(StreamMsg m)
	{
		if(m.type == MsgType.PACK)
		{
			children.add(m.sourceNodeId);
			ackReceived++;

		}
		if(m.type == MsgType.NACK)
		{
			ackReceived++;
		}
		if(m.type == MsgType.parentRequest)
		{
			if(parent == -1)
			{
				parent = m.sourceNodeId;
				sendParentRequests();
			}
			else
			{
				sendNACK(m.sourceNodeId);
			}
		}
		if(ackReceived >= senders.size() - 1)
		{
			if(nodeId == parent)
			{
				if(ackReceived == senders.size())
				{
					terminated = true;
					System.out.println("Children: " + children);
				}	
			}
			else
			{
				terminated = true;
				sendPACK();
				System.out.println("Parent:" + parent);
				System.out.println("Children: " + children);
			}
		}
		return true;		
	}

	public synchronized void initiateConstruction()
	{
		parent = this.nodeId;
		sendParentRequests();
	}

	public synchronized void sendPACK()
	{
		StreamMsg m = new StreamMsg();
		m.type = MsgType.PACK;
		m.sourceNodeId = nodeId;
		senders.get(parent).send(m);
	}

	public synchronized void sendNACK(int destination)
	{
		StreamMsg m = new StreamMsg();
		m.type = MsgType.NACK;
		m.sourceNodeId = nodeId;
		senders.get(destination).send(m);
	}

	synchronized void sendParentRequests()
	{
		StreamMsg m = new StreamMsg();
		m.type = MsgType.parentRequest;
		m.sourceNodeId = nodeId;
		senders.forEach((id, sender) -> {if(id != parent) sender.send(m);});
	}

	public synchronized  boolean isTerminated(){
		return terminated;
	}
}
