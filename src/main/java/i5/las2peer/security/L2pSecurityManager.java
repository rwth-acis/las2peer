package i5.las2peer.security;

import java.io.File;
import java.io.FilePermission;
import java.net.SocketPermission;
import java.security.Permission;
import java.util.PropertyPermission;

import i5.las2peer.logging.L2pLogger;

public class L2pSecurityManager extends SecurityManager {

	private static final L2pLogger logger = L2pLogger.getInstance(L2pSecurityManager.class.getName());

	// reminder: reflection makes member variables unsafe

	public L2pSecurityManager() {
		System.setProperty("java.security.policy", "las2peer.policy");
		// TODO check if the policy file exists, otherwise extract it from jar
	}

	@Override
	public void checkPermission(Permission perm) {
		if (perm instanceof FilePermission) {
			// block access to las2peer.policy file
			if (perm.getName().contains("las2peer.policy")) {
				logger.warning("Blocked permission " + perm);
				throw new SecurityException("Policy file access restricted!");
			}
			// allow read on java.home
			String javaHome = System.getProperty("java.home").toLowerCase();
			if (perm.getName().toLowerCase().startsWith(javaHome)) {
				return;
			}
			// allow read on java.class.path
			String javaClassPath = System.getProperty("java.class.path");
			String pathSeparator = System.getProperty("path.separator");
			String[] clsSplit = javaClassPath.split(pathSeparator);
			for (String path : clsSplit) {
				int ind = path.lastIndexOf(File.separator);
				if (ind != -1) {
					path = path.substring(0, ind);
				}
				if (perm.getName().startsWith(path)) {
					return;
				}
			}
			// check policy file
			try {
				super.checkPermission(perm);
			} catch (SecurityException e) {
				logger.warning("Blocked permission " + perm);
				throw e;
			}
		} else if (perm instanceof SocketPermission) {
			// check policy file
			try {
				super.checkPermission(perm);
			} catch (SecurityException e) {
				logger.warning("Blocked permission " + perm);
				throw e;
			}
		} else if (perm instanceof PropertyPermission) {
			String lName = perm.getName().toLowerCase();
			if ("java.security.policy".equals(lName)) {
				logger.warning("Blocked permission " + perm);
				throw new SecurityException("Policy file already set!");
			} else if (perm.getActions() == null || perm.getActions().contains("write")) {
				// readonly properties
				if ("java.home".equals(lName)) {
					logger.warning("Blocked permission " + perm);
					throw new SecurityException("Java home is readonly!");
				} else if ("java.class.path".equals(lName)) {
					logger.warning("Blocked permission " + perm);
					throw new SecurityException("Class path is readonly!");
				}
			}
		} else if (perm instanceof RuntimePermission) {
			// self secure this SecurityManager
			if ("setSecurityManager".equals(perm.getName())) {
				logger.warning("Blocked permission " + perm);
				throw new SecurityException("SecurityManager already set!");
			}
		}
	}

}
