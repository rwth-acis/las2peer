package i5.las2peer.persistency;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import i5.las2peer.api.persistency.EnvelopeAlreadyExistsException;
import i5.las2peer.api.persistency.EnvelopeException;
import i5.las2peer.api.persistency.EnvelopeNotFoundException;
import i5.las2peer.api.security.AgentException;
import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.security.AnonymousAgentImpl;
import i5.las2peer.security.GroupAgentImpl;
import i5.las2peer.security.UserAgentImpl;
import i5.las2peer.testing.MockAgentFactory;
import i5.las2peer.testing.TestSuite;

public class EnvelopeVersionTest {

	private ArrayList<PastryNodeImpl> nodes;
	private boolean asyncTestState;

	private class ExceptionHandler implements StorageExceptionHandler {
		@Override
		public void onException(Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	private final ExceptionHandler storageExceptionHandler = new ExceptionHandler();

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
			UserAgentImpl smith = MockAgentFactory.getAdam();
			smith.unlock("adamspass");
			EnvelopeVersion envelope1 = node1.createUnencryptedEnvelope("test", smith.getPublicKey(), "Hello World!");
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
						public void onEnvelopeReceived(EnvelopeVersion envelope) {
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
			UserAgentImpl smith = MockAgentFactory.getAdam();
			smith.unlock("adamspass");
			byte[] testContent = new byte[datasize];
			// generate random data
			new Random().nextBytes(testContent);
			EnvelopeVersion envelope1 = node1.createUnencryptedEnvelope("test", smith.getPublicKey(), testContent);
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
						public void onEnvelopeReceived(EnvelopeVersion envelope) {
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
			long timeout = System.currentTimeMillis() + 120000000L; // two minutes timeout
			while (System.currentTimeMillis() < timeout) {
				if (asyncTestState) {
					return;
				}
				Thread.sleep(200);
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
			UserAgentImpl smith = MockAgentFactory.getAdam();
			smith.unlock("adamspass");
			EnvelopeVersion envelope1 = node1.createUnencryptedEnvelope("test", smith.getPublicKey(), "Hello World 1!");
			EnvelopeVersion envelope2 = node1.createUnencryptedEnvelope("test", smith.getPublicKey(), "Hello World 2!");
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
									public void onEnvelopeReceived(EnvelopeVersion envelope) {
										try {
											Assert.assertEquals(2, envelope.getVersion());
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
						public Serializable onCollision(EnvelopeVersion toStore, EnvelopeVersion inNetwork,
								long numberOfCollisions) throws StopMergingException {
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
						public Set<PublicKey> mergeReaders(Set<PublicKey> toStoreReaders,
								Set<PublicKey> inNetworkReaders) {
							HashSet<PublicKey> merged = new HashSet<>();
							merged.addAll(toStoreReaders);
							merged.addAll(inNetworkReaders);
							return merged;
						}

						@Override
						public Set<String> mergeGroups(Set<String> toStoreGroups, Set<String> inNetworkGroups) {
							HashSet<String> merged = new HashSet<>();
							merged.addAll(toStoreGroups);
							merged.addAll(inNetworkGroups);
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
			UserAgentImpl smith = MockAgentFactory.getAdam();
			smith.unlock("adamspass");
			EnvelopeVersion envelope1 = node1.createUnencryptedEnvelope("test", smith.getPublicKey(), "Hello World 1!");
			EnvelopeVersion envelope2 = node1.createUnencryptedEnvelope("test", smith.getPublicKey(), "Hello World 2!");
			// upload first envelope
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
						private boolean testComplete = false;

						@Override
						public void onException(Exception e) {
							synchronized (this) {
								if (e instanceof EnvelopeAlreadyExistsException) {
									System.out.println("Expected exception '" + e.toString() + "' received.");
									testComplete = true;
									asyncTestState = true;
								} else if (!testComplete) { // test yet incomplete
									storageExceptionHandler.onException(e);
								}
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
			UserAgentImpl smith = MockAgentFactory.getAdam();
			smith.unlock("adamspass");
			EnvelopeVersion envelope1 = node1.createUnencryptedEnvelope("test", smith.getPublicKey(), "Hello World 1!");
			EnvelopeVersion envelope2 = node1.createUnencryptedEnvelope("test", smith.getPublicKey(), "Hello World 2!");
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
						public String onCollision(EnvelopeVersion toStore, EnvelopeVersion inNetwork,
								long numberOfCollisions) throws StopMergingException {
							throw new StopMergingException();
						}

						@Override
						public Set<PublicKey> mergeReaders(Set<PublicKey> toStoreReaders,
								Set<PublicKey> inNetworkReaders) {
							return new HashSet<>();
						}

						@Override
						public Set<String> mergeGroups(Set<String> toStoreGroups, Set<String> inNetworkGroups) {
							return new HashSet<>();
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
			UserAgentImpl smith = MockAgentFactory.getAdam();
			smith.unlock("adamspass");
			EnvelopeVersion updated = node1.createUnencryptedEnvelope("test", smith.getPublicKey(),
					"envelope version number 1");
			node1.storeEnvelope(updated, smith);
			for (int c = 2; c <= 250; c++) {
				updated = node1.createUnencryptedEnvelope(updated, "envelope version number " + c);
				node1.storeEnvelope(updated, smith);
				EnvelopeVersion fetched = node1.fetchEnvelope(updated.getIdentifier());
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
				public void onEnvelopeReceived(EnvelopeVersion envelope) {
					Assert.fail("Unexpected result (" + envelope.toString() + ")!");
				}
			}, new StorageExceptionHandler() {
				@Override
				public void onException(Exception e) {
					if (e instanceof EnvelopeNotFoundException) {
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
			UserAgentImpl smith = MockAgentFactory.getAdam();
			smith.unlock("adamspass");
			EnvelopeVersion envelope1 = node1.createUnencryptedEnvelope("test", smith.getPublicKey(), "Hello World!");
			// upload envelope
			node1.storeEnvelope(envelope1, smith);
			// change content type to integer
			EnvelopeVersion envelope2 = node1.createUnencryptedEnvelope(envelope1, 123456789);
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
			UserAgentImpl smith = MockAgentFactory.getAdam();
			smith.unlock("adamspass");
			final String testContent = "envelope of smith";
			EnvelopeVersion original = node1.createUnencryptedEnvelope("test", smith.getPublicKey(), testContent);
			node1.storeEnvelope(original, smith);
			UserAgentImpl neo = MockAgentFactory.getEve();
			neo.unlock("evespass");
			EnvelopeVersion overwritten = node1.createUnencryptedEnvelope(original, "envelope of neo");
			try {
				node1.storeEnvelope(overwritten, neo);
				Assert.fail(EnvelopeException.class.getName() + " expected");
			} catch (EnvelopeException e) {
				// expected store failed exception, already exists
			}
			EnvelopeVersion stored = node1.fetchEnvelope("test");
			Assert.assertEquals(testContent, stored.getContent());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testReadWithGroup() {
		try {
			// Agent Smith (member of group1) stores an envelope
			PastryNodeImpl node1 = nodes.get(0);
			UserAgentImpl smith = MockAgentFactory.getAdam();
			smith.unlock("adamspass");
			smith.notifyRegistrationTo(node1); // workaround for missing context during tests
			// Agent Neo (member group1, too) reads the stored envelope
			UserAgentImpl neo = MockAgentFactory.getEve();
			neo.unlock("evespass");
			neo.notifyRegistrationTo(node1); // workaround for missing context during tests
			GroupAgentImpl group1 = MockAgentFactory.getGroup1();
			Assert.assertTrue(group1.hasMember(smith));
			Assert.assertTrue(group1.hasMember(neo));
			group1.unlock(smith);
			node1.storeAgent(group1);
			final String testContent = "content from smith";
			EnvelopeVersion groupEnv = node1.createEnvelope("test", smith.getPublicKey(), testContent, group1);
			node1.storeEnvelope(groupEnv, smith);
			// Agent Neo (same group) reads the envelope
			PastryNodeImpl node2 = nodes.get(1);
			EnvelopeVersion fetchedEnv = node2.fetchEnvelope("test");
			String content = (String) fetchedEnv.getContent(node2.getAgentContext(neo));
			Assert.assertEquals(testContent, content);
			Assert.assertEquals(groupEnv.getReaderGroupIds(), fetchedEnv.getReaderGroupIds());
			// Agent Smith updates the envelope
			final String testContent2 = "content from Smith 2";
			EnvelopeVersion groupEnv2 = node1.createEnvelope(groupEnv, testContent2);
			Assert.assertEquals(fetchedEnv.getReaderGroupIds(), groupEnv2.getReaderGroupIds());
			node1.storeEnvelope(groupEnv2, smith);
			// Agent Neo reads the content again
			EnvelopeVersion fetchedEnv2 = node2.fetchEnvelope("test");
			String content2 = (String) fetchedEnv2.getContent(node2.getAgentContext(neo));
			Assert.assertEquals(testContent2, content2);
			Assert.assertEquals(groupEnv2.getReaderGroupIds(), fetchedEnv2.getReaderGroupIds());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testWriteWithGroup() {
		try {
			// Agent Smith (member of group1) stores an envelope owned by group1
			PastryNodeImpl node1 = nodes.get(0);
			UserAgentImpl smith = MockAgentFactory.getAdam();
			smith.unlock("adamspass");
			smith.notifyRegistrationTo(node1); // workaround for missing context during tests
			// Agent Neo (member group1, too) reads the stored envelope
			UserAgentImpl neo = MockAgentFactory.getEve();
			neo.unlock("evespass");
			neo.notifyRegistrationTo(node1); // workaround for missing context during tests
			GroupAgentImpl group1 = MockAgentFactory.getGroup1();
			Assert.assertTrue(group1.hasMember(smith));
			Assert.assertTrue(group1.hasMember(neo));
			group1.unlock(smith);
			node1.storeAgent(group1);
			final String testContent = "content from smith";
			EnvelopeVersion groupEnv = node1.createEnvelope("test", group1.getPublicKey(), testContent);
			node1.storeEnvelope(groupEnv, group1);
			// Agent Neo (same group) reads the envelope ...
			PastryNodeImpl node2 = nodes.get(1);
			EnvelopeVersion fetchedEnv = node2.fetchEnvelope("test");
			String content = (String) fetchedEnv.getContent(node2.getAgentContext(neo));
			Assert.assertEquals(testContent, content);
			// ... and updates it.
			final String testContent2 = "content from neo";
			EnvelopeVersion updated = node2.createEnvelope(fetchedEnv, testContent2);
			node2.storeEnvelope(updated, group1);
			// Agent Smith reads content from Neo
			EnvelopeVersion fetched2 = node1.fetchEnvelope("test");
			String content2 = (String) fetched2.getContent(node1.getAgentContext(smith));
			Assert.assertEquals(testContent2, content2);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testStoreAnonymous() {
		try {
			PastryNodeImpl node = nodes.get(0);
			AnonymousAgentImpl anonymous = AnonymousAgentImpl.getInstance();
			anonymous.unlock(AnonymousAgentImpl.PASSPHRASE);
			node.storeAgent(anonymous);
			Assert.fail("Exception expected");
		} catch (AgentException e) {
			// expected
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

}
