package i5.las2peer.security;

import java.io.File;
import java.io.FilePermission;
import java.lang.reflect.ReflectPermission;
import java.security.Permission;
import java.util.HashSet;
import java.util.Set;

import i5.las2peer.logging.L2pLogger;

public class L2pSecurityManager extends SecurityManager {

	private static final L2pLogger logger = L2pLogger.getInstance(L2pSecurityManager.class.getName());
	private static final Set<String> readablePaths = new HashSet<>();
	private static final Set<String> writeablePaths = new HashSet<>();
	public static final String REFLECTION_TEST_VAR = "unchangeable";

	public L2pSecurityManager() {
		String CLASS_PATH = System.getProperty("java.class.path");
		String SEPARATOR = System.getProperty("path.separator");
		String JAVA_HOME = System.getProperty("java.home");
		readablePaths.add(JAVA_HOME);
		// Linux related paths
		readablePaths.add("/dev/random");
		readablePaths.add("/dev/urandom");
		readablePaths.add("/tmp");
		// TODO add Windows paths, too
		// las2peer related files
		readablePaths.add("user.params");
		readablePaths.add("pastry.properties");
		readablePaths.add("etc/pastry.properties");
		readablePaths.add("config/pastry.properties");
		readablePaths.add("properties/pastry.properties");
		writeablePaths.add("log/");
		String[] classpaths = CLASS_PATH.split(SEPARATOR);
		for (String path : classpaths) {
			if (path.endsWith(File.separator)) {
				path = path.substring(0, path.lastIndexOf(File.separator));
			}
			readablePaths.add(path);
		}
	}

	@Override
	public void checkPermission(Permission perm) {
		// whitelist read file permission for local classpath
		if (perm instanceof FilePermission) {
			for (String action : perm.getActions().split(",")) {
				if ("read".equals(action)) {
					for (String path : readablePaths) {
						if (perm.getName().startsWith(path)) {
							return;
						}
					}
					for (String path : writeablePaths) {
						if (perm.getName().startsWith(path)) {
							return;
						}
					}
				} else if ("write".equals(action)) {
					for (String path : writeablePaths) {
						if (perm.getName().startsWith(path)) {
							return;
						}
					}
				}
			}
			try {
				super.checkPermission(perm);
			} catch (SecurityException e) {
				logger.warning("Blocked permission " + perm);
				throw e;
			}
		} else if (perm instanceof ReflectPermission) {
			// TODO block ReflectPermissions regarding this security manager
//			logger.warning("Reflection permission " + perm);
		} else if (perm instanceof RuntimePermission) {
			// self secure this SecurityManager
			if ("setSecurityManager".equals(perm.getName()) || "createSecurityManager".equals(perm.getName())) {
				logger.warning("Blocked permission " + perm);
				throw new SecurityException("SecurityManager already set!");
			}
		}
	}

}
