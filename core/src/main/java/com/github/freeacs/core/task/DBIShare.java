package com.github.freeacs.core.task;

import com.github.freeacs.common.scheduler.Task;
import com.github.freeacs.dbi.ACS;
import com.github.freeacs.dbi.DBI;
import com.github.freeacs.dbi.Identity;
import com.github.freeacs.dbi.Syslog;
import com.github.freeacs.dbi.SyslogConstants;
import com.github.freeacs.dbi.Users;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.slf4j.Logger;

/**
 * You can extend DBIShare if, and only if, you do not manipulate the contents of the ACS object.
 * Then you can share the same DBI object - and reduce the load on the system.
 *
 * @author Morten
 */
public abstract class DBIShare implements Task {
  private final DataSource mainDataSource;
  private final DataSource syslogDataSource;

  private static Users users;
  private static DBI dbi;
  private static Syslog syslog;
  private static Identity id;

  private String taskName;
  private long launchTms;
  private boolean running;

  private Throwable throwable;

  public DBIShare(String taskName, DataSource mainDataSource, DataSource syslogDataSource)
      throws SQLException {
    this.mainDataSource = mainDataSource;
    this.syslogDataSource = syslogDataSource;
    this.taskName = taskName;
    if (users == null) {
      users = new Users(this.mainDataSource);
    }
    if (id == null) {
      id =
          new Identity(
              SyslogConstants.FACILITY_CORE, "latest", users.getUnprotected(Users.USER_ADMIN));
    }
    if (dbi == null) {
      syslog = new Syslog(this.syslogDataSource, id);
      dbi = new DBI(Integer.MAX_VALUE, this.mainDataSource, syslog);
    }
  }

  protected ACS getLatestACS() {
    return dbi.getAcs();
  }

  protected DataSource getSyslogDataSource() {
    return syslogDataSource;
  }

  protected DataSource getMainDataSource() {
    return mainDataSource;
  }

  protected long getLaunchTms() {
    return launchTms;
  }

  protected Syslog getSyslog() {
    return syslog;
  }

  public void run() {
    try {
      running = true;
      runImpl();
      running = false;
    } catch (Throwable t) {
      running = false;
      throwable = t;
      getLogger().error("An error occurred", t);
    }
  }

  public boolean isRunning() {
    return running;
  }

  public abstract void runImpl() throws Exception;

  public abstract Logger getLogger();

  @Override
  public void setThisLaunchTms(long launchTms) {
    this.launchTms = launchTms;
  }

  @Override
  public String getTaskName() {
    return taskName;
  }

  public Throwable getThrowable() {
    return throwable;
  }

  public void setThrowable(Throwable throwable) {
    this.throwable = throwable;
  }
}
