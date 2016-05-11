package i5.las2peer.security;

import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.SocketPermission;
import java.net.URLDecoder;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.security.Permission;
import java.util.PropertyPermission;
import java.util.logging.Level;

import i5.las2peer.logging.L2pLogger;

public class L2pSecurityManager extends SecurityManager {

	private static final L2pLogger logger = L2pLogger.getInstance(L2pSecurityManager.class.getName());

	// reminder: reflection makes member variables unsafe

	public L2pSecurityManager() {
		// check if local policy file exists, otherwise extract it from jar
		if (!new File("etc/las2peer.policy").exists()) {
			logger.info("Policy file not found. Extracting default policy file from jar...");
			InputStream fromJar = this.getClass().getResourceAsStream("/las2peer.policy");
			if (fromJar != null) {
				try {
					Files.copy(fromJar, new File("etc/las2peer.policy").toPath(), new CopyOption[] {});
				} catch (IOException e) {
					logger.log(Level.SEVERE, "Problems creating policy file! Sandboxing probably not working!", e);
					logger.printStackTrace(e);
				}
			} else {
				logger.severe("Fatal Error! No local policy file and no file in jar! Sandboxing WILL NOT WORK!");
			}
		}
		System.setProperty("java.security.policy", "etc/las2peer.policy");
	}

	@Override
	public void checkPermission(Permission perm) {
		if (perm instanceof FilePermission) {
			// block access to las2peer.policy file
			if (perm.getName().toLowerCase().contains("las2peer.policy")) {
				logger.warning("Blocked permission " + perm);
				throw new SecurityException("Policy file access restricted!");
			}
			// allow read on java.home
			String javaHome = normalizePath(System.getProperty("java.home"));
			if (normalizePath(perm.getName()).startsWith(javaHome)) {
				return;
			}
			// allow read on java.class.path
			String javaClassPath = System.getProperty("java.class.path");
			String pathSeparator = System.getProperty("path.separator");
			String[] clsSplit = javaClassPath.split(pathSeparator);
			for (String path : clsSplit) {
				path = normalizePath(path);
				if (path.endsWith(File.separator)) {
					path = path.substring(0, -1);
				}
				if (normalizePath(perm.getName()).startsWith(path)) {
					return;
				}
			}
			// allow write to junitvmwatcher*.properties
			if (perm.getName().toLowerCase().matches("^.*junitvmwatcher[0-9]+.properties$")) {
				return;
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

	private String normalizePath(String path) {
		try {
			return URLDecoder.decode(new File(path).getAbsolutePath(), "UTF-8").toLowerCase();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return path;
		}
	}

}
