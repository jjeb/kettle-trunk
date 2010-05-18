@echo off
setlocal 

:: **************************************************
:: ** Kettle home                                  **
:: **************************************************

if "%KETTLE_HOME%"=="" set KETTLE_HOME=%~dp0
if %KETTLE_HOME:~-1%==\ set KETTLE_HOME=%KETTLE_HOME:~0,-1%

cd  %KETTLE_HOME%

REM **************************************************
REM ** Set up usage of JAVA_EXT_LIBS                **
REM **************************************************

if defined JAVA_EXT_DIRS goto :externalExtDirs
:noExternalExtDirs
set JAVA_EXT_DIRS=.
goto endExtDirs

:externalExtDirs
set JAVA_EXT_DIRS=%JAVA_EXT_DIRS%;%~dp0

:endExtDirs

if DEFINED JAVA_HOME goto withJavaHome
:noJavaHome
goto endJavaHome

:withJavaHome
REM Every directory contains the null device. So check
REM for directory existence:
if exist %JAVA_HOME%\jre\lib\ext\nul set JAVA_EXT_DIRS=%JAVA_HOME%\jre\lib\ext;%JAVA_EXT_DIRS%
if exist %JAVA_HOME%\lib\ext\nul set JAVA_EXT_DIRS=%JAVA_HOME%\lib\ext;%JAVA_EXT_DIRS%
set JAVA_EXT_DIRS=%JAVA_EXT_DIRS%;lib
goto endJavaHome

:endJavaHome

REM ******************
REM   KETTLE Library
REM *****************

set JAVA_EXT_DIRS=%JAVA_EXT_DIRS%;%KETTLE_HOME%\lib

REM **********************
REM   External Libraries
REM **********************

set JAVA_EXT_DIRS=%JAVA_EXT_DIRS%;%KETTLE_HOME%\libext
set JAVA_EXT_DIRS=%JAVA_EXT_DIRS%;%KETTLE_HOME%\libext\JDBC
set JAVA_EXT_DIRS=%JAVA_EXT_DIRS%;%KETTLE_HOME%\libext\webservices
set JAVA_EXT_DIRS=%JAVA_EXT_DIRS%;%KETTLE_HOME%\libext\spring
set JAVA_EXT_DIRS=%JAVA_EXT_DIRS%;%KETTLE_HOME%\libext\commons
set JAVA_EXT_DIRS=%JAVA_EXT_DIRS%;%KETTLE_HOME%\libext\web
set JAVA_EXT_DIRS=%JAVA_EXT_DIRS%;%KETTLE_HOME%\libext\pentaho
set JAVA_EXT_DIRS=%JAVA_EXT_DIRS%;%KETTLE_HOME%\libext\mondrian
set JAVA_EXT_DIRS=%JAVA_EXT_DIRS%;%KETTLE_HOME%\libext\salesforce

REM *****************
REM   SWT Libraries
REM *****************

set JAVA_EXT_DIRS=%JAVA_EXT_DIRS%;%KETTLE_HOME%libswt\runtime.jar

REM **********************
REM   Collect arguments
REM **********************

set _cmdline=
:TopArg
if %1!==! goto EndArg
set _cmdline=%_cmdline% %1
shift
goto TopArg
:EndArg


REM ******************************************************************
REM ** Set java runtime options                                     **
REM ** Change 512m to higher values in case you run out of memory.  **
REM ******************************************************************

set OPT=-Xmx512M "-Djava.ext.dirs=%JAVA_EXT_DIRS%" -Djava.library.path=libswt\win32\ "-DKETTLE_HOME=%KETTLE_HOME%" "-DKETTLE_REPOSITORY=%KETTLE_REPOSITORY%" "-DKETTLE_USER=%KETTLE_USER%" "-DKETTLE_PASSWORD=%KETTLE_PASSWORD%" "-DKETTLE_PLUGIN_PACKAGES=%KETTLE_PLUGIN_PACKAGES%" "-DKETTLE_LOG_SIZE_LIMIT=%KETTLE_LOG_SIZE_LIMIT%"

REM ***************
REM ** Run...    **
REM ***************

java %OPT% org.pentaho.di.pan.Pan %_cmdline%
