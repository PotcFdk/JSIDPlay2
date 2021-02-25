package server.restful.common;

import org.apache.tomcat.JarScanFilter;
import org.apache.tomcat.JarScanType;
import org.apache.tomcat.JarScanner;
import org.apache.tomcat.JarScannerCallback;

import jakarta.servlet.ServletContext;

public class NoOpJarScanner implements JarScanner {

	@Override
	public void setJarScanFilter(JarScanFilter jarScanFilter) {
	}

	@Override
	public void scan(JarScanType scanType, ServletContext context, JarScannerCallback callback) {
	}

	@Override
	public JarScanFilter getJarScanFilter() {
		return null;
	}
}
