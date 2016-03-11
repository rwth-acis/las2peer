package i5.las2peer.security;

import java.io.File;
import java.io.FilePermission;
import java.lang.reflect.ReflectPermission;
import java.security.Permission;
import java.util.HashSet;
import java.util.Set;

import i5.las2peer.execution.L2pThread;
import i5.las2peer.logging.L2pLogger;

public class L2pSecurityManager extends SecurityManager {

	private static final L2pLogger logger = L2pLogger.getInstance(L2pSecurityManager.class.getName());
	// fetch class at startup, because instanceof creates a permission request loop
	private static final Class<?> L2PTHREAD_CLASS = L2pThread.class;

	private static final Set<String> clsPaths = new HashSet<>();

	public L2pSecurityManager() {
		String CLASS_PATH = System.getProperty("java.class.path");
		String SEPARATOR = System.getProperty("path.separator");
		String[] classpaths = CLASS_PATH.split(SEPARATOR);
		for (String path : classpaths) {
			if (path.endsWith(File.separator)) {
				path = path.substring(0, path.lastIndexOf(File.separator));
			}
			clsPaths.add(path);
		}
	}

	@Override
	public void checkPermission(Permission perm) {
		Thread thread = Thread.currentThread();
		if (L2PTHREAD_CLASS.isInstance(thread)) {
			// whitelist read file permission for local classpath
			if (perm instanceof FilePermission) {
				for (String action : perm.getActions().split(",")) {
					if ("read".equals(action)) {
						for (String clsPath : clsPaths) {
							if (perm.getName().startsWith(clsPath)) {
								// local classpath is ok
								return;
							}
						}
					}
				}
				try {
					super.checkPermission(perm); // check policy file
				} catch (SecurityException e) {
					logger.warning("Blocked permission " + perm);
					throw e;
				}
			} else if (perm instanceof ReflectPermission) {
				logger.warning("Blocked permission " + perm);
				throw new SecurityException("Not allowed in a L2pThread");
			}
		}
	}

}
