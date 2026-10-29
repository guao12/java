#!/bin/sh
# 直接拉起缓存的 Maven 发行版，绕过 MinGW 下 mvn 脚本的路径转换问题。
# 用法: ./mvn-run.sh <maven 参数>  (例如 ./mvn-run.sh spring-boot:run)

MVN_UNIX="/c/Users/Administrator/.m2/wrapper/dists/apache-maven-3.9.9/66f6076d"
MVN_WIN=$(cygpath -w "$MVN_UNIX")
JH_WIN=$(cygpath -w "$JAVA_HOME")
BOOT_WIN=$(cygpath -w "$MVN_UNIX/boot/plexus-classworlds-2.8.0.jar")
CONF_WIN=$(cygpath -w "$MVN_UNIX/bin/m2.conf")
PROJ_WIN=$(cygpath -w "D:/workSpace/java/Dragon/springBootTest")
JAVACMD="$JH_WIN/bin/java.exe"

cd "D:/workSpace/java/Dragon/springBootTest" || exit 1

# 该环境（WorkBuddy agent shell）会注入 SERVER__PORT 环境变量，Spring Boot 的宽松绑定
# 会把它当成 server.port，从而覆盖 application-dev.properties 里的 8082。
# 这里显式用命令行参数覆盖，确保绑定到项目约定的 8082 端口。
MVN_ARGS="$@"
case "$MVN_ARGS" in
  *server.port*) ;;   # 已手动指定端口则不覆盖
  *) MVN_ARGS="$MVN_ARGS -Dspring-boot.run.arguments=--server.port=8082" ;;
esac

exec "$JAVACMD" \
  -classpath "$BOOT_WIN" \
  "-Dclassworlds.conf=$CONF_WIN" \
  "-Dmaven.home=$MVN_WIN" \
  "-Dlibrary.jansi.path=$MVN_WIN/lib/jansi-native" \
  "-Dmaven.multiModuleProjectDirectory=$PROJ_WIN" \
  org.codehaus.plexus.classworlds.launcher.Launcher $MVN_ARGS
