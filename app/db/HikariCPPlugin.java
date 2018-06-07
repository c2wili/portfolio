package db;
 
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import jregex.Matcher;
import org.apache.commons.lang.StringUtils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.exceptions.DatabaseException;
import play.libs.Crypto;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Response;

public class HikariCPPlugin extends PlayPlugin {

	/**
	 * hikari.db plugin
	 */

	    public static String url = "";

 	    @Override
	    public void onApplicationStart() {
 	    	
 	    	Properties p = Play.configuration;
 	    	
 	    	Logger.info("HikariCP onApplicationStart");
 	    	Logger.info("HikariCP.datasource: " + HikariCP.datasource);
 	    	
 	    	Logger.info("hikari.db.url: " + p.getProperty("hikari.db.url"));
 	    	Logger.info("hikari.db.driver: " + p.getProperty("hikari.db.driver"));
 	    	Logger.info("hikari.db.user: " + p.getProperty("hikari.db.user"));
 	    	Logger.info("*****************************************************");
 	    	Logger.info("changed? " + changed());
 	    	
 	    	
	        if (changed()) {
	            try {
  
	                if (HikariCP.datasource != null) {
	                	HikariCP.destroy();
	                	Logger.info("destroyd");
	                }

                    // Try the driver
                    String driver = p.getProperty("hikari.db.driver");
                    try {
                        Driver d = (Driver) Class.forName(driver, true, Play.classloader).newInstance();
                        DriverManager.registerDriver(new ProxyDriver(d));
                    } catch (Exception e) {
                        throw new Exception("Driver not found (" + driver + ")");
                    }
                    
                    String applicationSecret = Play.configuration.getProperty("application.secret").substring(0,16);
                    String pw = Crypto.decryptAES(p.getProperty("hikari.db.pass"), applicationSecret);
                    
                    // Try the connection
                    Connection fake = null;
                    try {
                        if (p.getProperty("hikari.db.user") == null) {
                            fake = DriverManager.getConnection(p.getProperty("hikari.db.url"));
                        } else {
                            fake = DriverManager.getConnection(p.getProperty("hikari.db.url"), p.getProperty("hikari.db.user"), pw);
                        }
                    } finally {
                        if (fake != null) {
                            fake.close();
                        }
                    } 
                    
	                HikariConfig config = new HikariConfig();
	                    
                    config.setJdbcUrl(p.getProperty("hikari.db.url"));
                    config.setUsername(p.getProperty("hikari.db.user"));
                    config.setPassword(pw);
                    
                    config.addDataSourceProperty("cachePrepStmts", "true");
                    config.addDataSourceProperty("prepStmtCacheSize", "250");
                    config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                    
	            config.setConnectionInitSql("set time zone 'America/Chicago'");
    
                    if(p.getProperty("hikari.db.connectionTestQuery") != null) config.setConnectionTestQuery(p.getProperty("hikari.db.connectionTestQuery"));
                    if(p.getProperty("hikari.db.maximumPoolSize") != null) config.setMaximumPoolSize(Integer.parseInt(p.getProperty("hikari.db.maximumPoolSize")));
                    if(p.getProperty("hikari.db.minimumIdle") != null) config.setMaximumPoolSize(Integer.parseInt(p.getProperty("hikari.db.minimumIdle")));
                    if(p.getProperty("hikari.db.idleTimeout") != null) config.setConnectionTimeout(Integer.parseInt(p.getProperty("hikari.db.idleTimeout")) * 1000);
                    if(p.getProperty("hikari.db.idle.min") != null) config.setMinimumIdle(Integer.parseInt(p.getProperty("hikari.db.idle.min")));
                    if(p.getProperty("hikari.db.connectionTimeout") != null) config.setConnectionTimeout(Integer.parseInt(p.getProperty("hikari.db.connectionTimeout")) * 1000);
                    if(p.getProperty("hikari.db.autoCommit") != null) config.setAutoCommit(Boolean.parseBoolean(p.getProperty("hikari.db.autoCommit")));
                    if(p.getProperty("hikari.db.maxLifetime") != null) config.setConnectionTimeout(Integer.parseInt(p.getProperty("hikari.db.maxLifetime")) * 1000);
                    
                    

                    HikariDataSource ds = new HikariDataSource(config);
                    HikariCP.datasource = ds;
                    
                    url = ds.getJdbcUrl();
                    Connection c = null;
                    try {
                        c = ds.getConnection();
                    } finally {
                        if (c != null) {
                            c.close();
                        }
                    }
                    
                    Logger.info("HikariCP: Connected to %s", ds.getJdbcUrl());
                    
                    HikariCP.destroyMethod = p.getProperty("hikari.db.destroyMethod", "");
                    
                    

	            } catch (Exception e) {
	                HikariCP.datasource = null;
	                Logger.error(e, "Cannot connected to the database : %s", e.getMessage());
	                if (e.getCause() instanceof InterruptedException) {
	                    throw new DatabaseException("Cannot connected to the database. Check the configuration.", e);
	                }
	                throw new DatabaseException("Cannot connected to the database, " + e.getMessage(), e);
	            }
	        }
	    }

	    @Override
	    public void onApplicationStop() {
	        if (Play.mode.isProd()) {
	            HikariCP.destroy();
	        }
	    }

