SET arg1=%1
SET arg2=%2
SET arg3=%3
cmd.exe /C mvn -f %arg1% release:%arg2% %arg3% -B -Pgpg -Dgoals="source:jar-no-fork javadoc:jar deploy" -Dgpg.pinentry-mode=default -DskipTests -Darguments="-Dgpg.pinentry-mode=default -DskipTests" -DdryRun=true