package com.impossibl.postgres.jdbc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.impossibl.postgres.system.Context;


public class PSQLDriver implements Driver {
	
	static class ConnectionSpecifier {
		public String hostname;
		public Integer port;
		public String database;
		public Properties parameters = new Properties();
	}

	
	
	public PSQLDriver() throws SQLException {
		DriverManager.registerDriver(this);
	}

	@Override
	public PSQLConnection connect(String url, Properties info) throws SQLException {
		
		ConnectionSpecifier connSpec = parseURL(url);
		if(connSpec == null) {
			return null;
		}

		try {

			Properties settings = new Properties();
			settings.putAll(connSpec.parameters);
			settings.putAll(info);
			settings.put("database", connSpec.database);
			
			SocketAddress address = new InetSocketAddress(connSpec.hostname, connSpec.port);
			
			PSQLConnection conn = new PSQLConnection(address, settings, Collections.<String, Class<?>>emptyMap());
			
			conn.init();
			
			return conn;
			
		}
		catch (IOException e) {
			
			throw new SQLException("Connection Error", e);
		}
		
	}

	@Override
	public boolean acceptsURL(String url) throws SQLException {
		return parseURL(url) != null;
	}

	/*
	 * URL Pattern jdbc:postgresql:(?://(?:([a-zA-Z0-9\-\.]+|\[[0-9a-f\:]+\])(?:\:(\d+))?)/)?(\w+)(?:\?(.*))?
	 * 	Capturing Groups:
	 * 		1 = host name, IPv4, IPv6	(optional)
	 * 		2 = port 									(optional)
	 * 		3 = database name					(required)
	 * 		4 = parameters						(optional)
	 */
	private static final Pattern URL_PATTERN = Pattern.compile("jdbc:postgresql:(?://(?:([a-zA-Z0-9\\-\\.]+|\\[[0-9a-f\\:]+\\])(?:\\:(\\d+))?)/)?(\\w+)(?:\\?(.*))?");
	
	/**
	 * Parses a URL connection string.
	 * 
	 * Uses the URL_PATTERN to capture a hostname or ip address, port, database
	 * name and a list of parameters specified as query name=value pairs. All
	 * parts but the database name are optional.
	 * 
	 * @param url
	 * @return
	 */
	private ConnectionSpecifier parseURL(String url) {
		
		try {
			
			//First match aginst the entire URL pattern.  If that doesn't work
			//then the url is invalid
			
			Matcher urlMatcher = URL_PATTERN.matcher(url);
			if (!urlMatcher.matches()) {
				return null;
			}
			
			//Now build a conn-spec from the optional pieces of the URL
			//
			
			ConnectionSpecifier spec = new ConnectionSpecifier();
			
			//Assign hostname, if provided, or use the default "localhost"
			
			spec.hostname = urlMatcher.group(1);
			if(spec.hostname == null || spec.hostname.isEmpty()) {
				spec.hostname = "localhost";
			}
			
			//Assign port, if provided, or use the default "5432"
			
			String port = urlMatcher.group(2);
			if(port != null && !port.isEmpty()) {
				spec.port = Integer.parseInt(port);
			}
			else {
				spec.port = 5432;
			}
			
			//Assign the database
			
			spec.database = urlMatcher.group(3);
			
			//Parse the query string as a list of name=value pairs separated by '&'
			//then assign them as extra parameters
			
			String params = urlMatcher.group(4);
			if(params != null && !params.isEmpty()) {
				
				for(String nameValue : params.split("&")) {
					
					String[] items = nameValue.split("=");
					
					if(items.length == 1) {
						spec.parameters.put(items[0],null);
					}
					else if(items.length == 2) {
						spec.parameters.put(items[0],items[1]);
					}
					
				}
			}
			
			return spec;
			
		}
		catch(Throwable e) {
			return null;
		}
		
	}

	@Override
	public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
		
		List<DriverPropertyInfo> propInfo = new ArrayList<>();
		
		ConnectionSpecifier spec = parseURL(url);
		if(spec == null)
			spec = new ConnectionSpecifier();
		
		if(spec.database == null || spec.database.isEmpty())
			propInfo.add(new DriverPropertyInfo("database",""));			
		if(spec.parameters.get("username") == null || spec.parameters.get("username").toString().isEmpty())
			propInfo.add(new DriverPropertyInfo("username",""));			
		if(spec.parameters.get("password") == null || spec.parameters.get("password").toString().isEmpty())
			propInfo.add(new DriverPropertyInfo("password",""));			
		
		return propInfo.toArray(new DriverPropertyInfo[propInfo.size()]);
	}

	@Override
	public int getMajorVersion() {
		return 0;
	}

	@Override
	public int getMinorVersion() {
		return 1;
	}

	@Override
	public boolean jdbcCompliant() {
		return false;
	}

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return Logger.getLogger(Context.class.getPackage().getName());
	}

}
