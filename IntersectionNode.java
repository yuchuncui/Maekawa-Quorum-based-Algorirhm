import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;

public class IntersectionNode extends Node {

	Request cur_locking_request = null;
	PriorityBlockingQueue<Request> waitingQue = null;

	public IntersectionNode() {
		super();
	}

	public void init(int id, ArrayList<Node> I, int reqQuota) {
		super.init(id, I, reqQuota);
		Comparator<Request> comparator = new RequestComparator();
		waitingQue = new PriorityBlockingQueue<Request>(3, comparator);
	}

	void onRequest(Request r) {
		if (cur_locking_request == null) {
			sendMsg(r.node, "lock", 0);
			cur_locking_request = r;
		} else {
			waitingQue.add(r);
			Request item = waitingQue.peek();
			if (r.ts > cur_locking_request.ts || r.ts > item.ts) {
				sendMsg(r.node, "fail", 0);
			} else {
				sendMsg(r.node, "inquire", cur_locking_request.ts);
			}
		}
	}

	void onYield(Request r) {
		Request tempReq = cur_locking_request;
		cur_locking_request = waitingQue.poll();
		waitingQue.add(tempReq);
		sendMsg(cur_locking_request.node, "lock", 0);
	}

	void onRelease(Request r) {
		if (waitingQue.size() > 0) {
			cur_locking_request = waitingQue.poll();
			sendMsg(cur_locking_request.node, "lock", 0);
		} else {
			cur_locking_request = null;
		}
	}
	
	@Override
	public void run() {
		new Thread(new IntersctMsgListener()).start();
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

	class IntersctMsgListener implements Runnable {

		@Override
		public void run() {
			while (Driver.numOfReqDone != Driver.totalReq) {
				Request r = messageQue.poll();
				if (r != null) {
					Driver.msgCount.incrementAndGet();
					System.out.println(id + " receives " + r.type + " from " + r.node.id + ".");
					switch (r.type) {
					case "request":
						onRequest(r);
						break;
					case "lock":
						onLock(r);
						break;
					case "inquire":
						onInquire(r);
						break;
					case "fail":
						onFail(r);
						break;
					case "yield":
						onYield(r);
						break;
					case "release":
						onRelease(r);
						break;
					}
				}
			}

		}
	}

}

class RequestComparator implements Comparator<Request> {

	public int compare(Request req0, Request req1) {
		if (req0.ts > req1.ts) {
			return 1;
		} else if (req0.ts < req1.ts) {
			return -1;
		} else {
			if (req0.node.id > req1.node.id) {
				return 1;
			} else if (req0.node.id < req1.node.id) {
				return -1;
			}
		}
		return 0;
	}
}
