### package description

#### application framework

 - *tsl2.nano.h5.<version-number>.jar*: base "nano.h5" application. contains jdbc-drivers, ant and a jpa implementation.
 - *tsl2.nano.h5.<version-number>-standalone.jar*: includes base "nano.h5" application with ant, jdbc-drivers for hsqldb and mysql libraries to work without internet access.
 - *tsl2.nano.h5.<version-number>-signed.jar*: same as *tsl2.nano.h5.<version-number>-standalone.jar* but with signed content to be accessable through webstart (*nano.h5.jnlp*)
 - *nano.h5.jnlp*: java webstart descriptor to start *tsl2.nano.h5.<version-number>-signed.jar*. will create its environment directory in its download directory.
 - *tsl2.nano.terminal.<version-number>.jar*: "Structured Input Shell", a terminal application as toolbox for configurations, start scripts and administration. It is also integrated in *tsl2.nano.h5.<version-number>.jar*, but without starting support.  

#### sample applications

 - *timesheet.zip*: sample appication package _timesheet_. copy the content of this zip file to the directory where you saved your *tsl2.nano.h5.<version-number>.jar* or *tsl2.nano.h5.<version-number>-standalone.jar*. start the timesheet.bat or timesheet.sh to start a timesheet app.