	    @Override
	    public String getStatus() {
	        StringWriter sw = new StringWriter();
	        PrintWriter out = new PrintWriter(sw);
	        if (HikariCP.datasource == null || !(HikariCP.datasource instanceof HikariDataSource)) {
	            out.println("Datasource:");
	            out.println("~~~~~~~~~~~");
	            out.println("(not yet connected)");
	            return sw.toString();
	        }
	        HikariDataSource datasource = (HikariDataSource) HikariCP.datasource;
	        out.println("Datasource:");
	        out.println("~~~~~~~~~~~");
	        out.println("Jdbc url: " + datasource.getJdbcUrl());
	        out.println("Jdbc driver: " + datasource.getDriverClassName());
	        out.println("Jdbc user: " + datasource.getUsername());
		
	        if (Play.mode.isDev()) {
	          out.println("Jdbc password: " + datasource.getPassword());
	        }
	       // out.println("Min pool size: " + datasource.getgetMinPoolSize());
	        out.println("Max pool size: " + datasource.getMaximumPoolSize());
	       //out.println("Initial pool size: " + datasource.getInitialPoolSize());
	       //out.println("Checkout timeout: " + datasource.getCheckoutTimeout());
	        return sw.toString();
	    }

	    @Override
	    public void invocationFinally() {
	        HikariCP.close();
	    }

	    private static void check(Properties p, String mode, String property) {
	        if (!StringUtils.isEmpty(p.getProperty(property))) {
	            Logger.warn("Ignoring " + property + " because running the in " + mode + " hikari.db.");
	        }
	    }

	    private static boolean changed() {
	        Properties p = Play.configuration;

            check(p, "hikari internal pool", "hikari.db.destroyMethod");
            p.put("hikari.db.destroyMethod", "close");
 
	        if ((p.getProperty("hikari.db.driver") == null) || (p.getProperty("hikari.db.url") == null)) {
	            return false;
	        }
	        
	        if (HikariCP.datasource == null) {
	            return true;
	        } else {
	        	HikariDataSource ds = (HikariDataSource) HikariCP.datasource;
	            if (!p.getProperty("hikari.db.driver").equals(ds.getDriverClassName())) {
	                return true;
	            }
	            if (!p.getProperty("hikari.db.url").equals(ds.getJdbcUrl())) {
	                return true;
	            }
	            if (!p.getProperty("hikari.db.driver", "").equals(ds.getUsername())) {
	                return true;
	            }
	            if (!p.getProperty("hikari.db.pass", "").equals(ds.getPassword())) {
	                return true;
	            }
	        }

	        if (!p.getProperty("hikari.db.destroyMethod", "").equals(HikariCP.destroyMethod)) {
	            return true;
	        }

	        return false;
	    }

	    /**
	     * Needed because DriverManager will not load a driver ouside of the system classloader
	     */
	    public static class ProxyDriver implements Driver {

	        private Driver driver;

	        ProxyDriver(Driver d) {
	            this.driver = d;
	        }

	        public boolean acceptsURL(String u) throws SQLException {
	            return this.driver.acceptsURL(u);
	        }

	        public Connection connect(String u, Properties p) throws SQLException {
	            return this.driver.connect(u, p);
	        }

	        public int getMajorVersion() {
	            return this.driver.getMajorVersion();
	        }

	        public int getMinorVersion() {
	            return this.driver.getMinorVersion();
	        }

	        public DriverPropertyInfo[] getPropertyInfo(String u, Properties p) throws SQLException {
	            return this.driver.getPropertyInfo(u, p);
	        }

	        public boolean jdbcCompliant() {
	            return this.driver.jdbcCompliant();
	        }
 
			public java.util.logging.Logger getParentLogger()
					throws SQLFeatureNotSupportedException {
				// TODO Auto-generated method stub
				return null;
			}
	    }
/*
	    public static class PlayConnectionCustomizer implements ConnectionCustomizer {

	        public static Map<String, Integer> isolationLevels;

	        static {
	            isolationLevels = new HashMap<String, Integer>();
	            isolationLevels.put("NONE", Connection.TRANSACTION_NONE);
	            isolationLevels.put("READ_UNCOMMITTED", Connection.TRANSACTION_READ_UNCOMMITTED);
	            isolationLevels.put("READ_COMMITTED", Connection.TRANSACTION_READ_COMMITTED);
	            isolationLevels.put("REPEATABLE_READ", Connection.TRANSACTION_REPEATABLE_READ);
	            isolationLevels.put("SERIALIZABLE", Connection.TRANSACTION_SERIALIZABLE);
	        }

	        public void onAcquire(Connection c, String parentDataSourceIdentityToken) {
	            Integer isolation = getIsolationLevel();
	            if (isolation != null) {
	                try {
	                    Logger.trace("Setting connection isolation level to %s", isolation);
	                    c.setTransactionIsolation(isolation);
	                } catch (SQLException e) {
	                    throw new DatabaseException("Failed to set isolation level to " + isolation, e);
	                }
	            }
	        }

	        public void onDestroy(Connection c, String parentDataSourceIdentityToken) {}
	        public void onCheckOut(Connection c, String parentDataSourceIdentityToken) {}
	        public void onCheckIn(Connection c, String parentDataSourceIdentityToken) {}
*/
	        /**
	         * Get the isolation level from either the isolationLevels map, or by
	         * parsing into an int.
	         */
/*	    
	        private Integer getIsolationLevel() {
	            String isolation = Play.configuration.getProperty("db.isolation");
	            if (isolation == null) {
	                return null;
	            }
	            Integer level = isolationLevels.get(isolation);
	            if (level != null) {
	                return level;
	            }

	            try {
	                return Integer.valueOf(isolation);
	            } catch (NumberFormatException e) {
	                throw new DatabaseException("Invalid isolation level configuration" + isolation, e);
	            }
	        }
	        */
	    
	}

