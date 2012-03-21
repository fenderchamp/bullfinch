package iinteractive.bullfinch.minion;

import iinteractive.bullfinch.ConfigurationException;
import iinteractive.bullfinch.PerformanceCollector;
import iinteractive.bullfinch.ProcessTimeoutException;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.TimeoutException;

import net.rubyeye.xmemcached.exception.MemcachedException;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * QueueMonitor is a convenience class for writing minions that subscribe to
 * kestrel queues for message processing.  It wraps the get/process/confirm
 * loop.
 *
 * The setup of kestrel and connection to same are provided by KestrelBased,
 * so you should consult that class for configuration information.
 *
 * Extenders of this class should implement the handle method.  The aforementioned
 * loop calls handle with a HashMap<String,Object> containing the incoming
 * request decoded from JSON.
 *
 * @author gphat
 *
 */
public abstract class QueueMonitor extends KestrelBased {

	static Logger logger = LoggerFactory.getLogger(QueueMonitor.class);
	private String queueName;
	private int timeout;
    private JSONParser parser = new JSONParser();

	public QueueMonitor(PerformanceCollector collector) {

		super(collector);
	}

	public void setTimeout(int timeout) {

		this.timeout = timeout;
	}

	@Override
	public void configure(HashMap<String,Object> config) throws Exception {

		super.configure(config);
		queueName = (String) config.get("subscribe_to");
		if(queueName == null) {
			throw new ConfigurationException("Each worker must have a subscribe_to!");
		}

		Long timeoutLng = (Long) config.get("timeout");
		if(timeoutLng == null) {
			throw new ConfigurationException("Each worker must have a timeout!");
		}
		timeout = timeoutLng.intValue();
	}

	/**
	 * Run the thread.  This method will call a get() on the queue, waiting on
	 * the timeout.  When it gets a message it will pass it off to the worker
	 * to handle.
	 */
	@Override
	public void run() {

		logger.debug("Began minion");
		if(this.client == null) {
			System.out.println("##### NULL CLIENT");
		}

		while(this.shouldContinue()) {
			try {
				logger.debug("Opening item from queue");
				// We're adding 1000 (1 second) to the queue timeout to let
				// xmemcached have some breathing room. Kestrel will timeout
				// by itself.
				String val = this.client.get(this.queueName + "/t=" + this.timeout + "/open", this.timeout);

				if (val != null) {
					try {
						process(val);
					} catch (ProcessTimeoutException e) {
						// ignore a timeout exception
					}
					// confirm the item we took off the queue.
					logger.debug("Closing item from queue");
					this.client.get(this.queueName + "/close");
				}
			} catch (TimeoutException e) {
				logger.debug("Timeout expired, cycling");
			} catch (MemcachedException e) {
				logger.error("Caught exception from memcached", e);
				/* Lets sleep for 5 seconds so as not to hammer the xmemcached
				 * library.
				 */
				try { Thread.sleep(5000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
			} catch(RuntimeException e) {
				/* Rethrow RTE */
				throw(e);
			} catch (Exception e) {
				logger.error("Unknown exception in processing loop", e);
				/* Sleep for longer since we have no idea what's broken. */
				try { Thread.sleep(3000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
			}
		}
	}

	private void process(String val) throws ProcessTimeoutException {
		JSONObject request = null;

		logger.debug("Got item from queue:\n" + val);

		try {
			request = (JSONObject) parser.parse(new StringReader(val));
		} catch (Error e) {
			logger.warn("unable to parse input, ignoring");
			return;
		} catch (Exception e) {
			logger.warn("unable to parse input, ignoring");
			return;
		}

		if(request == null) {
			logger.warn("Failed to parse request, ignoring");
			return;
		}

		// Try and get the response queue.
		String responseQueue = (String) request.get("response_queue");
		if(responseQueue == null) {
			logger.debug("request did not contain a response queue");
			return;
		}
		logger.debug("Response will go to " + responseQueue);

		// Get a list of items back from the worker
		@SuppressWarnings("unchecked")
		Iterator<String> items = this.handle(collector, request);

		// Send those items back into the queue

		long start = System.currentTimeMillis();
		while(items.hasNext()) {
			sendMessage(responseQueue, items.next());
		}
		collector.add(
			"ResultSet iteration and queue insertion",
			System.currentTimeMillis() - start,
			(String) request.get("tracer")
		);
		// Top if off with an EOF.
		sendMessage(responseQueue, "{ \"EOF\":\"EOF\" }");
	}

	/**
	 * Handle a request. Classes extending QueueMonitoring minion should
	 * implement this method.
	 *
	 * @param collector A PerformanceCollector instance
	 * @param request The request!
	 * @return An iterator of strings, suitable for returning to the caller.
	 * @throws Exception
	 */
	public abstract Iterator<String> handle(PerformanceCollector collector, HashMap<String,Object> request) throws ProcessTimeoutException;
}
