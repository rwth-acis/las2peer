package i5.las2peer.persistency;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import i5.las2peer.api.exceptions.ArtifactNotFoundException;
import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.security.Agent;
import i5.las2peer.security.UserAgent;
import i5.las2peer.testing.MockAgentFactory;
import i5.las2peer.testing.TestSuite;

public class EnvelopeEvaluation {

	private final static String DATA_DIR = "evaluation/data/";

	private ArrayList<PastryNodeImpl> nodes;

	@Rule
	public TestName name = new TestName();

	@Before
	public void startNetwork() {
		try {
			// start test node
			nodes = TestSuite.launchNetwork(30);
			System.out.println("Test network started");
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@After
	public void stopNetwork() {
		if (nodes != null) {
			for (PastryNodeImpl node : nodes) {
				node.shutDown();
			}
			nodes = null;
		}
	}

	public void testStartStopNetwork() {
		// just as time reference ...
	}

	/**
	 * This test generates data to check if there is a correlation between the time to store an envelope and the number
	 * of previous store operations on this identifier. In each iteration a random node is used for the store operation.
	 */
	@Test
	public void timeToStoreVSnumberOfEnvelopesStored() {
		try {
			final int numOfOperations = 1000;
			final ArrayList<Long> writes = new ArrayList<>();
			final ArrayList<Long> fetches = new ArrayList<>();
			final String testIdentifier = "test";
			final UserAgent smith = MockAgentFactory.getAdam();
			smith.unlockPrivateKey("adamspass");
			for (int c = 0; c < numOfOperations; c++) {
				// pick a random node
				PastryNodeImpl node = nodes.get(new Random().nextInt(nodes.size()));
				long startTime = System.currentTimeMillis();
				Envelope env = null;
				try {
					// first check if a previous version for the given identifier exists
					env = node.fetchEnvelope(testIdentifier);
					Assert.assertEquals(c, env.getVersion());
					// create a subsequent envelope with the new content
					env = node.createEnvelope(env, "Hello World " + c);
				} catch (ArtifactNotFoundException e) {
					env = node.createEnvelope(testIdentifier, "Hello World " + c);
				}
				long fetchDone = System.currentTimeMillis();
				fetches.add(fetchDone - startTime);
				long storeStart = System.currentTimeMillis();
				// store the new version in the network
				node.storeEnvelope(env, smith);
				long stopTime = System.currentTimeMillis();
				writes.add(stopTime - storeStart);
			}
			BufferedWriter bw = new BufferedWriter(
					new FileWriter(DATA_DIR + "results_store_" + numOfOperations + ".csv"));
			bw.write("x,fetchms,writems\n");
			for (int c = 0; c < numOfOperations; c++) {
				bw.write((c + 1) + "," + fetches.get(c) + "," + writes.get(c) + "\n");
			}
			bw.close();
//			System.out.println("Average time spend to store an envelope " + average + " milliseconds");
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	/**
	 * This test generates data to check if there is a correlation between the time to store an envelope and the number
	 * of threads simultaneously storing envelopes.
	 * 
	 * ALL operations are performed within the same network, which might have effects too. See other results for
	 * comparison.
	 */
	@Test
	public void storeEnvelopesSimultaenously() {
		try {
			// use 10 threads
			storeEnvelopesSimultaenously(10);
			// use 20 threads
			storeEnvelopesSimultaenously(20);
			// use 50 threads
			storeEnvelopesSimultaenously(50);
			// use 100 threads
			storeEnvelopesSimultaenously(100);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	private void storeEnvelopesSimultaenously(final int numOfThreads) throws Exception {
		final Random r = new Random();
		final UserAgent smith = MockAgentFactory.getAdam();
		smith.unlockPrivateKey("adamspass");
		final ArrayList<Long> times = new ArrayList<>(numOfThreads);
		final ArrayList<Thread> threads = new ArrayList<>(numOfThreads);
		for (int c = 0; c < numOfThreads; c++) {
			threads.add(new Thread() {
				@Override
				public void run() {
					try {
						PastryNodeImpl node = nodes.get(r.nextInt(nodes.size()));
						String id = Long.toString(r.nextLong());
						Envelope toStore = node.createUnencryptedEnvelope(id, "This is " + id);
						long start = System.currentTimeMillis();
						node.storeEnvelope(toStore, smith);
						long timediff = System.currentTimeMillis() - start;
						synchronized (times) {
							times.add(timediff);
						}
					} catch (Exception e) {
						e.printStackTrace();
						Assert.fail(e.toString());
					}
				}
			});
		}
		ExecutorService pool = Executors.newFixedThreadPool(16);
		for (Thread thread : threads) {
			pool.execute(thread);
		}
		System.out.println("Waiting up to 60 minutes for all store operations to complete ...");
		pool.shutdown();
		pool.awaitTermination(60, TimeUnit.MINUTES);
		long allTime = 0;
		for (Long timediff : times) {
			allTime += timediff;
		}
		float average = ((float) allTime) / numOfThreads;
		int c = 1;
		BufferedWriter bw = new BufferedWriter(new FileWriter(DATA_DIR + "results_parallel_" + numOfThreads + ".csv"));
		bw.write("x,ms\n");
		for (Long timediff : times) {
			bw.write(c + "," + Long.toString(timediff) + "\n");
			c++;
		}
		bw.close();
		System.out.println("Average time spend to store an envelope " + average + " milliseconds");
	}

	/**
	 * This test generates data to check if there is a correlation between the envelopes size and the time to store.
	 * 
	 * ALL operations are performed within the same network, which might have effects too. See other results for
	 * comparison.
	 */
	@Test
	public void storeEnvelopeSizeCorrelation() {
		try {
			final Random r = new Random();
			final UserAgent smith = MockAgentFactory.getAdam();
			smith.unlockPrivateKey("adamspass");
			// store envelope of ~100 kB (including overhead) for 10 times
			storeEnvelopeSize(r, 100, smith, 10);
			// store envelope of ~200 kB (including overhead) for 10 times
			storeEnvelopeSize(r, 200, smith, 10);
			// store envelope of ~500 kB (including overhead) for 10 times
			storeEnvelopeSize(r, 500, smith, 10);
			// store envelope of ~1000 kB (including overhead) for 10 times
			storeEnvelopeSize(r, 1000, smith, 10);
			// store envelope of ~2000 kB (including overhead) for 10 times
			storeEnvelopeSize(r, 2000, smith, 10);
			// store envelope of ~5000 kB (including overhead) for 10 times
			storeEnvelopeSize(r, 5000, smith, 10);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	private void storeEnvelopeSize(Random r, int sizeKB, Agent smith, int repetitions) throws Exception {
		final ArrayList<Float> speeds = new ArrayList<>(repetitions);
		for (int c = 0; c < repetitions; c++) {
			PastryNodeImpl node = nodes.get(r.nextInt(nodes.size()));
			String id = Long.toString(r.nextLong());
			byte[] content = new byte[(sizeKB - 2) * 1000];
			r.nextBytes(content);
			Envelope toStore = node.createUnencryptedEnvelope(id, content);
			long start = System.currentTimeMillis();
			node.storeEnvelope(toStore, smith);
			long stop = System.currentTimeMillis();
			float timediff = stop - start;
			speeds.add(content.length / 1000 / (timediff / 1000));
		}
		float allSpeeds = 0;
		for (Float speed : speeds) {
			allSpeeds += speed;
		}
		float avgSpeed = allSpeeds / speeds.size();
		int c = 1;
		BufferedWriter bw = new BufferedWriter(new FileWriter(DATA_DIR + "results_size_" + sizeKB + ".csv"));
		bw.write("x,kbpersecond\n");
		for (Float timediff : speeds) {
			bw.write(c + "," + Float.toString(timediff) + "\n");
			c++;
		}
		bw.close();
		System.out.println("Average speed storing an envelope " + avgSpeed + " kB / s");
	}

	/**
	 * This test generates data to check if there is a correlation between different data types used inside envelopes.
	 * 
	 * ALL operations are performed within the same network, which might have effects too. See other results for
	 * comparison.
	 */
	@Test
	public void storeEnvelopeTypeCorrelation() {
		try {
			final Random r = new Random();
			final UserAgent smith = MockAgentFactory.getAdam();
			smith.unlockPrivateKey("adamspass");
			storeEnvelopeTypeByte(r, smith, 50);
			storeEnvelopeTypeString(r, smith, 50);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	private void storeEnvelopeTypeByte(Random r, Agent smith, int repetitions) throws Exception {
		final ArrayList<Float> speeds = new ArrayList<>(repetitions);
		for (int c = 0; c < repetitions; c++) {
			PastryNodeImpl node = nodes.get(r.nextInt(nodes.size()));
			String id = Long.toString(r.nextLong());
			byte[] content = new byte[498000];
			r.nextBytes(content);
			Envelope toStore = node.createUnencryptedEnvelope(id, content);
			long start = System.currentTimeMillis();
			node.storeEnvelope(toStore, smith);
			long stop = System.currentTimeMillis();
			float timediff = stop - start;
			speeds.add(content.length / (timediff / 1000));
		}
		float allSpeeds = 0;
		for (Float speed : speeds) {
			allSpeeds += speed;
		}
		float avgSpeed = allSpeeds / speeds.size();
		int c = 1;
		BufferedWriter bw = new BufferedWriter(new FileWriter(DATA_DIR + "results_type_bytes.csv"));
		bw.write("x,kbpersecond\n");
		for (Float timediff : speeds) {
			bw.write(c + "," + Float.toString(timediff) + "\n");
			c++;
		}
		bw.close();
		System.out.println("Average speed storing an envelope with content bytes " + avgSpeed + " kB / s");
	}

	private void storeEnvelopeTypeString(Random r, Agent smith, int repetitions) throws Exception {
		final ArrayList<Float> speeds = new ArrayList<>(repetitions);
		for (int c = 0; c < repetitions; c++) {
			PastryNodeImpl node = nodes.get(r.nextInt(nodes.size()));
			String id = Long.toString(r.nextLong());
			byte[] bytes = new byte[498000];
			r.nextBytes(bytes);
			String content = new String(bytes);
			Envelope toStore = node.createUnencryptedEnvelope(id, content);
			long start = System.currentTimeMillis();
			node.storeEnvelope(toStore, smith);
			long stop = System.currentTimeMillis();
			float timediff = stop - start;
			speeds.add(bytes.length / (timediff / 1000));
		}
		float allSpeeds = 0;
		for (Float speed : speeds) {
			allSpeeds += speed;
		}
		float avgSpeed = allSpeeds / speeds.size();
		int c = 1;
		BufferedWriter bw = new BufferedWriter(new FileWriter(DATA_DIR + "results_type_string.csv"));
		bw.write("x,kbpersecond\n");
		for (Float timediff : speeds) {
			bw.write(c + "," + Float.toString(timediff) + "\n");
			c++;
		}
		bw.close();
		System.out.println("Average speed storing an envelope with content String " + avgSpeed + " kB / s");
	}

}
