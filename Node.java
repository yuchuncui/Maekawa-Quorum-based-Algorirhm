import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class Node implements Runnable {
	public interface Waitable {
		public boolean condition();
	}

	public void await(Waitable w) {
		while (!w.condition()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	int id = 0;
	int msgCount = 0;
	static AtomicInteger ts = new AtomicInteger();
	Queue<Request> lockMsgQue = null;
	Queue<Request> failMsgQue = null;
	boolean in_cs = false;
	boolean ReleSent = false;
	int req_quota = 0;
	ArrayList<Node> intersct = null;
	public Queue<Request> messageQue = null;

	public void init(int id, ArrayList<Node> I, int reqQuota) {
		this.id = id;
		intersct = I;
		req_quota = reqQuota;
		lockMsgQue = new ConcurrentLinkedQueue<Request>();
		failMsgQue = new ConcurrentLinkedQueue<Request>();
		messageQue = new ConcurrentLinkedQueue<Request>();
	}

	@Override
	public void run() {
		new Thread(new MsgListener()).start();
		for (int i = 0; i < req_quota; i++) {
			CS();
		}
		while (Driver.numOfReqDone != Driver.totalReq) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	void onLock(Request r) {
		lockMsgQue.add(r);
	}

	void onFail(Request r) {
		failMsgQue.add(r);
	}
	
	void onInquire(Request r) {
		if (ReleSent == false) {
			if (failMsgQue.size() > 0) {
				sendMsg(r.node, "yield", 0);
				lockMsgQue.remove(r.node);
			}
		} else if (lockMsgQue.size() >= intersct.size()) {
			if (in_cs == false) {
				sendMsg(r.node, "release", 0);
				ReleSent = true;
			}
		}
	}

	class MsgListener implements Runnable {

		@Override
		public void run() {
			while (Driver.numOfReqDone != Driver.totalReq) {
				Request r = messageQue.poll();
				if (r != null) {
					Driver.msgCount.incrementAndGet();
					System.out.println(id + " receives " + r.type + " from " + r.node.id + ".");
					if (r.type.equals("lock")) {
						onLock(r);
					} else if (r.type.equals("fail")) {
						onFail(r);
					} else if (r.type.equals("inquire")) {
						onInquire(r);
					}
					else {
						System.out.println("Msg type error.");
					}
				}
			}
		}
	}

	protected void sendMsg(Node n, String type, int ts) {
		n.messageQue.add(new Request(this, ts, type));
	}

	public void CS() {
		int cur_ts = ts.getAndIncrement();
		for (Node n : intersct) {
			sendMsg(n, "request", cur_ts);
		}
		Driver.printToWeb(this.id + " requesting CS");
		System.out.println(this.id + " requesting CS");
		await(new Waitable() {

			@Override
			public boolean condition() {
				return lockMsgQue.size() >= intersct.size();
			}
		});
		Driver.printToWeb(this.id + " in CS");
		System.out.println(this.id + " in CS");
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
		}
		Driver.printToWeb(this.id + " exits CS");
		System.out.println(this.id + " exits CS");
		for (Node n : intersct) {
			sendMsg(n, "release", cur_ts);
		}
		lockMsgQue.clear();
		failMsgQue.clear();
		ReleSent = false;		
		Driver.numOfReqDone++;
	}

}
