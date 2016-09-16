package i5.las2peer.persistency;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import i5.las2peer.api.StorageCollisionHandler;
import i5.las2peer.api.StorageEnvelopeHandler;
import i5.las2peer.api.StorageExceptionHandler;
import i5.las2peer.api.StorageStoreResultHandler;
import i5.las2peer.api.exceptions.ArtifactNotFoundException;
import i5.las2peer.api.exceptions.EnvelopeAlreadyExistsException;
import i5.las2peer.api.exceptions.StopMergingException;
import i5.las2peer.api.exceptions.StorageException;
import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.security.GroupAgent;
import i5.las2peer.security.UserAgent;
import i5.las2peer.testing.MockAgentFactory;
import i5.las2peer.testing.TestSuite;

public class EnvelopeTest {

	private ArrayList<PastryNodeImpl> nodes;
	private boolean asyncTestState;

	private static class ExceptionHandler implements StorageExceptionHandler {
		@Override
		public void onException(Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	private static final ExceptionHandler storageExceptionHandler = new ExceptionHandler();

	@Rule
	public TestName name = new TestName();

	@Before
	public void startNetwork() {
		try {
			// start test node
			nodes = TestSuite.launchNetwork(3);
			System.out.println("Test network started");
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		asyncTestState = false;
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

	@Test
	public void testStartStopNetwork() {
		// just as time reference ...
	}

	@Test
	public void testStoreAndFetch() {
		try {
			PastryNodeImpl node1 = nodes.get(0);
			// create envelope to store in the shared network storage
			UserAgent smith = MockAgentFactory.getAdam();
			smith.unlockPrivateKey("adamspass");
			Envelope envelope1 = node1.createUnencryptedEnvelope("test", "Hello World!");
			// upload envelope
			node1.storeEnvelopeAsync(envelope1, smith, new StorageStoreResultHandler() {
				@Override
				public void onResult(Serializable serializable, int successfulOperations) {
					System.out.println(
							"Successfully stored artifact " + serializable + " " + successfulOperations + " times");
					// fetch envelope again
					System.out.println("Fetching artifact ...");
					node1.fetchEnvelopeAsync("test", new StorageEnvelopeHandler() {
						@Override
						public void onEnvelopeReceived(Envelope envelope) {
							try {
								String content = (String) envelope.getContent();
								System.out.println("Envelope content is '" + content + "'");
								asyncTestState = true;
							} catch (Exception e) {
								Assert.fail(e.toString());
								return;
							}
						}
					}, storageExceptionHandler);
				}
			}, null, storageExceptionHandler);
			// wait till the envelope was fetched
			System.out.println("Waiting ...");
			for (int n = 1; n <= 100; n++) {
				if (asyncTestState) {
					return;
				}
				Thread.sleep(100);
			}
			Assert.fail();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testStoreAndFetchBig1() {
		testStoreAndFetchBig(100000);
	}

	@Test
	public void testStoreAndFetchBig2() {
		testStoreAndFetchBig(400000);
	}

	@Test
	public void testStoreAndFetchBig3() {
		testStoreAndFetchBig(990000); // = ~1 MB
	}

	@Test
	public void testStoreAndFetchBig4() {
		testStoreAndFetchBig(2300000);
	}

	@Test
	public void testStoreAndFetchBig5() {
		testStoreAndFetchBig(4900000); // = ~5 MB
	}

	private void testStoreAndFetchBig(int datasize) {
		try {
			PastryNodeImpl node1 = nodes.get(0);
			// create envelope to store in the shared network storage
			UserAgent smith = MockAgentFactory.getAdam();
			smith.unlockPrivateKey("adamspass");
			byte[] testContent = new byte[datasize];
			// generate random data
			new Random().nextBytes(testContent);
			Envelope envelope1 = node1.createUnencryptedEnvelope("test", testContent);
			// upload envelope
			long startStore = System.currentTimeMillis();
			node1.storeEnvelopeAsync(envelope1, smith, new StorageStoreResultHandler() {
				@Override
				public void onResult(Serializable serializable, int successfulOperations) {
					long stopTime = System.currentTimeMillis();
					long storeTime = stopTime - startStore;
					System.out.println("Successfully stored artifact " + successfulOperations + " times");
					System.out.println("Stored " + testContent.length / 1000.f + " kB in " + storeTime + " ms, speed "
							+ testContent.length / storeTime + " kB/s");
					// fetch envelope again
					System.out.println("Fetching artifact ...");
					node1.fetchEnvelopeAsync("test", new StorageEnvelopeHandler() {
						@Override
						public void onEnvelopeReceived(Envelope envelope) {
							try {
								byte[] content = (byte[]) envelope.getContent();
								Assert.assertArrayEquals(testContent, content);
								asyncTestState = true;
							} catch (Exception e) {
								Assert.fail(e.toString());
								return;
							}
						}
					}, storageExceptionHandler);
				}
			}, null, storageExceptionHandler);
			// wait till the envelope was fetched
			System.out.println("Waiting ...");
			for (int n = 1; n <= 100; n++) {
				if (asyncTestState) {
					return;
				}
				Thread.sleep(400);
			}
			Assert.fail("Unexpected result");
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	/**
	 * test collision (Envelope with same identifier + version)
	 */
	@Test
	public void testCollisionWithSingleMerge() {
		try {
			PastryNodeImpl node1 = nodes.get(0);
			// create envelope to store in the shared network storage
			UserAgent smith = MockAgentFactory.getAdam();
			smith.unlockPrivateKey("adamspass");
			Envelope envelope1 = node1.createUnencryptedEnvelope("test", "Hello World 1!");
			Envelope envelope2 = node1.createUnencryptedEnvelope("test", "Hello World 2!");
			// upload envelope
			node1.storeEnvelopeAsync(envelope1, smith, new StorageStoreResultHandler() {
				@Override
				public void onResult(Serializable serializable, int successfulOperations) {
					System.out.println("Successfully stored artifact " + successfulOperations + " times");
					// store another envelope with the same identifier and version as before
					// => expect collision
					node1.storeEnvelopeAsync(envelope2, smith, new StorageStoreResultHandler() {
						@Override
						public void onResult(Serializable serializable, int successfulOperations) {
							if (successfulOperations > 0) {
								System.out.println("Successfully stored artifact " + successfulOperations + " times");
								// fetch envelope again
								System.out.println("Fetching artifact ...");
								node1.fetchEnvelopeAsync("test", new StorageEnvelopeHandler() {
									@Override
									public void onEnvelopeReceived(Envelope envelope) {
										try {
											Assert.assertEquals(envelope.getVersion(), 2);
											String content = (String) envelope.getContent();
											System.out.println("Envelope content is '" + content + "'");
											asyncTestState = true;
										} catch (Exception e) {
											Assert.fail(e.toString());
											return;
										}
									}
								}, storageExceptionHandler);
							} else {
								Assert.fail("Merging failed!");
							}
						}
					}, new StorageCollisionHandler() {
						@Override
						public Serializable onCollision(Envelope toStore, Envelope inNetwork, long numberOfCollisions)
								throws StopMergingException {
							if (numberOfCollisions > 100) {
								throw new StopMergingException(
										"Merging failed, too many (" + numberOfCollisions + ") collisions!",
										numberOfCollisions);
							}
							// we return the "merged" version of both envelopes
							// usually there one should put more effort into merging
							try {
								String result = inNetwork.getContent() + " " + toStore.getContent();
								return result;
							} catch (Exception e) {
								Assert.fail(e.toString());
								return "";
							}
						}

						@Override
						public List<PublicKey> mergeReaders(HashMap<PublicKey, byte[]> toStoreReaders,
								HashMap<PublicKey, byte[]> inNetworkReaders) {
							ArrayList<PublicKey> merged = new ArrayList<>();
							merged.addAll(toStoreReaders.keySet());
							merged.addAll(inNetworkReaders.keySet());
							return merged;
						}
					}, storageExceptionHandler);
				}
			}, null, storageExceptionHandler);
			// wait for the collision
			System.out.println("Waiting ...");
			for (int n = 1; n <= 100; n++) {
				if (asyncTestState) {
					return;
				}
				Thread.sleep(100);
			}
			Assert.fail("No collision occurred!");
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	/**
	 * test without collision manager
	 */
	@Test
	public void testCollisionWithoutCollisionManager() {
		try {
			PastryNodeImpl node1 = nodes.get(0);
			// create envelope to store in the shared network storage
			UserAgent smith = MockAgentFactory.getAdam();
			smith.unlockPrivateKey("adamspass");
			Envelope envelope1 = node1.createUnencryptedEnvelope("test", "Hello World 1!");
			Envelope envelope2 = node1.createUnencryptedEnvelope("test", "Hello World 2!");
			// upload envelope
			node1.storeEnvelopeAsync(envelope1, smith, new StorageStoreResultHandler() {
				@Override
				public void onResult(Serializable serializable, int successfulOperations) {
					System.out.println("Successfully stored artifact " + successfulOperations + " times");
					// store another envelope with the same identifier and version as before
					// => expect collision
					node1.storeEnvelopeAsync(envelope2, smith, new StorageStoreResultHandler() {
						@Override
						public void onResult(Serializable serializable, int successfulOperations) {
							Assert.fail("Exception expected!");
						}
					}, null, new StorageExceptionHandler() {
						@Override
						public void onException(Exception e) {
							if (e instanceof EnvelopeAlreadyExistsException) {
								System.out.println("Expected exception '" + e.toString() + "' received.");
								asyncTestState = true;
							} else {
								storageExceptionHandler.onException(e);
							}
						}
					});
				}
			}, null, storageExceptionHandler);
			// wait for the collision
			System.out.println("Waiting ...");
			for (int n = 1; n <= 100; n++) {
				if (asyncTestState) {
					return;
				}
				Thread.sleep(100);
			}
			Assert.fail("No collision occurred!");
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	/**
	 * test with merging cancelation
	 */
	@Test
	public void testCollisionWithMergeCancelation() {
		try {
			PastryNodeImpl node1 = nodes.get(0);
			// create envelope to store in the shared network storage
			UserAgent smith = MockAgentFactory.getAdam();
			smith.unlockPrivateKey("adamspass");
			Envelope envelope1 = node1.createUnencryptedEnvelope("test", "Hello World 1!");
			Envelope envelope2 = node1.createUnencryptedEnvelope("test", "Hello World 2!");
			// upload envelope
			node1.storeEnvelopeAsync(envelope1, smith, new StorageStoreResultHandler() {
				@Override
				public void onResult(Serializable serializable, int successfulOperations) {
					System.out.println("Successfully stored artifact " + successfulOperations + " times");
					// store another envelope with the same identifier and version as before
					// => expect collision
					node1.storeEnvelopeAsync(envelope2, smith, new StorageStoreResultHandler() {
						@Override
						public void onResult(Serializable serializable, int successfulOperations) {
							Assert.fail("Exception expected!");
						}
					}, new StorageCollisionHandler() {
						@Override
						public String onCollision(Envelope toStore, Envelope inNetwork, long numberOfCollisions)
								throws StopMergingException {
							throw new StopMergingException();
						}

						@Override
						public List<PublicKey> mergeReaders(HashMap<PublicKey, byte[]> toStoreReaders,
								HashMap<PublicKey, byte[]> inNetworkReaders) {
							return new ArrayList<>();
						}
					}, new StorageExceptionHandler() {
						@Override
						public void onException(Exception e) {
							if (e instanceof StopMergingException) {
								System.out.println("Expected exception '" + e.toString() + "' received.");
								asyncTestState = true;
							} else {
								storageExceptionHandler.onException(e);
							}
						}
					});
				}
			}, null, storageExceptionHandler);
			// wait for the collision
			System.out.println("Waiting ...");
			for (int n = 1; n <= 100; n++) {
				if (asyncTestState) {
					return;
				}
				Thread.sleep(100);
			}
			Assert.fail("No collision occurred!");
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	/**
	 * test (250 times) update content of envelope
	 */
	@Test
	public void testUpdateContent() {
		try {
			PastryNodeImpl node1 = nodes.get(0);
			UserAgent smith = MockAgentFactory.getAdam();
			smith.unlockPrivateKey("adamspass");
			Envelope updated = node1.createUnencryptedEnvelope("test", "envelope version number 1");
			node1.storeEnvelope(updated, smith);
			for (int c = 2; c <= 250; c++) {
				updated = node1.createUnencryptedEnvelope(updated, "envelope version number " + c);
				node1.storeEnvelope(updated, smith);
				Envelope fetched = node1.fetchEnvelope(updated.getIdentifier());
				Assert.assertEquals(updated.getIdentifier(), fetched.getIdentifier());
				Assert.assertEquals(updated.getVersion(), fetched.getVersion());
				Assert.assertEquals(updated.getContent(), fetched.getContent());
			}
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testFetchNonExisting() {
		try {
			PastryNodeImpl node1 = nodes.get(0);
			// fetch envelope again
			System.out.println("Fetching artifact ...");
			node1.fetchEnvelopeAsync("testtesttest", new StorageEnvelopeHandler() {
				@Override
				public void onEnvelopeReceived(Envelope envelope) {
					Assert.fail("Unexpected result (" + envelope.toString() + ")!");
				}
			}, new StorageExceptionHandler() {
				@Override
				public void onException(Exception e) {
					if (e instanceof ArtifactNotFoundException) {
						// expected exception
						System.out.println("Expected exception '" + e.toString() + "' received.");
						asyncTestState = true;
					} else {
						Assert.fail("Unexpected exception (" + e.toString() + ")!");
					}
				}
			});
			// wait till the envelope was fetched
			System.out.println("Waiting ...");
			for (int n = 1; n <= 100; n++) {
				if (asyncTestState) {
					return;
				}
				Thread.sleep(100);
			}
			Assert.fail("Exception expected!");
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testChangeContentType() {
		try {
			PastryNodeImpl node1 = nodes.get(0);
			// create envelope to store in the shared network storage
			UserAgent smith = MockAgentFactory.getAdam();
			smith.unlockPrivateKey("adamspass");
			Envelope envelope1 = node1.createUnencryptedEnvelope("test", "Hello World!");
			// upload envelope
			node1.storeEnvelope(envelope1, smith);
			// change content type to integer
			Envelope envelope2 = node1.createUnencryptedEnvelope(envelope1, 123456789);
			node1.storeEnvelope(envelope2, smith);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testContentLocking() {
		try {
			PastryNodeImpl node1 = nodes.get(0);
			UserAgent smith = MockAgentFactory.getAdam();
			smith.unlockPrivateKey("adamspass");
			final String testContent = "envelope of smith";
			Envelope original = node1.createUnencryptedEnvelope("test", testContent);
			node1.storeEnvelope(original, smith);
			UserAgent neo = MockAgentFactory.getEve();
			neo.unlockPrivateKey("evespass");
			Envelope overwritten = node1.createUnencryptedEnvelope(original, "envelope of neo");
			try {
				node1.storeEnvelope(overwritten, neo);
				Assert.fail(StorageException.class.getName() + " expected");
			} catch (StorageException e) {
				// expected store failed exception, already exists
			}
			Envelope stored = node1.fetchEnvelope("test");
			Assert.assertEquals(testContent, stored.getContent());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	// TODO Test disabled no Context in Unit tests available
//	@Test
	public void testReadWithGroup() {
		try {
			PastryNodeImpl node1 = nodes.get(0);
			UserAgent smith = MockAgentFactory.getAdam();
			smith.unlockPrivateKey("adamspass");
			GroupAgent group1 = MockAgentFactory.getGroup1();
			Assert.assertTrue(group1.isMember(smith));
			group1.unlockPrivateKey(smith);
			node1.storeAgent(group1);
			node1.registerReceiver(smith);
			final String testContent = "envelope of smith";
			Envelope groupEnv = node1.createEnvelope("test", testContent, group1);
			node1.storeEnvelope(groupEnv, group1);
			PastryNodeImpl node2 = nodes.get(1);
			Envelope fetchedEnv = node2.fetchEnvelope("test");
			String content = (String) fetchedEnv.getContent(smith);
			Assert.assertEquals(testContent, content);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

}
