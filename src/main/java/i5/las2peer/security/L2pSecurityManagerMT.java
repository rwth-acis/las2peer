package i5.las2peer.security;

import java.io.File;
import java.io.FilePermission;
import java.lang.reflect.ReflectPermission;
import java.net.SocketPermission;
import java.security.Permission;
import java.util.PropertyPermission;
import java.util.logging.LoggingPermission;

import i5.las2peer.logging.L2pLogger;

public final class L2pSecurityManagerMT extends SecurityManager {

	@Override
	public void checkPermission(Permission perm) {
		// as long as reflections are uncontrolled, no private variable is safe
		L2pLogger logger = L2pLogger.getInstance(this.getClass().getName());
		String CLASS_PATH = System.getProperty("java.class.path");
		String SEPARATOR = System.getProperty("path.separator");
		String[] clsPaths = CLASS_PATH.split(SEPARATOR);
		String name = perm.getName();
		String[] actions = perm.getActions().split(",");
		if (perm instanceof FilePermission) {
			// check classpath
			for (String action : actions) {
				action = action.trim();
				if (action.equals("read")) {
					// check if in class path
					for (String clsPath : clsPaths) {
						// TODO optimize
						if (clsPath.endsWith(File.separator)) {
							clsPath = clsPath.substring(0, clsPath.lastIndexOf(File.separator));
						}
						if (name.startsWith(clsPath)) {
							return;
						}
					}
				} else if (action.equals("write")) {
					// FIXME limit write access to log directoy (maybe download directory)
					return;
				}
			}
			// check policy file
			try {
				super.checkPermission(perm);
			} catch (SecurityException e) {
				logger.warning("blocked permission: " + perm.toString());
				throw new SecurityException("File access denied!");
			}
		} else if (perm instanceof RuntimePermission) {
			// secure this SecurityManager itself
			if ("setSecurityManager".equals(name) || "createSecurityManager".equals(name)) {
				logger.warning("blocked permission: " + perm.toString());
				throw new SecurityException("SecurityManager already set!");
			}
		} else if (perm instanceof PropertyPermission) {
			// FIXME secure security essential properties
		} else if (perm instanceof SocketPermission) {
			// TODO maybe some ports should be blocked?
			// a service may open ports to listen for other protocolls than LAS2peer
		} else if (perm instanceof LoggingPermission) {
			// TODO limit logging control
			// a service may steal local logging information
		} else if (perm instanceof ReflectPermission) {
			// TODO check reflection calls
			// if reflection api is handled safely this class may be optimized,
			// till then don't use "private" variables or methods
		}
	}

}
